package soot.jimple.infoflow.android.callbacks.filters;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.ComponentReachableMethods;

/**
 * Common interface for all callback filters. A callback filter takes the
 * discovered associations between host components and callbacks and filters out
 * those that are not of interest to the analysis.
 * 
 * @author Steven Arzt
 *
 */
public interface ICallbackFilter {

	/**
	 * Checks whether the given callback handler is usable in the given Android
	 * component
	 * 
	 * @param component
	 *            The component at whose runtime the callback is to be invoked
	 * @param callbackHandler
	 *            The class containing the callback implementation
	 * @return True if the filter accepts the callback to be registered, otherwise
	 *         false
	 */
	boolean accepts(SootClass component, SootClass callbackHandler);

	/**
	 * Checks whether the given concrete callback method is usable in the given
	 * Android component
	 * 
	 * @param component
	 *            The component at whose runtime the callback is to be invoked
	 * @param callback
	 *            The callback method to be invoked
	 * @return True if the filter accepts the callback to be registered, otherwise
	 *         false
	 */
	boolean accepts(SootClass component, SootMethod callback);

	/**
	 * Method that is called whenever the Soot scene changes and the class
	 * references need to be updated
	 */
	void reset();

	/**
	 * Sets the analysis that can provide the methods that are reachable inside the
	 * app. A callback analysis is free to pass a new analysis into the filter when
	 * changing its scope, e.g., when processing a new component.
	 * 
	 * @param rm
	 *            The reachable methods analysis
	 */
	void setReachableMethods(ComponentReachableMethods rm);

}
