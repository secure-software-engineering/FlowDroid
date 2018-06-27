package soot.jimple.infoflow.taintWrappers;

/**
 * Common interface for all data flow analyses that support taint wrappers
 * 
 * @author Steven Arzt
 *
 */
public interface ITaintWrapperDataFlowAnalysis {

	/**
	 * Sets the taint wrapper to be used for propagating taints over unknown
	 * (library) callees. If this value is null, no taint wrapping is used.
	 * 
	 * @param taintWrapper
	 *            The taint wrapper to use or null to disable taint wrapping
	 */
	public void setTaintWrapper(ITaintPropagationWrapper taintWrapper);

	/**
	 * Gets the taint wrapper to be used for propagating taints over unknown
	 * (library) callees. If this value is null, no taint wrapping is used.
	 * 
	 * @return The taint wrapper to use or null if taint wrapping is disabled
	 */
	public ITaintPropagationWrapper getTaintWrapper();

}
