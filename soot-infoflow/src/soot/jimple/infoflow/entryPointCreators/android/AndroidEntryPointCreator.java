/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.entryPointCreators.android;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointUtils.ComponentType;
import soot.jimple.infoflow.entryPointCreators.android.components.AbstractComponentEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.components.ActivityEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.components.BroadcastReceiverEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.components.ContentProviderEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.components.ServiceConnectionEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.android.components.ServiceEntryPointCreator;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * class which creates a dummy main method with the entry points according to
 * the Android lifecycles
 * 
 * based on:
 * http://developer.android.com/reference/android/app/Activity.html#ActivityLifecycle
 * and http://developer.android.com/reference/android/app/Service.html and
 * http://developer.android.com/reference/android/content/BroadcastReceiver.html#ReceiverLifecycle
 * and
 * http://developer.android.com/reference/android/content/BroadcastReceiver.html
 * and https://developer.android.com/reference/android/app/Fragment.html
 * 
 * @author Christian, Steven Arzt
 * 
 */
public class AndroidEntryPointCreator extends AbstractAndroidEntryPointCreator implements IEntryPointCreator {

	private static final boolean DEBUG = false;

	protected MultiMap<SootClass, SootMethod> callbackFunctions = new HashMultiMap<>();;

	private SootClass applicationClass = null;
	private Local applicationLocal = null;

	private MultiMap<SootClass, String> activityLifecycleCallbacks = new HashMultiMap<>();
	private MultiMap<SootClass, String> applicationCallbackClasses = new HashMultiMap<>();
	private Map<SootClass, SootField> callbackClassToField = new HashMap<>();

	private MultiMap<SootClass, SootClass> fragmentClasses = null;
	private Map<SootClass, SootMethod> componentToEntryPoint = new HashMap<>();

	private final Collection<SootClass> androidClasses;

	/**
	 * Creates a new instance of the {@link AndroidEntryPointCreator} class and
	 * registers a list of classes to be automatically scanned for Android lifecycle
	 * methods
	 * 
	 * @param androidClasses
	 *            The list of classes to be automatically scanned for Android
	 *            lifecycle methods
	 */
	public AndroidEntryPointCreator(Collection<SootClass> androidClasses) {
		this.androidClasses = androidClasses;
		this.overwriteDummyMainMethod = true;
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		// Make sure that we don't have any leftover state
		// from previous runs
		reset();

		// For some weird reason unknown to anyone except the flying spaghetti
		// monster, the onCreate() methods of content providers run even before
		// the application object's onCreate() is called.
		{
			boolean hasContentProviders = false;
			NopStmt beforeContentProvidersStmt = Jimple.v().newNopStmt();
			body.getUnits().add(beforeContentProvidersStmt);
			for (SootClass currentClass : androidClasses) {
				if (entryPointUtils.getComponentType(currentClass) == ComponentType.ContentProvider) {
					// Create an instance of the content provider
					Local localVal = generateClassConstructor(currentClass, body);
					if (localVal == null) {
						logger.warn("Constructor cannot be generated for {}", currentClass.getName());
						continue;
					}
					localVarsForClasses.put(currentClass, localVal);

					// Conditionally call the onCreate method
					NopStmt thenStmt = Jimple.v().newNopStmt();
					createIfStmt(thenStmt);
					searchAndBuildMethod(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE, currentClass, localVal);
					body.getUnits().add(thenStmt);
					hasContentProviders = true;
				}
			}
			// Jump back to the beginning of this section to overapproximate the
			// order in which the methods are called
			if (hasContentProviders)
				createIfStmt(beforeContentProvidersStmt);
		}

		// If we have an implementation of android.app.Application, this needs
		// special treatment
		initializeApplicationClass();

		// If we have an application, we need to start it in the very beginning
		if (applicationClass != null) {
			// Create the application
			applicationLocal = generateClassConstructor(applicationClass, body);
			localVarsForClasses.put(applicationClass, applicationLocal);
			if (applicationLocal == null) {
				logger.warn("Constructor cannot be generated for application class {}", applicationClass.getName());
			} else {
				localVarsForClasses.put(applicationClass, applicationLocal);

				boolean hasApplicationCallbacks = applicationCallbackClasses != null
						&& !applicationCallbackClasses.isEmpty();
				boolean hasActivityLifecycleCallbacks = activityLifecycleCallbacks != null
						&& !activityLifecycleCallbacks.isEmpty();

				// Create instances of all application callback classes
				if (hasApplicationCallbacks || hasActivityLifecycleCallbacks) {
					NopStmt beforeCbCons = Jimple.v().newNopStmt();
					body.getUnits().add(beforeCbCons);

					if (hasApplicationCallbacks)
						createClassInstances(applicationCallbackClasses.keySet());
					if (hasActivityLifecycleCallbacks) {
						createClassInstances(activityLifecycleCallbacks.keySet());

						// Assign the instance to the field
						for (SootClass sc : activityLifecycleCallbacks.keySet()) {
							SootField fld = callbackClassToField.get(sc);
							Local lc = localVarsForClasses.get(sc);
							if (sc != null && lc != null)
								body.getUnits()
										.add(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(fld.makeRef()), lc));
						}
					}

					// Jump back to overapproximate the order in which the
					// constructors are called
					createIfStmt(beforeCbCons);
				}

				// Call the onCreate() method
				searchAndBuildMethod(AndroidEntryPointConstants.APPLICATION_ONCREATE, applicationClass,
						applicationLocal);

				//////////////
				// Initializes the ApplicationHolder static field with the
				////////////// singleton application
				// instance created above
				// (Used by the Activity::getApplication patched in
				////////////// LibraryClassPatcher)
				SootClass scApplicationHolder = LibraryClassPatcher.createOrGetApplicationHolder();
				body.getUnits()
						.add(Jimple.v().newAssignStmt(
								Jimple.v()
										.newStaticFieldRef(scApplicationHolder.getFieldByName("application").makeRef()),
								applicationLocal));
				//////////////
			}
		}

