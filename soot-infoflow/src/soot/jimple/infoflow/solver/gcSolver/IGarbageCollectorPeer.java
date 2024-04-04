package soot.jimple.infoflow.solver.gcSolver;

/**
 * A garbage collector that can operate as part of a peer group
 * 
 * @author Steven Arzt
 *
 */
public interface IGarbageCollectorPeer<A> {

	/**
	 * Checks whether the given abstraction has any open dependencies in any of the
	 * solvers that are members of this peer group that prevent its jump functions
	 * from being garbage collected
	 * 
	 * @param abstraction The abstraction to check
	 * @return True if the abstraction has active dependencies and thus cannot be
	 *         garbage-collected, false otherwise
	 */
	public boolean hasActiveDependencies(A abstraction);

	public void notifySolverTerminated();
}
