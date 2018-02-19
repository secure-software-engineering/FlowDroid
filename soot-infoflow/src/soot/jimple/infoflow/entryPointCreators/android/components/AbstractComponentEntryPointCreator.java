package soot.jimple.infoflow.entryPointCreators.android.components;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.android.AbstractAndroidEntryPointCreator;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Class for generating a dummy main method that represents the lifecycle of a
 * single component
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractComponentEntryPointCreator extends AbstractAndroidEntryPointCreator {

	protected final SootClass component;
	protected final SootClass applicationClass;
	protected Set<SootMethod> callbacks = null;

	public AbstractComponentEntryPointCreator(SootClass component, SootClass applicationClass) {
		this.component = component;
		this.applicationClass = applicationClass;
		this.overwriteDummyMainMethod = true;
	}

	public void setCallbacks(Set<SootMethod> callbacks) {
		this.callbacks = callbacks;
	}

	@Override
	protected void createEmptyMainMethod() {
		// Generate a method name
		String componentPart = component.getName();
		if (componentPart.contains("."))
			componentPart = componentPart.substring(componentPart.lastIndexOf(".") + 1);
		final String baseMethodName = dummyMethodName + "_" + componentPart;

		// Get the target method
		int methodIndex = 0;
		String methodName = baseMethodName;
		SootClass mainClass = Scene.v().getSootClass(dummyClassName);
		if (!overwriteDummyMainMethod)
			while (mainClass.declaresMethodByName(methodName))
				methodName = baseMethodName + "_" + methodIndex++;

		// Remove the existing main method if necessary. Do not clear the
		// existing one, this would take much too long.
		mainMethod = mainClass.getMethodByNameUnsafe(methodName);
		if (mainMethod != null) {
			mainClass.removeMethod(mainMethod);
			mainMethod = null;
		}

		// Create the method
		Type intentType = RefType.v("android.content.Intent");
		mainMethod = Scene.v().makeSootMethod(methodName, Collections.singletonList(intentType), VoidType.v());

		// Create the body
		JimpleBody body = Jimple.v().newBody();
		body.setMethod(mainMethod);
		mainMethod.setActiveBody(body);

		// Add the method to the class
		mainClass.addMethod(mainMethod);

		// First add class to scene, then make it an application class
		// as addClass contains a call to "setLibraryClass"
		mainClass.setApplicationClass();
		mainMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

		// Add the identity statements to the body. This must be done after the
		// method has been properly declared.
		body.insertIdentityStmts();
	}

	@Override
	public Collection<String> getRequiredClasses() {
		// Handled by the main Android entry point creator
		return null;
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		// Before-class marker
		Stmt beforeComponentStmt = Jimple.v().newNopStmt();
		body.getUnits().add(beforeComponentStmt);

		Stmt endClassStmt = Jimple.v().newReturnVoidStmt();
		try {
			// Create a new instance of the activity
			Local localVal = generateClassConstructor(component, body);
			if (localVal == null) {
				logger.warn("Constructor cannot be generated for {}", component.getName());
				return mainMethod;
			}
			localVarsForClasses.put(component, localVal);

			// We may skip the complete component
			createIfStmt(endClassStmt);

			// Create calls to the lifecycle methods
			generateComponentLifecycle(localVal);
			createIfStmt(beforeComponentStmt);
		} finally {
			body.getUnits().add(endClassStmt);
		}
		System.out.println(mainMethod.getActiveBody());
		return mainMethod;
	}

	/**
	 * Generates the component-specific portion of the lifecycle
	 * 
	 * @param componentLocal
	 *            The local the contains an instance of the component
	 */
	protected abstract void generateComponentLifecycle(Local componentLocal);

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		return null;
	}

	/**
	 * Generates invocation statements for all callback methods which need to be
	 * invoked during the given class' run cycle.
	 * 
	 * @return True if a matching callback has been found, otherwise false.
	 */
	protected boolean addCallbackMethods() {
		return addCallbackMethods(null, "");
	}

	/**
	 * Generates invocation statements for all callback methods which need to be
	 * invoked during the given class' run cycle.
	 * 
	 * @param referenceClasses
	 *            The classes for which no new instances shall be created, but
	 *            rather existing ones shall be used.
	 * @param callbackSignature
	 *            An empty string if calls to all callback methods for the given
	 *            class shall be generated, otherwise the subsignature of the only
	 *            callback method to generate.
	 * @return True if a matching callback has been found, otherwise false.
	 */
	protected boolean addCallbackMethods(Set<SootClass> referenceClasses, String callbackSignature) {
		// Do we have callbacks at all?
		if (callbacks == null)
			return false;

		// Get all classes in which callback methods are declared
		MultiMap<SootClass, SootMethod> callbackClasses = getCallbackMethods(callbackSignature);

		// The class for which we are generating the lifecycle always has an
		// instance.
		if (referenceClasses == null || referenceClasses.isEmpty())
			referenceClasses = Collections.singleton(component);
		else {
			referenceClasses = new HashSet<>(referenceClasses);
			referenceClasses.add(component);
		}

		Stmt beforeCallbacks = Jimple.v().newNopStmt();
		body.getUnits().add(beforeCallbacks);

		boolean callbackFound = false;
		for (SootClass callbackClass : callbackClasses.keySet()) {
			// If we already have a parent class that defines this callback, we
			// use it. Otherwise, we create a new one.
			boolean hasParentClass = false;
			for (SootClass parentClass : referenceClasses) {
				Local parentLocal = this.localVarsForClasses.get(parentClass);
				if (isCompatible(parentClass, callbackClass)) {
					// Create the method invocation
					addSingleCallbackMethod(referenceClasses, callbackClasses, callbackClass, parentLocal);
					callbackFound = true;
					hasParentClass = true;
				}
			}

			// We only create new instance if we were not able to find a
			// suitable parent class
			if (!hasParentClass) {
				// Check whether we already have a local
				Local classLocal = localVarsForClasses.get(callbackClass);

				// Create a new instance of this class
				// if we need to call a constructor, we insert the respective
				// Jimple statement here
				Set<Local> tempLocals = new HashSet<>();
				if (classLocal == null) {
					classLocal = generateClassConstructor(callbackClass, body, new HashSet<SootClass>(),
							referenceClasses, tempLocals);
					if (classLocal == null)
						continue;
				}

				addSingleCallbackMethod(referenceClasses, callbackClasses, callbackClass, classLocal);
				callbackFound = true;

				// Clean up the base local if we generated it
				for (Local tempLocal : tempLocals)
					body.getUnits().add(Jimple.v().newAssignStmt(tempLocal, NullConstant.v()));
			}
		}
		// jump back since we don't now the order of the callbacks
		if (callbackFound)
			createIfStmt(beforeCallbacks);

		return callbackFound;
	}

	/**
	 * Gets all callback methods registered for the given class
	 * 
	 * @param callbackSignature
	 *            An empty string if all callback methods for the given class shall
	 *            be return, otherwise the subsignature of the only callback method
	 *            to return.
	 * @return The callback methods registered for the given class
	 */
	private MultiMap<SootClass, SootMethod> getCallbackMethods(String callbackSignature) {
		MultiMap<SootClass, SootMethod> callbackClasses = new HashMultiMap<>();
		for (SootMethod theMethod : this.callbacks) {
			// Parse the callback
			if (!callbackSignature.isEmpty() && !callbackSignature.equals(theMethod.getSubSignature()))
				continue;

			// Check that we don't have one of the lifecycle methods as they are
			// treated separately.
			if (entryPointUtils.isEntryPointMethod(theMethod))
				continue;

			callbackClasses.put(theMethod.getDeclaringClass(), theMethod);
		}
		return callbackClasses;
	}

	/**
	 * Creates invocation statements for a single callback class
	 * 
	 * @param referenceClasses
	 *            The classes for which no new instances shall be created, but
	 *            rather existing ones shall be used.
	 * @param callbackClasses
	 *            The map between callback classes and their callback methods
	 * @param callbackClass
	 *            The class for which to create invocations
	 * @param classLocal
	 *            The base local of the respective class instance
	 */
	private void addSingleCallbackMethod(Set<SootClass> referenceClasses,
			MultiMap<SootClass, SootMethod> callbackClasses, SootClass callbackClass, Local classLocal) {
		for (SootMethod callbackMethod : callbackClasses.get(callbackClass)) {
			// We always create an opaque predicate to allow for skipping the
			// callback
			NopStmt thenStmt = Jimple.v().newNopStmt();
			createIfStmt(thenStmt);
			buildMethodCall(callbackMethod, body, classLocal, generator, referenceClasses);
			body.getUnits().add(thenStmt);
		}
	}

}
