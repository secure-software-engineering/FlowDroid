package soot.jimple.infoflow.data.pathBuilders;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import heros.solver.Pair;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collect.ConcurrentIdentityHashMultiMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Class for reconstructing abstraction paths from sinks to source. This builder
 * is context-sensitive which makes it more precise than the
 * {@link ContextInsensitivePathBuilder}, but also a bit slower.
 * 
 * @author Steven Arzt
 */
public class ContextSensitivePathBuilder extends ConcurrentAbstractionPathBuilder {

	protected ConcurrentIdentityHashMultiMap<Abstraction, SourceContextAndPath> pathCache = new ConcurrentIdentityHashMultiMap<>();

	/**
	 * Creates a new instance of the {@link ContextSensitivePathBuilder} class
	 * 
	 * @param manager The data flow manager that gives access to the icfg and other
	 *                objects
	 */
	public ContextSensitivePathBuilder(InfoflowManager manager) {
		super(manager, createExecutor(manager));
	}

	private static InterruptableExecutor createExecutor(InfoflowManager manager) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		int mtn = manager.getConfig().getMaxThreadNum();
		InterruptableExecutor executor = new InterruptableExecutor(mtn == -1 ? numThreads : Math.min(mtn, numThreads),
				Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
		executor.setThreadFactory(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Path reconstruction");
			}

		});
		return executor;
	}

	/**
	 * Task for tracking back the path from sink to source.
	 * 
	 * @author Steven Arzt
	 */
	protected class SourceFindingTask implements Runnable, Comparable<SourceFindingTask> {
		private final Abstraction abstraction;

		public SourceFindingTask(Abstraction abstraction) {
			this.abstraction = abstraction;
		}

		@Override
		public void run() {
			final Set<SourceContextAndPath> paths = pathCache.get(abstraction);
			final Abstraction pred = abstraction.getPredecessor();

			if (pred != null && paths != null) {
				for (SourceContextAndPath scap : paths) {
					// Process the predecessor
					if (processPredecessor(scap, pred)) {
						// Schedule the predecessor
						scheduleDependentTask(new SourceFindingTask(pred));
					}

					// Process the predecessor's neighbors
					if (pred.getNeighbors() != null) {
						for (Abstraction neighbor : pred.getNeighbors()) {
							if (processPredecessor(scap, neighbor)) {
								// Schedule the predecessor
								scheduleDependentTask(new SourceFindingTask(neighbor));
							}
						}
					}
				}
			}
		}

		private boolean processPredecessor(SourceContextAndPath scap, Abstraction pred) {
			// Shortcut: If this a call-to-return node, we should not enter and
			// immediately leave again for performance reasons.
			if (pred.getCurrentStmt() != null && pred.getCurrentStmt() == pred.getCorrespondingCallSite()) {
				SourceContextAndPath extendedScap = scap.extendPath(pred, pathConfig);
				if (extendedScap == null)
					return false;

				checkForSource(pred, extendedScap);
				return pathCache.put(pred, extendedScap);
			}

			// If we enter a method, we put it on the stack
			SourceContextAndPath extendedScap = scap.extendPath(pred, pathConfig);
			if (extendedScap == null)
				return false;

			// Do we process a method return?
			if (pred.getCurrentStmt() != null && pred.getCurrentStmt().containsInvokeExpr()) {
				// Pop the top item off the call stack. This gives us the item
				// and the new SCAP without the item we popped off.
				Pair<SourceContextAndPath, Stmt> pathAndItem = extendedScap.popTopCallStackItem();
				if (pathAndItem != null) {
					Stmt topCallStackItem = pathAndItem.getO2();
					// Make sure that we don't follow an unrealizable path
					if (topCallStackItem != pred.getCurrentStmt())
						return false;

					// We have returned from a function
					extendedScap = pathAndItem.getO1();
				}
			}

			// Add the new path
			checkForSource(pred, extendedScap);

			final int maxPaths = pathConfig.getMaxPathsPerAbstraction();
			if (maxPaths > 0) {
				Set<SourceContextAndPath> existingPaths = pathCache.get(pred);
				if (existingPaths != null && existingPaths.size() > maxPaths)
					return false;
			}
			return pathCache.put(pred, extendedScap);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SourceFindingTask other = (SourceFindingTask) obj;
			return abstraction == other.abstraction;
		}

		@Override
		public int compareTo(SourceFindingTask arg0) {
			return Integer.compare(abstraction.getPathLength(), arg0.abstraction.getPathLength());
		}

	}

	/**
	 * Checks whether the given abstraction is a source. If so, a result entry is
	 * created.
	 * 
	 * @param abs  The abstraction to check
	 * @param scap The path leading up to the current abstraction
	 * @return True if the current abstraction is a source, otherwise false
	 */
	protected boolean checkForSource(Abstraction abs, SourceContextAndPath scap) {
		if (abs.getPredecessor() != null)
			return false;

		// If we have no predecessors, this must be a source
		assert abs.getSourceContext() != null;

		// A source should normally never have neighbors, but it can happen
		// with ICCTA
		if (abs.getNeighbors() != null) {
			// we ignore this issue for now, because the neighbor's source
			// contexts seem to be equal to our own one
		}

		// Register the source that we have found
		SourceContext sourceContext = abs.getSourceContext();
		Pair<ResultSourceInfo, ResultSinkInfo> newResult = results.addResult(scap.getDefinition(), scap.getAccessPath(),
				scap.getStmt(), sourceContext.getDefinition(), sourceContext.getAccessPath(), sourceContext.getStmt(),
				sourceContext.getUserData(), scap.getAbstractionPath());

		// Notify our handlers
		if (resultAvailableHandlers != null)
			for (OnPathBuilderResultAvailable handler : resultAvailableHandlers)
				handler.onResultAvailable(newResult.getO1(), newResult.getO2());

		return true;
	}

	@Override
	public void runIncrementalPathCompuation() {
		Set<AbstractionAtSink> incrementalAbs = new HashSet<>();
		for (Abstraction abs : pathCache.keySet())
			for (SourceContextAndPath scap : pathCache.get(abs)) {
				if (abs.getNeighbors() != null && abs.getNeighbors().size() != scap.getNeighborCounter()) {
					// This is a path for which we have to process the new
					// neighbors
					scap.setNeighborCounter(abs.getNeighbors().size());

					for (Abstraction neighbor : abs.getNeighbors())
						incrementalAbs.add(new AbstractionAtSink(scap.getDefinition(), neighbor, scap.getStmt()));
				}
			}
		if (!incrementalAbs.isEmpty())
			this.computeTaintPaths(incrementalAbs);
	}

	@Override
	public void computeTaintPaths(Set<AbstractionAtSink> res) {
		try {
			super.computeTaintPaths(res);

			// Wait for the path builder to terminate. The path reconstruction should stop
			// on time anyway. In case it doesn't, we make sure that we don't get stuck.
			long pathTimeout = manager.getConfig().getPathConfiguration().getPathReconstructionTimeout();
			if (pathTimeout > 0)
				executor.awaitCompletion(pathTimeout + 20, TimeUnit.SECONDS);
			else
				executor.awaitCompletion();
		} catch (InterruptedException e) {
			logger.error("Could not wait for executor termination", e);
		} finally {
			onTaintPathsComputed();
		}
	}

	/**
	 * Method that is called when the taint paths have been computed
	 */
	protected void onTaintPathsComputed() {
		shutdown();
	}

	/**
	 * Terminates the internal executor and cleans up all resources that were used
	 */
	public void shutdown() {
		executor.shutdown();
	}

	@Override
	protected Runnable getTaintPathTask(final AbstractionAtSink abs) {
		SourceContextAndPath scap = new SourceContextAndPath(abs.getSinkDefinition(),
				abs.getAbstraction().getAccessPath(), abs.getSinkStmt());
		scap = scap.extendPath(abs.getAbstraction(), pathConfig);

		if (pathCache.put(abs.getAbstraction(), scap)) {
			if (!checkForSource(abs.getAbstraction(), scap))
				return new SourceFindingTask(abs.getAbstraction());
		}
		return null;
	}

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

	@Override
	protected boolean triggerComputationForNeighbors() {
		return true;
	}

}