		// prepare outer loop:
		NopStmt outerStartStmt = Jimple.v().newNopStmt();
		body.getUnits().add(outerStartStmt);

		for (SootClass currentClass : androidClasses) {
			currentClass.setApplicationClass();

			// Get the callbacks and component type of the current component
			ComponentType componentType = entryPointUtils.getComponentType(currentClass);

			// Before-class marker
			Stmt beforeComponentStmt = Jimple.v().newNopStmt();
			body.getUnits().add(beforeComponentStmt);

			// Generate the lifecycles for the different kinds of Android
			// classes
			AbstractComponentEntryPointCreator componentCreator = null;
			switch (componentType) {
			case Activity:
				Set<SootClass> activityFragments = fragmentClasses == null ? null : fragmentClasses.get(currentClass);
				componentCreator = new ActivityEntryPointCreator(currentClass, applicationClass,
						activityLifecycleCallbacks, activityFragments, callbackClassToField);
				break;
			case Service:
			case GCMBaseIntentService:
			case GCMListenerService:
				componentCreator = new ServiceEntryPointCreator(currentClass, applicationClass);
				break;
			case ServiceConnection:
				componentCreator = new ServiceConnectionEntryPointCreator(currentClass, applicationClass);
				break;
			case BroadcastReceiver:
				componentCreator = new BroadcastReceiverEntryPointCreator(currentClass, applicationClass);
				break;
			case ContentProvider:
				componentCreator = new ContentProviderEntryPointCreator(currentClass, applicationClass);
				break;
			}

			// Create a call to the component's lifecycle method
			if (componentCreator != null) {
				componentCreator.setCallbacks(callbackFunctions.get(currentClass));
				SootMethod lifecycleMethod = componentCreator.createDummyMain();
				componentToEntryPoint.put(currentClass, lifecycleMethod);
				body.getUnits().add(Jimple.v()
						.newInvokeStmt(Jimple.v().newStaticInvokeExpr(lifecycleMethod.makeRef(), NullConstant.v())));
			}
		}

