package soot.jimple.infoflow.solver.fastSolver;

/**
 * Common interface for all abstractions processed by the IFDS solver
 * 
 * @author Steven Arzt
 */
public interface FastSolverLinkedNode<D, N> extends Cloneable {

	/**
	 * Links this node to a neighbor node, i.e., to an abstraction that would have
	 * been merged with this one of paths were not being tracked.
	 * 
	 * @return True if the neighbor was added, false if it was rejected
	 */
	public boolean addNeighbor(D originalAbstraction);

	/**
	 * Gets the number of neighbors already registered with this abstraction
	 * 
	 * @return The number of neighbors already registered with this abstraction
	 */
	public int getNeighborCount();

	/**
	 * Explicitly sets the predecessor of this node.
	 * 
	 * @param predecessor
	 *            The predecessor node to set
	 */
	public void setPredecessor(D predecessor);

	/**
	 * Gets the predecessor of this node
	 * 
	 * @return The predecessor of this node is applicable, null for source nodes
	 */
	public D getPredecessor();

	/**
	 * Clones this data flow abstraction
	 * 
	 * @return A clone of the current data flow abstraction
	 */
	public D clone();

	/**
	 * Clones this data flow abstraction with the current statement and corresponding call site set
	 *
	 * @return A clone of the current data flow abstraction
	 */
	public D clone(N currentUnit, N callSite);

	/**
	 * If this abstraction supports alias analysis, this returns the active copy of
	 * the current abstraction. Otherwise, "this" is returned.
	 * 
	 * @return The active copy if supported, otherwise the "this" reference
	 */
	public D getActiveCopy();

	/**
	 * Gets the length of the path over which this node was propagated
	 * 
	 * @return The length of the path over which this node was propagated
	 */
	public int getPathLength();

}
