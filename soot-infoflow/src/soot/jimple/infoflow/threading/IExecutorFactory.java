package soot.jimple.infoflow.threading;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Common interface for classes that can create instances of thread pool
 * executors
 * 
 * @author Steven Arzt
 *
 */
public interface IExecutorFactory {

	/**
	 * Creates a new executor object for spawning worker threads
	 * 
	 * @param numThreads
	 *            The number of threads to use
	 * @param allowSetSemantics
	 *            True if the executor is allowed to skip new tasks if the same task
	 *            has already been scheduled before. False if the executor must
	 *            schedule all tasks it is given.
	 * @param config
	 *            The configuration of the data flow solver
	 * @return The generated executor
	 */
	public InterruptableExecutor createExecutor(int numThreads, boolean allowSetSemantics,
			InfoflowConfiguration config);

}
