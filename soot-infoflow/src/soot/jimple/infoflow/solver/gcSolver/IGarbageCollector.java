package soot.jimple.infoflow.solver.gcSolver;

import heros.solver.PathEdge;

/**
 * Common interface for all garbage collector implementations oin the solver
 * 
 * @author Steven Arzt
 *
 */
public interface IGarbageCollector<N, D> {

	/**
	 * Notifies the garbage collector that a new edge has been scheduled for
	 * processing
	 * 
	 * @param edge The edge that has been scheduled
	 */
	public void notifyEdgeSchedule(PathEdge<N, D> edge);

	/**
	 * Notifies the garbage collector that an edge has been fully processed
	 * 
	 * @param edge The edge has been fully processed
	 */
	public void notifyTaskProcessed(PathEdge<N, D> edge);

	/**
	 * Performs the garbage collection
	 */
	public void gc();

	/**
	 * Gets the number of methods for which taint abstractions were removed during
	 * garbage collection
	 * 
	 * @return The number of methods for which taint abstractions were removed
	 *         during garbage collection
	 */
	public int getGcedMethods();

	/**
	 * Gets the number of taint abstractions that were removed during garbage
	 * collection
	 * 
	 * @return The number of taint abstractions that were removed during garbage
	 *         collection
	 */
	public int getGcedEdges();

	/**
	 * Notifies the garbage collector that the IFDS solver has finished propagating
	 * its edges
	 */
	public void notifySolverTerminated();

}