		// Add conditional calls to the application callback methods
		if (applicationLocal != null) {
			Unit beforeAppCallbacks = Jimple.v().newNopStmt();
			body.getUnits().add(beforeAppCallbacks);
			addApplicationCallbackMethods();
			createIfStmt(beforeAppCallbacks);
		}

		createIfStmt(outerStartStmt);

		// Add a call to application.onTerminate()
		if (applicationLocal != null)
			searchAndBuildMethod(AndroidEntryPointConstants.APPLICATION_ONTERMINATE, applicationClass,
					applicationLocal);

		body.getUnits().add(Jimple.v().newReturnVoidStmt());

		// Optimize and check the generated main method
		NopEliminator.v().transform(body);
		eliminateSelfLoops(body);
		eliminateFallthroughIfs(body);

		if (DEBUG || Options.v().validate())
			mainMethod.getActiveBody().validate();

		logger.debug("Generated main method:\n{}", body);

		System.out.println(mainMethod.getActiveBody());

		return mainMethod;
	}

	/**
	 * Find the application class and its callbacks
	 */
	private void initializeApplicationClass() {
		// Find the application class
		for (SootClass currentClass : androidClasses) {
			// Is this the application class?
			if (entryPointUtils.isApplicationClass(currentClass)) {
				if (applicationClass != null)
					throw new RuntimeException("Multiple application classes in app");
				applicationClass = currentClass;
				break;
			}
		}

		// We can only look for callbacks if we have an application class
		if (applicationClass == null)
			return;

		// Look into the application class' callbacks
		SootClass scActCallbacks = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACKSINTERFACE);
		Collection<SootMethod> callbacks = callbackFunctions.get(applicationClass);
		if (callbacks != null) {
			for (SootMethod smCallback : callbacks) {
				// Is this a special callback class? We have callbacks that model activity
				// lifecycle events and ones that model generic events (e.g., low memory)
				if (Scene.v().getOrMakeFastHierarchy().canStoreType(smCallback.getDeclaringClass().getType(),
						scActCallbacks.getType()))
					activityLifecycleCallbacks.put(smCallback.getDeclaringClass(), smCallback.getSignature());
				else
					applicationCallbackClasses.put(smCallback.getDeclaringClass(), smCallback.getSignature());
			}
		}

		// Create fields for the activity lifecycle classes
		for (SootClass callbackClass : activityLifecycleCallbacks.keySet()) {
			String baseName = callbackClass.getName();
			if (baseName.contains("."))
				baseName = baseName.substring(baseName.lastIndexOf(".") + 1);

			// Generate a fresh field name
			SootClass dummyMainClass = mainMethod.getDeclaringClass();
			int idx = 0;
			String fieldName = baseName;
			while (dummyMainClass.declaresFieldByName(fieldName)) {
				fieldName = baseName + "_" + idx;
				idx++;
			}
			SootField fld = new SootField(fieldName, RefType.v(callbackClass));
			mainMethod.getDeclaringClass().addField(fld);
			fld.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
			callbackClassToField.put(callbackClass, fld);
		}
	}

	/**
	 * Removes if statements that jump to the fall-through successor
	 * 
	 * @param body
	 *            The body from which to remove unnecessary if statements
	 */
	private void eliminateFallthroughIfs(Body body) {
		boolean changed = false;
		do {
			changed = false;
			IfStmt ifs = null;
			Iterator<Unit> unitIt = body.getUnits().snapshotIterator();
			while (unitIt.hasNext()) {
				Unit u = unitIt.next();
				if (ifs != null && ifs.getTarget() == u) {
					body.getUnits().remove(ifs);
					changed = true;
				}
				ifs = null;
				if (u instanceof IfStmt)
					ifs = (IfStmt) u;
			}
		} while (changed);
	}

	/**
	 * Adds calls to the callback methods defined in the application class
	 * 
	 * @param applicationClass
	 *            The class in which the user-defined application is implemented
	 * @param applicationLocal
	 *            The local containing the instance of the user-defined application
	 */
	private void addApplicationCallbackMethods() {
		if (!this.callbackFunctions.containsKey(applicationClass))
			return;

		// Do not try to generate calls to methods in non-concrete classes
		if (applicationClass.isAbstract())
			return;
		if (applicationClass.isPhantom()) {
			logger.warn("Skipping possible application callbacks in " + "phantom class %s", applicationClass);
			return;
		}

		for (SootClass sc : applicationCallbackClasses.keySet())
			for (String methodSig : applicationCallbackClasses.get(sc)) {
				SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v().parseSootMethodString(methodSig);
				String subSig = methodAndClass.getSubSignature();
				SootMethod method = findMethod(Scene.v().getSootClass(sc.getName()), subSig);

				// We do not consider lifecycle methods which are directly
				// inserted
				// at their respective positions
				if (sc == applicationClass
						&& AndroidEntryPointConstants.getApplicationLifecycleMethods().contains(subSig))
					continue;

				// If this is an activity lifecycle method, we skip it as well
				// TODO: can be removed once we filter it in general
				if (activityLifecycleCallbacks.containsKey(sc))
					if (AndroidEntryPointConstants.getActivityLifecycleCallbackMethods().contains(subSig))
						continue;

				// If we found no implementation or if the implementation we found is in a
				// system class, we skip it. Note that null methods may happen since all
				// callback interfaces for application callbacks are registered under the name
				// of the application class.
				if (method == null)
					continue;
				if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
					continue;

				// Get the local instance of the target class
				Local local = this.localVarsForClasses.get(sc);
				if (local == null) {
					logger.warn(String.format("Could not create call to application callback %s. Local was null.",
							method.getSignature()));
					continue;
				}

				// Add a conditional call to the method
				NopStmt thenStmt = Jimple.v().newNopStmt();
				createIfStmt(thenStmt);
				buildMethodCall(method, body, local, generator);
				body.getUnits().add(thenStmt);
			}
	}

	@Override
	public Collection<String> getRequiredClasses() {
		Set<String> requiredClasses = new HashSet<String>(androidClasses.size());
		for (SootClass sc : androidClasses)
			requiredClasses.add(sc.getName());
		return requiredClasses;
	}

	public void setFragments(MultiMap<SootClass, SootClass> fragments) {
		fragmentClasses = fragments;
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		return componentToEntryPoint.values();
	}

	public Map<SootClass, SootMethod> getComponentToEntryPointMap() {
		return componentToEntryPoint;
	}

	/**
	 * Sets the list of callback functions to be integrated into the Android
	 * lifecycle
	 * 
	 * @param callbackFunctions
	 *            The list of callback functions to be integrated into the Android
	 *            lifecycle. This is a mapping from the Android element class
	 *            (activity, service, etc.) to the list of callback methods for that
	 *            element.
	 */
	public void setCallbackFunctions(MultiMap<SootClass, SootMethod> callbackFunctions) {
		this.callbackFunctions = callbackFunctions;
	}

	/**
	 * Returns the list of callback functions of the Android lifecycle.
	 * 
	 * @return callbackFunctions The list of callback functions of the Android
	 *         lifecycle. This is a mapping from the Android element class
	 *         (activity, service, etc.) to the list of callback methods for that
	 *         element.
	 */
	public MultiMap<SootClass, SootMethod> getCallbackFunctions() {
		return callbackFunctions;
	}

	@Override
	protected void reset() {
		super.reset();

		// Get rid of the generated component methods
		for (SootMethod sm : componentToEntryPoint.values())
			sm.getDeclaringClass().removeMethod(sm);
		componentToEntryPoint.clear();

		// Get rid of the generated fields
		for (SootField fld : callbackClassToField.values())
			fld.getDeclaringClass().removeField(fld);
		callbackClassToField.clear();
	}

}
