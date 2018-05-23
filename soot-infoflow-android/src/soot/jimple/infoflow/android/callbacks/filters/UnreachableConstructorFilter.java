package soot.jimple.infoflow.android.callbacks.filters;

import soot.SootClass;
import soot.SootMethod;

/**
 * Filter for ruling out objects for which no factory method or allocation site
 * is reachable in the current component
 * 
 * @author Steven Arzt
 *
 */
public class UnreachableConstructorFilter extends AbstractCallbackFilter {

	@Override
	public boolean accepts(SootClass component, SootClass callbackHandler) {
		// If we have no reachability information, there is nothing we can do
		if (reachableMethods == null)
			return true;

		// If the callback is in the component class itself, it is trivially reachable
		if (component == callbackHandler)
			return true;

		{
			SootClass curHandler = callbackHandler;
			while (curHandler.isInnerClass()) {
				// Do not be overly aggressive for inner classes
				SootClass outerClass = curHandler.getOuterClass();
				if (component == outerClass)
					return true;

				// Make sure that we don't loop infinitely, even if everything is weird
				if (curHandler == outerClass)
					break;
				curHandler = outerClass;
			}
		}

		// Is this handler class instantiated in a reachable method?
		boolean hasConstructor = false;
		for (SootMethod sm : callbackHandler.getMethods()) {
			if (sm.isConstructor()) {
				if (reachableMethods.contains(sm)) {
					hasConstructor = true;
					break;
				}
			}
		}
		return hasConstructor;
	}

	@Override
	public boolean accepts(SootClass component, SootMethod callback) {
		// No filtering here
		return true;
	}

	@Override
	public void reset() {
		// nothing to do here
	}

}
