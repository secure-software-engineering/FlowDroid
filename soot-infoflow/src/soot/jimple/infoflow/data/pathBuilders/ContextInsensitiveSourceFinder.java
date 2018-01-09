package soot.jimple.infoflow.data.pathBuilders;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Class for reconstructing abstraction paths from sinks to source
 * 
 * @author Steven Arzt
 */
public class ContextInsensitiveSourceFinder extends ConcurrentAbstractionPathBuilder {

	private int lastTaskId = 0;
	private int numTasks = 0;

	/**
	 * Creates a new instance of the {@link ContextInsensitiveSourceFinder}
	 * class
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param executor
	 *            The executor in which to run the path reconstruction tasks
	 * @param maxThreadNum
	 *            The maximum number of threads to use
	 */
	public ContextInsensitiveSourceFinder(InfoflowManager manager, InterruptableExecutor executor) {
		this(manager, executor, -1);
	}

	/**
	 * Creates a new instance of the {@link ContextInsensitiveSourceFinder}
	 * class
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param executor
	 *            The executor in which to run the path reconstruction tasks
	 * @param maxThreadNum
	 *            The maximum number of threads to use
	 * @param numTasks
	 *            The maximum number of abstractions for which a path
	 *            reconstruction will be performed
	 */
	public ContextInsensitiveSourceFinder(InfoflowManager manager, InterruptableExecutor executor, int numTasks) {
		super(manager, executor);
		this.numTasks = numTasks;
	}

	/**
	 * Task for only finding sources, not the paths towards them
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final int taskId;
		private final AbstractionAtSink flagAbs;
		private final List<Abstraction> abstractionQueue = new LinkedList<Abstraction>();

		public SourceFindingTask(int taskId, AbstractionAtSink flagAbs, Abstraction abstraction) {
			this.taskId = taskId;
			this.flagAbs = flagAbs;
			this.abstractionQueue.add(abstraction);
			abstraction.registerPathFlag(taskId, numTasks);
		}

		@Override
		public void run() {
			while (!abstractionQueue.isEmpty()) {
				// Terminate the thread when we run out of memory
				if (isKilled()) {
					abstractionQueue.clear();
					return;
				}

				Abstraction abstraction = abstractionQueue.remove(0);
				if (abstraction.getSourceContext() != null) {
					// Register the result
					results.addResult(flagAbs.getSinkDefinition(), flagAbs.getAbstraction().getAccessPath(),
							flagAbs.getSinkStmt(), abstraction.getSourceContext().getDefinition(),
							abstraction.getSourceContext().getAccessPath(), abstraction.getSourceContext().getStmt(),
							abstraction.getSourceContext().getUserData(), null);

					// Sources may not have predecessors
					assert abstraction.getPredecessor() == null;
				} else if (abstraction.getPredecessor().registerPathFlag(taskId, numTasks))
					abstractionQueue.add(abstraction.getPredecessor());

				if (abstraction.getNeighbors() != null)
					for (Abstraction nb : abstraction.getNeighbors())
						if (nb.registerPathFlag(taskId, numTasks))
							abstractionQueue.add(nb);
			}
		}
	}

	@Override
	protected boolean triggerComputationForNeighbors() {
		return false;
	}

	@Override
	protected Runnable getTaintPathTask(AbstractionAtSink abs) {
		return new SourceFindingTask(lastTaskId++, abs, abs.getAbstraction());
	}

	@Override
	public void runIncrementalPathCompuation() {
		// not implemented
	}

	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink> res) {
		if (numTasks < 0)
			numTasks = res.size();
		else
			numTasks += res.size();
		super.computeTaintPaths(res);
	}

}
