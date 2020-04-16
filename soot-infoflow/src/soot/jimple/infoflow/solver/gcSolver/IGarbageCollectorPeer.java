package soot.jimple.infoflow.solver.gcSolver;

import soot.SootMethod;

/**
 * A garbage collector that can operate as part of a peer group
 * 
 * @author Steven Arzt
 *
 */
public interface IGarbageCollectorPeer {

	/**
	 * Checks whether the given method has any open dependencies in any of the
	 * solvers that are members of this peer group that prevent its jump functions
	 * from being garbage collected
	 * 
	 * @param method The method to check
	 * @return True it the method has active dependencies and thus cannot be
	 *         garbage-collected, false otherwise
	 */
	public boolean hasActiveDependencies(SootMethod method);

}
