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
package soot.jimple.infoflow.entryPointCreators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import heros.TwoElementSet;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointUtils.ComponentType;
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
public class AndroidEntryPointCreator extends BaseEntryPointCreator implements IEntryPointCreator {

	private static final boolean DEBUG = false;

	private JimpleBody body;
	private LocalGenerator generator;
	private int conditionCounter;
	private Value intCounter;

	private SootClass applicationClass = null;
	private Local applicationLocal = null;
	private MultiMap<SootClass, String> activityLifecycleCallbacks = new HashMultiMap<>();
	private MultiMap<SootClass, String> applicationCallbackClasses = new HashMultiMap<>();
	private MultiMap<SootClass, SootClass> fragmentClasses = null;

	private final Collection<SootClass> androidClasses;
	private final Collection<String> additionalEntryPoints;

	private MultiMap<SootClass, SootMethod> callbackFunctions;
	private boolean modelAdditionalMethods = false;

	private AndroidEntryPointUtils entryPointUtils = null;

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
		this(androidClasses, Collections.<String>emptySet());
	}

	/**
	 * Creates a new instance of the {@link AndroidEntryPointCreator} class and
	 * registers a list of classes to be automatically scanned for Android lifecycle
	 * methods
	 * 
	 * @param androidClasses
	 *            The list of classes to be automatically scanned for Android
	 *            lifecycle methods
	 * @param additionalEntryPoints
	 *            Additional entry points to be called during the running phase of
	 *            the respective component. These values must be valid Soot method
	 *            signatures.
	 */
	public AndroidEntryPointCreator(Collection<SootClass> androidClasses, Collection<String> additionalEntryPoints) {
		this.androidClasses = androidClasses;
		this.additionalEntryPoints = additionalEntryPoints;
		this.callbackFunctions = new HashMultiMap<>();
		this.overwriteDummyMainMethod = true;
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
	protected SootMethod createDummyMainInternal(SootMethod emptySootMethod) {
		// Make sure that we don't have any leftover state
		// from previous runs
		reset();

		// Initialize the utility class
		this.entryPointUtils = new AndroidEntryPointUtils();

		MultiMap<String, String> classMap = SootMethodRepresentationParser.v().parseClassNames2(additionalEntryPoints,
				false);
		for (SootClass androidClass : this.androidClasses)
			if (!classMap.containsKey(androidClass.getName()))
				classMap.put(androidClass.getName(), null);

		//
		SootMethod mainMethod = emptySootMethod;
		body = (JimpleBody) emptySootMethod.getActiveBody();
		generator = new LocalGenerator(body);

		// add entrypoint calls
		conditionCounter = 0;
		intCounter = generator.generateLocal(IntType.v());

		AssignStmt assignStmt = Jimple.v().newAssignStmt(intCounter, IntConstant.v(conditionCounter));
		body.getUnits().add(assignStmt);

		// Resolve all requested classes
		for (String className : classMap.keySet())
			Scene.v().forceResolve(className, SootClass.SIGNATURES);

		// For some weird reason unknown to anyone except the flying spaghetti
		// monster, the onCreate() methods of content providers run even before
		// the application object's onCreate() is called.
		{
			boolean hasContentProviders = false;
			NopStmt beforeContentProvidersStmt = Jimple.v().newNopStmt();
			body.getUnits().add(beforeContentProvidersStmt);
			for (String className : classMap.keySet()) {
				SootClass currentClass = Scene.v().getSootClass(className);
				if (entryPointUtils.getComponentType(currentClass) == ComponentType.ContentProvider) {
					// Create an instance of the content provider
					Local localVal = generateClassConstructor(currentClass, body);
					if (localVal == null) {
						logger.warn("Constructor cannot be generated for {}", currentClass.getName());
						continue;
					}
					localVarsForClasses.put(currentClass.getName(), localVal);

					// Conditionally call the onCreate method
					NopStmt thenStmt = Jimple.v().newNopStmt();
					createIfStmt(thenStmt);
					buildMethodCall(findMethod(currentClass, AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE), body,
							localVal, generator);
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
		initializeApplicationClass(classMap);

		// If we have an application, we need to start it in the very beginning
		if (applicationClass != null) {
			// Create the application
			applicationLocal = generateClassConstructor(applicationClass, body);
			if (applicationLocal == null) {
				logger.warn("Constructor cannot be generated for application class {}", applicationClass.getName());
			} else {
				localVarsForClasses.put(applicationClass.getName(), applicationLocal);

				// Create instances of all application callback classes
				if (!applicationCallbackClasses.isEmpty() || !activityLifecycleCallbacks.isEmpty()) {
					NopStmt beforeCbCons = Jimple.v().newNopStmt();
					body.getUnits().add(beforeCbCons);

					createClassInstances(applicationCallbackClasses.keySet());
					createClassInstances(activityLifecycleCallbacks.keySet());
					createClassInstances(fragmentClasses.keySet());

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

		for (String className : classMap.keySet()) {
			// no execution order given for all apps:
			NopStmt entryExitStmt = Jimple.v().newNopStmt();
			createIfStmt(entryExitStmt);

			SootClass currentClass = Scene.v().getSootClass(className);
			currentClass.setApplicationClass();
			NopStmt endClassStmt = Jimple.v().newNopStmt();

			Set<String> callbackSigs = classMap.get(className);

			try {
				ComponentType componentType = entryPointUtils.getComponentType(currentClass);

				// Check if one of the methods is instance. This tells us
				// whether we need to create a constructor invocation or not.
				// Furthermore, we collect references to the corresponding
				// SootMethod objects.
				boolean instanceNeeded = componentType != ComponentType.Plain;
				Map<String, SootMethod> plainMethods = new HashMap<String, SootMethod>();
				if (!instanceNeeded && callbackSigs != null)
					for (String method : callbackSigs) {
						if (method != null && !method.isEmpty()) {
							SootMethod sm = null;

							// Find the method. It may either be implemented
							// directly in the
							// given class or it may be inherited from one of
							// the superclasses.
							if (Scene.v().containsMethod(method))
								sm = Scene.v().getMethod(method);
							else {
								SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v()
										.parseSootMethodString(method);
								if (!Scene.v().containsClass(methodAndClass.getClassName())) {
									logger.warn("Class for entry point {} not found, skipping...", method);
									continue;
								}
								sm = findMethod(Scene.v().getSootClass(methodAndClass.getClassName()),
										methodAndClass.getSubSignature());
								if (sm == null) {
									logger.warn("Method for entry point {} not found in class, skipping...", method);
									continue;
								}
							}

							plainMethods.put(method, sm);
							if (!sm.isStatic())
								instanceNeeded = true;
						}
					}

				// Before-class marker
				Stmt beforeComponentStmt = Jimple.v().newNopStmt();
				body.getUnits().add(beforeComponentStmt);

				// if we need to call a constructor, we insert the respective
				// Jimple statement here
				if (instanceNeeded && !localVarsForClasses.containsKey(currentClass.getName())) {
					Local localVal = generateClassConstructor(currentClass, body);
					if (localVal == null) {
						logger.warn("Constructor cannot be generated for {}", currentClass.getName());
						continue;
					}
					localVarsForClasses.put(currentClass.getName(), localVal);
				}
				Local classLocal = localVarsForClasses.get(className);

				// Generate the lifecycles for the different kinds of Android
				// classes
				switch (componentType) {
				case Activity:
					generateActivityLifecycle(callbackSigs, currentClass, endClassStmt, classLocal,
							beforeComponentStmt);
					break;
				case Service:
				case GCMBaseIntentService:
				case GCMListenerService:
					generateServiceLifecycle(callbackSigs, currentClass, endClassStmt, classLocal);
					break;
				// case Fragment:
				// generateFragmentLifecycle(entry.getValue(), currentClass,
				// endClassStmt, classLocal);
				// break;
				case ServiceConnection:
					generateServiceConnetionLifecycle(callbackSigs, currentClass, endClassStmt, classLocal);
					break;
				case BroadcastReceiver:
					generateBroadcastReceiverLifecycle(callbackSigs, currentClass, endClassStmt, classLocal);
					break;
				case ContentProvider:
					generateContentProviderLifecycle(callbackSigs, currentClass, endClassStmt, classLocal);
					break;
				case Plain:
					// Allow the complete class to be skipped
					createIfStmt(endClassStmt);

					NopStmt beforeClassStmt = Jimple.v().newNopStmt();
					body.getUnits().add(beforeClassStmt);
					for (SootMethod currentMethod : plainMethods.values()) {
						if (!currentMethod.isStatic() && classLocal == null) {
							logger.warn("Skipping method {} because we have no instance", currentMethod);
							continue;
						}

						// Create a conditional call on the current method
						NopStmt thenStmt = Jimple.v().newNopStmt();
						createIfStmt(thenStmt);
						buildMethodCall(currentMethod, body, classLocal, generator);
						body.getUnits().add(thenStmt);

						// Because we don't know the order of the custom
						// statements,
						// we assume that you can loop arbitrarily
						createIfStmt(beforeClassStmt);
					}
					break;
				}
			} finally {
				body.getUnits().add(endClassStmt);
				body.getUnits().add(entryExitStmt);
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
		return mainMethod;
	}

	/**
	 * Creates instance of the given classes
	 * 
	 * @param classes
	 *            The classes of which to create instances
	 */
	private void createClassInstances(Collection<SootClass> classes) {
		for (SootClass callbackClass : classes) {
			NopStmt thenStmt = Jimple.v().newNopStmt();
			createIfStmt(thenStmt);
			Local l = localVarsForClasses.get(callbackClass.getName());
			if (l == null) {
				l = generateClassConstructor(callbackClass, body, Collections.singleton(applicationClass));
				if (l != null)
					localVarsForClasses.put(callbackClass.getName(), l);
			}
			body.getUnits().add(thenStmt);
		}
	}

	/**
	 * Find the application class and its callbacks
	 * 
	 * @param classMap
	 *            A mapping between a component and its callback handlers
	 */
	private void initializeApplicationClass(MultiMap<String, String> classMap) {
		// Find the application class
		for (String className : classMap.keySet()) {
			SootClass currentClass = Scene.v().getSootClass(className);
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
				// Is this a special callback class?
				if (Scene.v().getOrMakeFastHierarchy().canStoreType(smCallback.getDeclaringClass().getType(),
						scActCallbacks.getType()))
					activityLifecycleCallbacks.put(smCallback.getDeclaringClass(), smCallback.getSignature());
				applicationCallbackClasses.put(smCallback.getDeclaringClass(), smCallback.getSignature());
			}
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
	 * Generates the lifecycle for an Android content provider class
	 * 
	 * @param entryPoints
	 *            The list of methods to consider in this class
	 * @param currentClass
	 *            The class for which to build the content provider lifecycle
	 * @param endClassStmt
	 *            The statement to which to jump after completing the lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateContentProviderLifecycle(Set<String> entryPoints, SootClass currentClass, NopStmt endClassStmt,
			Local classLocal) {
		// As we don't know the order in which the different Android lifecycles
		// run, we allow for each single one of them to be skipped
		createIfStmt(endClassStmt);

		// ContentProvider.onCreate() runs before everything else, even before
		// Application.onCreate(). We must thus handle it elsewhere.
		// Stmt onCreateStmt =
		// searchAndBuildMethod(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE,
		// currentClass, entryPoints, classLocal);

		// see:
		// http://developer.android.com/reference/android/content/ContentProvider.html
		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		boolean hasAdditionalMethods = false;
		if (entryPoints != null && modelAdditionalMethods) {
			for (SootMethod currentMethod : currentClass.getMethods())
				if (entryPoints.contains(currentMethod.toString()))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);
		}
		addCallbackMethods(currentClass);
		body.getUnits().add(endWhileStmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhileStmt);
		// createIfStmt(onCreateStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

	/**
	 * Generates the lifecycle for an Android broadcast receiver class
	 * 
	 * @param entryPoints
	 *            The list of methods to consider in this class
	 * @param currentClass
	 *            The class for which to build the broadcast receiver lifecycle
	 * @param endClassStmt
	 *            The statement to which to jump after completing the lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateBroadcastReceiverLifecycle(Set<String> entryPoints, SootClass currentClass,
			NopStmt endClassStmt, Local classLocal) {
		// As we don't know the order in which the different Android lifecycles
		// run, we allow for each single one of them to be skipped
		createIfStmt(endClassStmt);

		Stmt onReceiveStmt = searchAndBuildMethod(AndroidEntryPointConstants.BROADCAST_ONRECEIVE, currentClass,
				classLocal);
		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		boolean hasAdditionalMethods = false;
		if (entryPoints != null && modelAdditionalMethods) {
			for (SootMethod currentMethod : currentClass.getMethods())
				if (entryPoints.contains(currentMethod.toString()))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);
		}
		addCallbackMethods(currentClass);
		body.getUnits().add(endWhileStmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhileStmt);
		createIfStmt(onReceiveStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

	private void generateServiceConnetionLifecycle(Set<String> entryPoints, SootClass currentClass,
			NopStmt endClassStmt, Local classLocal) {
		// As we don't know the order in which the different Android lifecycles
		// run, we allow for each single one of them to be skipped
		createIfStmt(endClassStmt);

		Stmt onServiceConnectedStmt = searchAndBuildMethod(
				AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICECONNECTED, currentClass, classLocal);
		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		boolean hasAdditionalMethods = false;
		if (entryPoints != null && modelAdditionalMethods) {
			for (SootMethod currentMethod : currentClass.getMethods())
				if (entryPoints.contains(currentMethod.toString()))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);
		}
		addCallbackMethods(currentClass);
		body.getUnits().add(endWhileStmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhileStmt);
		createIfStmt(onServiceConnectedStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

	/**
	 * Generates the lifecycle for an Android service class
	 * 
	 * @param entryPoints
	 *            The list of methods to consider in this class
	 * @param currentClass
	 *            The class for which to build the service lifecycle
	 * @param endClassStmt
	 *            The statement to which to jump after completing the lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateServiceLifecycle(Set<String> entryPoints, SootClass currentClass, NopStmt endClassStmt,
			Local classLocal) {
		// 1. onCreate:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONCREATE, currentClass, classLocal);

		// service has two different lifecycles:
		// lifecycle1:
		// 2. onStart:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONSTART1, currentClass, classLocal);

		// onStartCommand can be called an arbitrary number of times, or never
		NopStmt beforeStartCommand = Jimple.v().newNopStmt();
		NopStmt afterStartCommand = Jimple.v().newNopStmt();
		body.getUnits().add(beforeStartCommand);
		createIfStmt(afterStartCommand);

		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONSTART2, currentClass, classLocal);
		createIfStmt(beforeStartCommand);
		body.getUnits().add(afterStartCommand);

		// methods:
		// all other entryPoints of this class:
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		ComponentType componentType = entryPointUtils.getComponentType(currentClass);
		boolean hasAdditionalMethods = false;
		if (entryPoints != null && modelAdditionalMethods) {
			for (SootMethod currentMethod : currentClass.getMethods())
				if (entryPoints.contains(currentMethod.toString()))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);
		}
		if (componentType == ComponentType.GCMBaseIntentService) {
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null
						&& !sm.getDeclaringClass().getName()
								.equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, sm);
			}
		} else if (componentType == ComponentType.GCMListenerService) {
			for (String sig : AndroidEntryPointConstants.getGCMListenerServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null
						&& !sm.getDeclaringClass().getName().equals(AndroidEntryPointConstants.GCMLISTENERSERVICECLASS))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, sm);
			}
		}
		addCallbackMethods(currentClass);
		body.getUnits().add(endWhileStmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhileStmt);

		// lifecycle1 end

		// lifecycle2 start
		// onBind:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONBIND, currentClass, classLocal);

		NopStmt beforemethodsStmt = Jimple.v().newNopStmt();
		body.getUnits().add(beforemethodsStmt);
		// methods
		NopStmt startWhile2Stmt = Jimple.v().newNopStmt();
		NopStmt endWhile2Stmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhile2Stmt);
		hasAdditionalMethods = false;
		if (modelAdditionalMethods) {
			for (SootMethod currentMethod : currentClass.getMethods())
				if (entryPoints.contains(currentMethod.toString()))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);
		}
		if (componentType == ComponentType.GCMBaseIntentService)
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null && !sm.getName().equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					hasAdditionalMethods |= createPlainMethodCall(classLocal, sm);
			}
		addCallbackMethods(currentClass);
		body.getUnits().add(endWhile2Stmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhile2Stmt);

		// onUnbind:
		Stmt onDestroyStmt = Jimple.v().newNopStmt();
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONUNBIND, currentClass, classLocal);
		createIfStmt(onDestroyStmt); // fall through to rebind or go to destroy

		// onRebind:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONREBIND, currentClass, classLocal);
		createIfStmt(beforemethodsStmt);

		// lifecycle2 end

		// onDestroy:
		body.getUnits().add(onDestroyStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONDESTROY, currentClass, classLocal);
		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));

		// either begin or end or next class:
		// createIfStmt(onCreateStmt); // no, the process gets killed in between
	}

	private boolean createPlainMethodCall(Local classLocal, SootMethod currentMethod) {
		// Do not create calls to lifecycle methods which we handle explicitly
		if (AndroidEntryPointConstants.getServiceLifecycleMethods().contains(currentMethod.getSubSignature()))
			return false;

		NopStmt beforeStmt = Jimple.v().newNopStmt();
		NopStmt thenStmt = Jimple.v().newNopStmt();
		body.getUnits().add(beforeStmt);
		createIfStmt(thenStmt);
		buildMethodCall(currentMethod, body, classLocal, generator);

		body.getUnits().add(thenStmt);
		createIfStmt(beforeStmt);
		return true;
	}

	/**
	 * Generates the lifecycle for an Android activity
	 * 
	 * @param entryPoints
	 *            The list of methods to consider in this class
	 * @param currentClass
	 *            The class for which to build the activity lifecycle
	 * @param endClassStmt
	 *            The statement to which to jump after completing the lifecycle
	 * @param beforeClassStmt
	 *            The statement right before the activity lifecycle begins
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateActivityLifecycle(Set<String> entryPoints, SootClass currentClass, NopStmt endClassStmt,
			Local classLocal, Stmt beforeClassStmt) {
		Set<SootClass> currentClassSet = Collections.singleton(currentClass);

		// As we don't know the order in which the different Android lifecycles
		// run, we allow for each single one of them to be skipped
		createIfStmt(endClassStmt);

		Set<SootClass> referenceClasses = new HashSet<SootClass>();
		if (applicationClass != null)
			referenceClasses.add(applicationClass);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet())
			referenceClasses.add(callbackClass);
		for (SootClass callbackClass : this.applicationCallbackClasses.keySet())
			referenceClasses.add(callbackClass);
		referenceClasses.add(currentClass);

		// 1. onCreate:
		{
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATE, currentClass, classLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED,
						callbackClass, localVarsForClasses.get(callbackClass.getName()), currentClassSet);
			}
		}

		// Adding the lifecycle of the Fragments that belong to this Activity:
		// iterate through the fragments detected in the CallbackAnalyzer
		if (fragmentClasses != null && !fragmentClasses.isEmpty()) {
			for (SootClass scFragment : fragmentClasses.get(currentClass)) {
				// Get a class local
				boolean generatedFragmentLocal = false;
				Local fragmentLocal = localVarsForClasses.get(scFragment.getName());
				Set<Local> tempLocals = new HashSet<>();
				if (fragmentLocal == null) {
					fragmentLocal = generateClassConstructor(scFragment, body, new HashSet<SootClass>(),
							referenceClasses, tempLocals);
					if (fragmentLocal == null)
						continue;
					generatedFragmentLocal = true;
				}

				// The onAttachFragment() callbacks tells the activity that a
				// new fragment was attached
				TwoElementSet<SootClass> classAndFragment = new TwoElementSet<SootClass>(currentClass, scFragment);
				Stmt afterOnAttachFragment = Jimple.v().newNopStmt();
				createIfStmt(afterOnAttachFragment);
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONATTACHFRAGMENT, currentClass, classLocal,
						classAndFragment);
				body.getUnits().add(afterOnAttachFragment);

				// Render the fragment lifecycle
				generateFragmentLifecycle(entryPoints, scFragment, endClassStmt, fragmentLocal, currentClass);

				// Get rid of the locals
				if (generatedFragmentLocal) {
					body.getUnits().add(Jimple.v().newAssignStmt(fragmentLocal, NullConstant.v()));
					for (Local tempLocal : tempLocals)
						body.getUnits().add(Jimple.v().newAssignStmt(tempLocal, NullConstant.v()));
				}
			}
		}

		// 2. onStart:
		Stmt onStartStmt;
		{
			onStartStmt = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSTART, currentClass, classLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				Stmt s = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
						callbackClass, localVarsForClasses.get(callbackClass.getName()), currentClassSet);
				if (onStartStmt == null)
					onStartStmt = s;
			}

			// If we don't have an onStart method, we need to create a
			// placeholder so that we
			// have somewhere to jump
			if (onStartStmt == null)
				body.getUnits().add(onStartStmt = Jimple.v().newNopStmt());

		}
		// onRestoreInstanceState is optional, the system only calls it if a
		// state has previously been stored.
		{
			Stmt afterOnRestore = Jimple.v().newNopStmt();
			createIfStmt(afterOnRestore);
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESTOREINSTANCESTATE, currentClass, classLocal,
					currentClassSet);
			body.getUnits().add(afterOnRestore);
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPOSTCREATE, currentClass, classLocal);

		// 3. onResume:
		Stmt onResumeStmt = Jimple.v().newNopStmt();
		body.getUnits().add(onResumeStmt);
		{
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESUME, currentClass, classLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED,
						callbackClass, localVarsForClasses.get(callbackClass.getName()), currentClassSet);
			}
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPOSTRESUME, currentClass, classLocal);

		// Scan for other entryPoints of this class:
		{
			boolean hasMethodsToInvoke = false;
			if (entryPoints != null) {
				if (modelAdditionalMethods)
					for (SootMethod currentMethod : currentClass.getMethods())
						if (entryPoints.contains(currentMethod.toString())
								&& !AndroidEntryPointConstants.getActivityLifecycleMethods()
										.contains(currentMethod.getSubSignature())) {
							hasMethodsToInvoke = true;
							break;
						}
			}

			boolean hasCallbacks = this.callbackFunctions.containsKey(currentClass);
			if (hasMethodsToInvoke || hasCallbacks) {
				NopStmt startWhileStmt = Jimple.v().newNopStmt();
				NopStmt endWhileStmt = Jimple.v().newNopStmt();
				body.getUnits().add(startWhileStmt);
				createIfStmt(endWhileStmt);

				// Add the callbacks
				addCallbackMethods(currentClass);

				// Add the other entry points
				boolean hasAdditionalMethods = false;
				if (hasMethodsToInvoke)
					for (SootMethod currentMethod : currentClass.getMethods())
						if (entryPoints.contains(currentMethod.toString()))
							hasAdditionalMethods |= createPlainMethodCall(classLocal, currentMethod);

				body.getUnits().add(endWhileStmt);
				if (hasAdditionalMethods)
					createIfStmt(startWhileStmt);
			}
		}

		// 4. onPause:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPAUSE, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED, callbackClass,
					localVarsForClasses.get(callbackClass.getName()), currentClassSet);
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATEDESCRIPTION, currentClass, classLocal);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSAVEINSTANCESTATE, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
					callbackClass, localVarsForClasses.get(callbackClass.getName()), currentClassSet);
		}

		// goTo Stop, Resume or Create:
		// (to stop is fall-through, no need to add)
		createIfStmt(onResumeStmt);
		// createIfStmt(onCreateStmt); // no, the process gets killed in between

		// 5. onStop:
		Stmt onStop = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSTOP, currentClass, classLocal);
		boolean hasAppOnStop = false;
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			Stmt onActStoppedStmt = searchAndBuildMethod(
					AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED, callbackClass,
					localVarsForClasses.get(callbackClass.getName()), currentClassSet);
			hasAppOnStop |= onActStoppedStmt != null;
		}
		if (hasAppOnStop && onStop != null)
			createIfStmt(onStop);

		// goTo onDestroy, onRestart or onCreate:
		// (to restart is fall-through, no need to add)
		NopStmt stopToDestroyStmt = Jimple.v().newNopStmt();
		createIfStmt(stopToDestroyStmt);
		// createIfStmt(onCreateStmt); // no, the process gets killed in between

		// 6. onRestart:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESTART, currentClass, classLocal);
		createIfStmt(onStartStmt); // jump to onStart(), fall through to
									// onDestroy()

		// 7. onDestroy
		body.getUnits().add(stopToDestroyStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONDESTROY, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED,
					callbackClass, localVarsForClasses.get(callbackClass.getName()), currentClassSet);
		}

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
		createIfStmt(beforeClassStmt);
	}

	/**
	 * Generates the lifecycle for an Android Fragment class
	 * 
	 * @param entryPoints
	 *            The list of methods to consider in this class
	 * @param currentClass
	 *            The class for which to build the fragment lifecycle
	 * @param endClassStmt
	 *            The statement to which to jump after completing the lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 * 
	 */
	private void generateFragmentLifecycle(Set<String> entryPoints, SootClass currentClass, NopStmt endClassStmt,
			Local classLocal, SootClass activity) {
		createIfStmt(endClassStmt);

		// 1. onAttach:
		Stmt onAttachStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONATTACH, currentClass, classLocal,
				Collections.singleton(activity));
		if (onAttachStmt == null)
			body.getUnits().add(onAttachStmt = Jimple.v().newNopStmt());

		// 2. onCreate:
		Stmt onCreateStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONCREATE, currentClass,
				classLocal);
		if (onCreateStmt == null)
			body.getUnits().add(onCreateStmt = Jimple.v().newNopStmt());

		// 3. onCreateView:
		Stmt onCreateViewStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONCREATEVIEW, currentClass,
				classLocal);
		if (onCreateViewStmt == null)
			body.getUnits().add(onCreateViewStmt = Jimple.v().newNopStmt());

		Stmt onViewCreatedStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONVIEWCREATED, currentClass,
				classLocal);
		if (onViewCreatedStmt == null)
			body.getUnits().add(onViewCreatedStmt = Jimple.v().newNopStmt());

		// 0. onActivityCreated:
		Stmt onActCreatedStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONACTIVITYCREATED,
				currentClass, classLocal);
		if (onActCreatedStmt == null)
			body.getUnits().add(onActCreatedStmt = Jimple.v().newNopStmt());

		// 4. onStart:
		Stmt onStartStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSTART, currentClass, classLocal);
		if (onStartStmt == null)
			body.getUnits().add(onStartStmt = Jimple.v().newNopStmt());

		// 5. onResume:
		Stmt onResumeStmt = Jimple.v().newNopStmt();
		body.getUnits().add(onResumeStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONRESUME, currentClass, classLocal);

		// 6. onPause:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONPAUSE, currentClass, classLocal);
		createIfStmt(onResumeStmt);

		// 7. onSaveInstanceState:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSAVEINSTANCESTATE, currentClass, classLocal);

		// 8. onStop:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSTOP, currentClass, classLocal);
		createIfStmt(onCreateViewStmt);
		createIfStmt(onStartStmt);

		// 9. onDestroyView:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDESTROYVIEW, currentClass, classLocal);
		createIfStmt(onCreateViewStmt);

		// 10. onDestroy:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDESTROY, currentClass, classLocal);

		// 11. onDetach:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDETACH, currentClass, classLocal);
		createIfStmt(onAttachStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
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

				// If we found no implementation or if the implementation we
				// found
				// is in a system class, we skip it. Note that null methods may
				// happen since all callback interfaces for application
				// callbacks
				// are registered under the name of the application class.
				if (method == null)
					continue;
				if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
					continue;

				// Get the local instance of the target class
				Local local = this.localVarsForClasses.get(methodAndClass.getClassName());
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

	/**
	 * Generates invocation statements for all callback methods which need to be
	 * invoked during the given class' run cycle.
	 * 
	 * @param currentClass
	 *            The class for which we currently build the lifecycle
	 * @return True if a matching callback has been found, otherwise false.
	 */
	private boolean addCallbackMethods(SootClass currentClass) {
		return addCallbackMethods(currentClass, null, "");
	}

	/**
	 * Generates invocation statements for all callback methods which need to be
	 * invoked during the given class' run cycle.
	 * 
	 * @param currentClass
	 *            The class for which we currently build the lifecycle
	 * @param referenceClasses
	 *            The classes for which no new instances shall be created, but
	 *            rather existing ones shall be used.
	 * @param callbackSignature
	 *            An empty string if calls to all callback methods for the given
	 *            class shall be generated, otherwise the subsignature of the only
	 *            callback method to generate.
	 * @return True if a matching callback has been found, otherwise false.
	 */
	private boolean addCallbackMethods(SootClass currentClass, Set<SootClass> referenceClasses,
			String callbackSignature) {
		// If no callbacks are declared for the current class, there is nothing
		// to be done here
		if (currentClass == null)
			return false;
		if (!this.callbackFunctions.containsKey(currentClass))
			return false;

		// Get all classes in which callback methods are declared
		MultiMap<SootClass, SootMethod> callbackClasses = getCallbackMethodsForClass(currentClass, callbackSignature);
		callbackClasses.putAll(getCallbackMethodsForClass(null, callbackSignature));

		// The class for which we are generating the lifecycle always has an
		// instance.
		if (referenceClasses == null || referenceClasses.isEmpty())
			referenceClasses = Collections.singleton(currentClass);
		else {
			referenceClasses = new HashSet<SootClass>(referenceClasses);
			referenceClasses.add(currentClass);
		}

		Stmt beforeCallbacks = Jimple.v().newNopStmt();
		body.getUnits().add(beforeCallbacks);

		boolean callbackFound = false;
		for (SootClass callbackClass : callbackClasses.keySet()) {
			// If we already have a parent class that defines this callback, we
			// use it. Otherwise, we create a new one.
			boolean hasParentClass = false;
			for (SootClass parentClass : referenceClasses) {
				Local parentLocal = this.localVarsForClasses.get(parentClass.getName());
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
				Local classLocal = localVarsForClasses.get(callbackClass.getName());

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
	 * @param className
	 *            The class for which to get the callback methods
	 * @param callbackSignature
	 *            An empty string if all callback methods for the given class shall
	 *            be return, otherwise the subsignature of the only callback method
	 *            to return.
	 * @return The callback methods registered for the given class
	 */
	private MultiMap<SootClass, SootMethod> getCallbackMethodsForClass(SootClass clazz, String callbackSignature) {
		MultiMap<SootClass, SootMethod> callbackClasses = new HashMultiMap<>();
		for (SootMethod theMethod : this.callbackFunctions.get(clazz)) {
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

	private Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal) {
		return searchAndBuildMethod(subsignature, currentClass, classLocal, Collections.<SootClass>emptySet());
	}

	private Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal,
			Set<SootClass> parentClasses) {
		if (currentClass == null || classLocal == null)
			return null;

		SootMethod method = findMethod(currentClass, subsignature);
		if (method == null) {
			logger.warn("Could not find Android entry point method: {}", subsignature);
			return null;
		}

		// If the method is in one of the predefined Android classes, it cannot
		// contain custom code, so we do not need to call it
		if (AndroidEntryPointConstants.isLifecycleClass(method.getDeclaringClass().getName()))
			return null;

		// If this method is part of the Android framework, we don't need to
		// call it
		if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return null;

		assert method.isStatic() || classLocal != null : "Class local was null for non-static method "
				+ method.getSignature();

		// write Method
		return buildMethodCall(method, body, classLocal, generator, parentClasses);
	}

	private void createIfStmt(Unit target) {
		if (target == null) {
			return;
		}
		final Jimple jimple = Jimple.v();
		EqExpr cond = jimple.newEqExpr(intCounter, IntConstant.v(conditionCounter++));
		IfStmt ifStmt = jimple.newIfStmt(cond, target);
		body.getUnits().add(ifStmt);
	}

	/**
	 * Sets whether additional methods which are present in a component, but are
	 * neither lifecycle methods nor callbacks, shall also be modeled in the dummy
	 * main method.
	 * 
	 * @param modelAdditionalMethods
	 *            True if additional methods shall be modeled, otherwise false
	 */
	public void setModelAdditionalMethods(boolean modelAdditionalMethods) {
		this.modelAdditionalMethods = modelAdditionalMethods;
	}

	/**
	 * Gets whether additional methods which are present in a component, but are
	 * neither lifecycle methods nor callbacks, shall also be modeled in the dummy
	 * main method.
	 * 
	 * @return True if additional methods shall be modeled, otherwise false
	 */
	public boolean getModelAdditionalMethods() {
		return this.modelAdditionalMethods;
	}

	@Override
	public Collection<String> getRequiredClasses() {
		Set<String> requiredClasses = new HashSet<String>(androidClasses.size());
		for (SootClass sc : androidClasses)
			requiredClasses.add(sc.getName());
		requiredClasses
				.addAll(SootMethodRepresentationParser.v().parseClassNames(additionalEntryPoints, false).keySet());
		return requiredClasses;
	}

	public void setFragments(MultiMap<SootClass, SootClass> fragments) {
		fragmentClasses = fragments;
	}

}
