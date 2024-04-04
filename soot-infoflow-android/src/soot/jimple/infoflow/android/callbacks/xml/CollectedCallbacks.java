package soot.jimple.infoflow.android.callbacks.xml;

import java.util.Set;

import soot.SootClass;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.util.MultiMap;

/**
 * Data object for storing collected callbacks for serialization and re-use
 * 
 * @author Steven Arzt
 *
 */
public class CollectedCallbacks {

	protected Set<SootClass> entryPoints;
	protected MultiMap<SootClass, AndroidCallbackDefinition> callbackMethods;
	protected MultiMap<SootClass, SootClass> fragmentClasses;

	public CollectedCallbacks() {

	}

	public CollectedCallbacks(Set<SootClass> entryPoints,
			MultiMap<SootClass, AndroidCallbackDefinition> callbackMethods,
			MultiMap<SootClass, SootClass> fragmentClasses) {
		this.entryPoints = entryPoints;
		this.callbackMethods = callbackMethods;
		this.fragmentClasses = fragmentClasses;
	}

	public Set<SootClass> getEntryPoints() {
		return entryPoints;
	}

	public MultiMap<SootClass, AndroidCallbackDefinition> getCallbackMethods() {
		return callbackMethods;
	}

	public MultiMap<SootClass, SootClass> getFragmentClasses() {
		return fragmentClasses;
	}

}
