package soot.jimple.infoflow.memory;

/**
 * Common interface for all classes that describe why a solver was forcefully
 * terminated
 * 
 * @author Steven Arzt
 *
 */
public interface ISolverTerminationReason {

	/**
	 * Combines this termination reason with the given one
	 * 
	 * @param terminationReason
	 *            The reason to combine this one with
	 * @return A combined termination reason that models the current reason as well
	 *         as the given one, i.e., the fact that the solver was terminated for
	 *         multiple reasons
	 */
	public ISolverTerminationReason combine(ISolverTerminationReason terminationReason);

}
