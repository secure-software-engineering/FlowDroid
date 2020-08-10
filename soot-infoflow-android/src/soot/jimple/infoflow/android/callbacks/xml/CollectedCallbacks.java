package soot.jimple.infoflow.android.callbacks.xml;

import java.util.Collection;
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

	protected Collection<SootClass> entryPoints;
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

}
