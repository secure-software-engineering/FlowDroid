package soot.jimple.infoflow.data.pathBuilders;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Stack;

import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.results.InfoflowResults;

/**
 * Recursive algorithm for reconstructing abstraction paths from sink to source
 * 
 * @author Steven Arzt
 */
public class RecursivePathBuilder extends AbstractAbstractionPathBuilder {

	private final InfoflowResults results = new InfoflowResults();
	private final CountingThreadPoolExecutor executor;

	private static int lastTaskId = 0;

	/**
	 * Creates a new instance of the {@link RecursivePathBuilder} class
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param config
	 *            The configuration of the data flow solver
	 * @param executor
	 *            The executor in which to run the path reconstruction tasks
	 * @param reconstructPaths
	 *            True if the exact propagation path between source and sink shall
	 *            be reconstructed.
	 */
	public RecursivePathBuilder(InfoflowManager manager, CountingThreadPoolExecutor executor) {
		super(manager);
		this.executor = executor;
	}

	/**
	 * Gets the path of statements from the source to the current statement with
	 * which this abstraction is associated. If this path is ambiguous, a single
	 * path is selected randomly.
	 * 
	 * @param taskId
	 *            A unique ID identifying this path search task
	 * @param curAbs
	 *            The current abstraction from which to start the search
	 * @return The path from the source to the current statement
	 */
	private Set<SourceContextAndPath> getPaths(int taskId, Abstraction curAbs,
			Stack<Pair<Stmt, Set<Abstraction>>> callStack) {
		Set<SourceContextAndPath> cacheData = new HashSet<SourceContextAndPath>();

		Pair<Stmt, Set<Abstraction>> stackTop = callStack.isEmpty() ? null : callStack.peek();
		if (stackTop != null) {
			Set<Abstraction> callAbs = stackTop.getO2();
			if (!callAbs.add(curAbs))
				return Collections.emptySet();
		}

		// If the current statement is a source, we have found a path root
		if (curAbs.getSourceContext() != null) {
			// Construct the path root
			SourceContextAndPath sourceAndPath = new SourceContextAndPath(curAbs.getSourceContext().getDefinition(),
					curAbs.getSourceContext().getAccessPath(), curAbs.getSourceContext().getStmt(),
					curAbs.getSourceContext().getUserData()).extendPath(curAbs);
			cacheData.add(sourceAndPath);

			// Sources may not have predecessors
			assert curAbs.getPredecessor() == null;
		} else {
			// If we have a corresponding call site for the current return
			// statement, we need to add it to the call stack.
			Stack<Pair<Stmt, Set<Abstraction>>> newCallStack = new Stack<Pair<Stmt, Set<Abstraction>>>();
			newCallStack.addAll(callStack);
			if (curAbs.getCorrespondingCallSite() != null/*
															 * && curAbs. isAbstractionActive()
															 */)
				newCallStack.push(new Pair<Stmt, Set<Abstraction>>(curAbs.getCorrespondingCallSite(),
						Collections.newSetFromMap(new IdentityHashMap<Abstraction, Boolean>())));

			boolean isMethodEnter = curAbs.getCurrentStmt() != null && curAbs.getCurrentStmt().containsInvokeExpr();

			// If the current statement enters a method, we have to check the
			// call stack.
			// We only accept the path if the call site to which we return
			// matches the
			// call site at which we have entered the callee. We accept the call
			// stack
			// to run empty if we follow returns past seeds.
			boolean scanPreds = true;
			if (isMethodEnter)
				if (!newCallStack.isEmpty()) {
					Pair<Stmt, Set<Abstraction>> newStackTop = newCallStack.isEmpty() ? null : newCallStack.peek();
					if (newStackTop != null && newStackTop.getO1() != null) {
						if (curAbs.getCurrentStmt() != newStackTop.getO1())
							scanPreds = false;
						newCallStack.pop();
					}
				}

			if (scanPreds) {
				// Otherwise, we have to check the predecessor
				for (SourceContextAndPath curScap : getPaths(taskId, curAbs.getPredecessor(), newCallStack)) {
					SourceContextAndPath extendedPath = curScap.extendPath(curAbs, pathConfig);
					if (extendedPath != null)
						cacheData.add(extendedPath);
				}
			}
		}

		if (curAbs.getNeighbors() != null)
			for (Abstraction nb : curAbs.getNeighbors())
				for (SourceContextAndPath path : getPaths(taskId, nb, callStack))
					cacheData.add(path);

		return Collections.unmodifiableSet(cacheData);
	}

	/**
	 * Computes the path of tainted data between the source and the sink
	 * 
	 * @param res
	 *            The data flow tracker results
	 */
	private void computeTaintPathsInternal(final Set<AbstractionAtSink> res) {
		logger.debug("Running path reconstruction");
		logger.info("Obtainted {} connections between sources and sinks", res.size());
		int curResIdx = 0;
		for (final AbstractionAtSink abs : res) {
			logger.info(String.format("Building path %d...", ++curResIdx));
			executor.execute(new Runnable() {

				@Override
				public void run() {
					Stack<Pair<Stmt, Set<Abstraction>>> initialStack = new Stack<Pair<Stmt, Set<Abstraction>>>();
					initialStack.push(new Pair<Stmt, Set<Abstraction>>(null,
							Collections.newSetFromMap(new IdentityHashMap<Abstraction, Boolean>())));
					for (SourceContextAndPath context : getPaths(lastTaskId++, abs.getAbstraction(), initialStack)) {
						results.addResult(abs.getSinkDefinition(), abs.getAbstraction().getAccessPath(),
								abs.getSinkStmt(), context.getDefinition(), context.getAccessPath(), context.getStmt(),
								context.getUserData(), context.getAbstractionPath());
					}
				}

			});

		}

		try {
			executor.awaitCompletion();
		} catch (InterruptedException ex) {
			logger.error("Could not wait for path executor completion: {0}", ex.getMessage());
			ex.printStackTrace();
		}
		executor.shutdown();
		logger.debug("Path reconstruction done.");
	}

	@Override
	public void computeTaintPaths(Set<AbstractionAtSink> res) {
		computeTaintPathsInternal(res);
	}

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

	@Override
	public void runIncrementalPathCompuation() {
		// not implemented
	}

	@Override
	public void forceTerminate(ISolverTerminationReason reason) {
		// This path builder cannot be terminated
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public boolean isKilled() {
		return false;
	}

	@Override
	public void reset() {
		// This path builder cannot be terminated
	}

	@Override
	public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
		// not supported
	}

	@Override
	public ISolverTerminationReason getTerminationReason() {
		// This path builder cannot be terminated
		return null;
	}

}
