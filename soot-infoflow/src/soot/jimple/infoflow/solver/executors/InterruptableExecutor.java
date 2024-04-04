package soot.jimple.infoflow.solver.executors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.solver.CountingThreadPoolExecutor;
import soot.jimple.infoflow.collect.BlackHoleCollection;

public class InterruptableExecutor extends CountingThreadPoolExecutor {

	protected static final Logger logger = LoggerFactory.getLogger(InterruptableExecutor.class);

	private boolean interrupted = false;
	private boolean terminated = false;

	public InterruptableExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	/**
	 * Interrupts the executor. This will make the awaitCompletion() methods return
	 * immediately and silently reject all new tasks.
	 */
	public void interrupt() {
		// Make sure that no new tasks are spawned
		this.interrupted = true;

		// Get rid of the cancelled tasks in the work queue
		getQueue().clear();
		getQueue().drainTo(new BlackHoleCollection<>());
		this.purge();

		// Signal to the blocking threads that we are done
		numRunningTasks.resetAndInterrupt();
	}

	/**
	 * Resets the executor to allow for new tasks once all previous tasks have
	 * completed or after the executor has been interrupted.
	 */
	public void reset() {
		this.terminated = false;
		this.interrupted = false;
	}

	@Override
	public void execute(Runnable command) {
		// If the executor was terminated, it must be reset explicitly
		if (this.terminated) {
			logger.warn("Executor has terminated. Call reset() before submitting new tasks.");
			return;
		}

		// Discard all submitted tasks if the executor has been interrupted
		try {
			if (!this.interrupted)
				super.execute(command);
		} catch (RejectedExecutionException ex) {
			// We expect the solver to be aborted, just terminate silently
			// now
			this.interrupted = true;
		}
	}

	@Override
	public void awaitCompletion() throws InterruptedException {
		// If we already know that we're done, there's no need to wait
		if (terminated)
			return;

		// Wait for the tasks to complete
		super.awaitCompletion();
		terminated = true;
	}

	@Override
	public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		// If we already know that we're done, there's no need to wait
		if (terminated)
			return;

		// Wait for the tasks to complete
		super.awaitCompletion(timeout, unit);
		terminated = true;
	}

	/**
	 * Gets whether this executor has terminated all of its tasks
	 * 
	 * @return True if this executor has terminated all of its tasks, otherwise
	 *         false
	 */
	public boolean isFinished() {
		return terminated || numRunningTasks.isAtZero();
	}

	@Override
	public boolean isTerminated() {
		return terminated || super.isTerminated();
	}

}
