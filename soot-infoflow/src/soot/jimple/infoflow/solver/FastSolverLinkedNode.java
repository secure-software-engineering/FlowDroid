package soot.jimple.infoflow.solver;

import soot.jimple.infoflow.data.SourceContext;

/**
 * Common interface for all abstractions processed by the IFDS solver
 * 
 * @author Steven Arzt
 */
public interface FastSolverLinkedNode<N> extends Cloneable {

	/**
	 * Clones this data flow abstraction
	 * 
	 * @return A clone of the current data flow abstraction
	 */
	public Object clone();

	/**
	 * Gets the length of the path over which this node was propagated
	 * 
	 * @return The length of the path over which this node was propagated
	 */
	public int getPathLength();

	public void deriveSourceContext(FastSolverLinkedNode<N> originalNode);

	public FastSolverLinkedNode<N> reduce();

	/**
	 * Gets the context of the taint, i.e. the statement and value of the source
	 * 
	 * @return The statement and value of the source
	 */
	public SourceContext getSourceContext();

}
