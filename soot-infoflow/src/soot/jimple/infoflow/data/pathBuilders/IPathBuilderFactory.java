package soot.jimple.infoflow.data.pathBuilders;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Common interface for all path builder factories
 * 
 * @author Steven Arzt
 */
public interface IPathBuilderFactory {

	/**
	 * Creates a new path builder. Use this overload to have the path builder
	 * manage its own executors.
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param maxThreadNum
	 *            The maximum number of threads to use * @return The newly
	 *            created path builder
	 */
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, int maxThreadNum);

	/**
	 * Creates a new path builder. Use this overload if you want the path
	 * builder to submit its tasks to an existing executor.
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param executor
	 *            The executor in which to run the path reconstruction tasks.
	 *            * @return The newly created path builder
	 */
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, InterruptableExecutor executor);

	/**
	 * Gets whether the {@link IAbstractionPathBuilder} object created by this
	 * factory supports the reconstruction of the exact paths between source and
	 * sink.
	 * 
	 * @return True if the {@link IAbstractionPathBuilder} object constructed by
	 *         this factory gives the exact propagation path between source and
	 *         sink, false if it only reports source-to-sink connections without
	 *         paths.
	 */
	public boolean supportsPathReconstruction();

	/**
	 * Gets whether the {@link IAbstractionPathBuilder} object created by this
	 * factory supports context-sensitive path reconstruction.
	 * 
	 * @return True if the {@link IAbstractionPathBuilder} object created by
	 *         this factory supports context-sensitive path reconstruction,
	 *         otherwise false.
	 */
	public boolean isContextSensitive();

}
