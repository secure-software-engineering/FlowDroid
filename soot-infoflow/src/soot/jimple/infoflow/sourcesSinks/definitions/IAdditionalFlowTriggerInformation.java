package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

import soot.Local;
import soot.jimple.Stmt;

/**
 * Contains information about for what local variables to trigger additional
 * flows
 */
public interface IAdditionalFlowTriggerInformation {
	/**
	 * Returns a set of local variables for which to trigger additional flows
	 * 
	 * @param stmt the statement
	 * @return the set of local variables
	 */
	public Set<Local> getTriggeredAdditionalFlows(Stmt stmt);
}
