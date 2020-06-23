package soot.jimple.infoflow.solver.gcSolver;

/**
 * Possible triggers when to start garbage collection
 * 
 * @author Steven Arzt
 *
 */
public enum GarbageCollectionTrigger {

	/**
	 * Garbage collection is triggered immediately when a new edge has been
	 * propagated
	 */
	Immediate,

	/**
	 * Start garbage collection after the method threshold has been reached
	 */
	MethodThreshold,

	/**
	 * Start garbage collection after the edge threshold has been reached
	 */
	EdgeThreshold,

	/**
	 * Compute the thresholds, but never actually gargabe-collect anything. Useful
	 * only as a performance baseline.
	 */
	Never

}
