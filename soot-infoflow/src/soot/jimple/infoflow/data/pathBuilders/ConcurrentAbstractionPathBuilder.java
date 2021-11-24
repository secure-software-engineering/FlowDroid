package soot.jimple.infoflow.data.pathBuilders;

import java.util.HashSet;
import java.util.Set;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

public abstract class ConcurrentAbstractionPathBuilder extends AbstractAbstractionPathBuilder {

	protected final InfoflowResults results = new InfoflowResults();

	protected final InterruptableExecutor executor;
	private Set<IMemoryBoundedSolverStatusNotification> notificationListeners = new HashSet<>();
	private ISolverTerminationReason killFlag = null;

	public ConcurrentAbstractionPathBuilder(InfoflowManager manager, InterruptableExecutor executor) {
		super(manager);
		this.executor = executor;
	}

	/**
	 * Gets the task that computes the result paths for a single abstraction. If the
	 * concrete path building algorithm does not want to handle the given
	 * abstraction, it can return null to not execute anything.
	 * 
	 * @param abs The abstraction for which to compute the result paths
	 */
	protected abstract Runnable getTaintPathTask(AbstractionAtSink abs);

	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink> res) {
		if (res == null || res.isEmpty())
			return;

		logger.info("Obtainted {} connections between sources and sinks", res.size());

		// Notify the listeners that the solver has been started
		for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
			listener.notifySolverStarted(this);

		// Start the propagation tasks
		int curResIdx = 0;
		for (final AbstractionAtSink abs : res) {
			// We need to reset the executor before we can submit new jobs
			executor.reset();

			// The solver may already have been killed
			if (killFlag != null) {
				// Reduce the memory pressure
				res.clear();
				break;
			}

			// Schedule the main abstraction
			logger.info(String.format("Building path %d...", ++curResIdx));
			Runnable task = getTaintPathTask(abs);
			if (task != null)
				executor.execute(task);

			// Also build paths for the neighbors of our result abstraction
			if (triggerComputationForNeighbors() && abs.getAbstraction().getNeighbors() != null) {
				for (Abstraction neighbor : abs.getAbstraction().getNeighbors()) {
					AbstractionAtSink neighborAtSink = new AbstractionAtSink(abs.getSinkDefinition(), neighbor,
							abs.getSinkStmt());
					task = getTaintPathTask(neighborAtSink);
					if (task != null)
						executor.execute(task);
				}
			}

			// If we do sequential path processing, we wait for the current
			// sink abstraction to be processed before working on the next
			if (pathConfig.getSequentialPathProcessing()) {
				try {
					executor.awaitCompletion();
					executor.reset();
				} catch (InterruptedException ex) {
					logger.error("Could not wait for path executor completion", ex);
				}
			}
		}

		// Notify the listeners that the solver has been terminated
		for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
			listener.notifySolverTerminated(this);
	}

	/**
	 * Specifies whether the class shall create separate path reconstruction tasks
	 * for the neighbors of the abstractions that arrive at the sink
	 * 
	 * @return True if separate path reconstruction tasks shall be started for each
	 *         neighbor of each abstraction that reaches a sink, false if only the
	 *         abstraction itself shall trigger a path reconstruction task
	 */
	protected abstract boolean triggerComputationForNeighbors();

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

	@Override
	public void forceTerminate(ISolverTerminationReason reason) {
		killFlag = reason;
		executor.interrupt();
		logger.warn("Path reconstruction terminated due to low memory");
	}

	/**
	 * Schedules the given task for execution
	 * 
	 * @param task The task to execute
	 */
	protected void scheduleDependentTask(Runnable task) {
		if (!isKilled())
			executor.execute(task);
	}

	@Override
	public boolean isTerminated() {
		return killFlag != null || this.executor.isFinished();
	}

	@Override
	public boolean isKilled() {
		return killFlag != null;
	}

	@Override
	public void reset() {
		this.killFlag = null;
	}

	@Override
	public ISolverTerminationReason getTerminationReason() {
		return killFlag;
	}

	@Override
	public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
		this.notificationListeners.add(listener);
	}

	/**
	 * Gets the executor that runs the tasks for the path reconstruction
	 * 
	 * @return The executor that runs the tasks for the path reconstruction
	 */
	InterruptableExecutor getExecutor() {
		return this.executor;
	}

}
