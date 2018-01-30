package soot.jimple.infoflow.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.memory.IMemoryBoundedSolver.IMemoryBoundedSolverStatusNotification;
import soot.jimple.infoflow.memory.reasons.TimeoutReason;
import soot.jimple.infoflow.results.InfoflowResults;

/**
 * Class for enforcing timeouts on IFDS solvers
 * 
 * @author Steven Arzt
 *
 */
public class FlowDroidTimeoutWatcher implements IMemoryBoundedSolverStatusNotification {

	/**
	 * Enumeration containing all states in which a solver can be
	 * 
	 * @author Steven Arzt
	 *
	 */
	private enum SolverState {
		/**
		 * The solver has not been started yet
		 */
		IDLE,
		/**
		 * The solver is running
		 */
		RUNNING,
		/**
		 * The solver has completed its work
		 */
		DONE
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final long timeout;
	private final InfoflowResults results;
	private final Map<IMemoryBoundedSolver, SolverState> solvers = new ConcurrentHashMap<>();
	private boolean stopped = false;

	/**
	 * Creates a new instance of the {@link FlowDroidTimeoutWatcher} class
	 * 
	 * @param timeout
	 *            The timeout in seconds after which the solvers shall be
	 *            stopped
	 */
	public FlowDroidTimeoutWatcher(long timeout) {
		this.timeout = timeout;
		this.results = null;
	}

	/**
	 * Creates a new instance of the {@link FlowDroidTimeoutWatcher} class
	 * 
	 * @param timeout
	 *            The timeout in seconds after which the solvers shall be
	 *            stopped
	 * @param res
	 *            The InfoflowResults object
	 */
	public FlowDroidTimeoutWatcher(long timeout, InfoflowResults res) {
		this.timeout = timeout;
		this.results = res;
	}

	/**
	 * Gets the timeout after which the IFDS solvers are aborted
	 * 
	 * @return The timeout after which the IFDS solvers are aborted
	 */
	public long getTimeout() {
		return this.timeout;
	}

	/**
	 * Adds a solver that shall be terminated when the timeout is reached
	 * 
	 * @param solver
	 *            A solver that shall be terminated when the timeout is reached
	 */
	public void addSolver(IMemoryBoundedSolver solver) {
		this.solvers.put(solver, SolverState.IDLE);
		solver.addStatusListener(this);
	}

	/**
	 * Starts the timeout watcher
	 */
	public void start() {
		final long startTime = System.currentTimeMillis();
		logger.info("FlowDroid timeout watcher started");
		this.stopped = false;

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Sleep until we have reached the timeout
				boolean allTerminated = isTerminated();
				long timeElapsed = 0;

				while (!stopped && ((timeElapsed = System.currentTimeMillis() - startTime) < 1000 * timeout)) {
					allTerminated = isTerminated();
					if (allTerminated)
						break;

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// There's little we can do here

					}
				}

				// If things have not stopped on their own account, we force
				// them to
				if (!stopped & !allTerminated) {
					logger.warn("Timeout reached, stopping the solvers...");
					if (results != null)
						results.addException("Timeout reached");

					TimeoutReason reason = new TimeoutReason(timeElapsed / 1000, timeout);
					for (IMemoryBoundedSolver solver : solvers.keySet())
						solver.forceTerminate(reason);
				}

				logger.info("FlowDroid timeout watcher terminated");
			}

			private boolean isTerminated() {
				boolean allTerminated;
				// Check whether all solvers in our watchlist have finished
				// their work
				allTerminated = true;
				for (IMemoryBoundedSolver solver : solvers.keySet()) {
					if (solvers.get(solver) != SolverState.DONE || !solver.isTerminated()) {
						allTerminated = false;
						break;
					}
				}
				return allTerminated;
			}

		}, "FlowDroid Timeout Watcher").start();
	}

	/**
	 * Stops the timeout watcher so that it no longer interferes with solver
	 * execution
	 */
	public void stop() {
		this.stopped = true;
	}

	/**
	 * Resets the internal state of the watcher so that it can be used again
	 * after being stopped
	 */
	public void reset() {
		this.stopped = false;
		for (IMemoryBoundedSolver solver : this.solvers.keySet())
			this.solvers.put(solver, SolverState.IDLE);
	}

	@Override
	public void notifySolverStarted(IMemoryBoundedSolver solver) {
		solvers.put(solver, SolverState.RUNNING);
	}

	@Override
	public void notifySolverTerminated(IMemoryBoundedSolver solver) {
		solvers.put(solver, SolverState.DONE);
	}

}
