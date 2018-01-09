package soot.jimple.infoflow.solver;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Common interface for handlers that take care of follow-return-past-seeds
 * cases which the normal solver does not handle
 * 
 * @author Steven Arzt
 *
 */
public interface IFollowReturnsPastSeedsHandler {
	
	/**
	 * This method is called when followReturnsPastSeeds is enabled and a taint
	 * leaves a method for which we do not have any callers.
	 * @param d1 The abstraction at the beginning of the callee
	 * @param u The return site
	 * @param d2 The abstraction at the return site
	 */
	public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2);
	
}
