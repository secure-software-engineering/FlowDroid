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
package soot.jimple.infoflow.android.entryPointCreators;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Hierarchy;
import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils.ComponentType;
import soot.jimple.infoflow.android.entryPointCreators.components.AbstractComponentEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.ActivityEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.BroadcastReceiverEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.android.entryPointCreators.components.ContentProviderEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.FragmentEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.ServiceConnectionEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.ServiceEntryPointCreator;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
import soot.jimple.infoflow.android.manifest.IManifestHandler;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SootUtils;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.options.Options;
import soot.tagkit.ExpectedTypeTag;
import soot.toDex.SootToDexUtils;
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

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final boolean DEBUG = false;

	protected MultiMap<SootClass, SootMethod> callbackFunctions = new HashMultiMap<>();

	private SootClass applicationClass = null;
	private Local applicationLocal = null;

	private MultiMap<SootClass, String> activityLifecycleCallbacks = new HashMultiMap<>();
	private MultiMap<SootClass, String> applicationCallbackClasses = new HashMultiMap<>();
	private Map<SootClass, SootField> callbackClassToField = new HashMap<>();

	private MultiMap<SootClass, SootClass> fragmentClasses = null;
	private final ComponentEntryPointCollection componentToInfo = new ComponentEntryPointCollection();

	private Collection<SootClass> components;

	private MultiMap<SootMethod, Stmt> javascriptInterfaceStmts;

	private SootClass applicationComponentFactoryClass;

	private SootField classLoaderField;

	private SootField instantiatorField;

	// Contains *all* potential component classes, irregular of whether
	// they are defined in the manifest or not. Note that the app component
	// factory might create other classes than those listed in the manifest, which
	// makes
	// everything quite complex.
	// In other words, this set contains *all* possible components
	private Set<SootClass> allComponentClasses = new HashSet<>();

	private static final String DEFAULT_COMPONENTDATAEXCHANGENAME = "ComponentDataExchangeInterface";

	private SootClass componentDataExchangeInterface;

	private AbstractCollection<SootMethod> additionalMethods;

	/**
	 * Creates a new instance of the {@link AndroidEntryPointCreator} class and
	 * registers a list of classes to be automatically scanned for Android lifecycle
	 * methods
	 * 
	 * @param components The list of classes to be automatically scanned for Android
	 *                   lifecycle methods
	 */
	public AndroidEntryPointCreator(IManifestHandler manifest, Collection<SootClass> components) {
		super(manifest);
		this.components = components;
		this.overwriteDummyMainMethod = true;
		Hierarchy h = Scene.v().getActiveHierarchy();
		for (String clzName : new String[] { AndroidEntryPointConstants.ACTIVITYCLASS,
				AndroidEntryPointConstants.BROADCASTRECEIVERCLASS, AndroidEntryPointConstants.CONTENTPROVIDERCLASS,
				AndroidEntryPointConstants.SERVICECLASS }) {
			SootClass sc = Scene.v().getSootClassUnsafe(clzName, false);
			if (sc != null && !sc.isPhantom()) {
				allComponentClasses.addAll(h.getSubclassesOf(sc));
			}
		}

		ComponentExchangeInfo info = generateComponentDataExchangeInterface();
		initializeComponentDataTransferMethods(info);

	}

	private ComponentExchangeInfo generateComponentDataExchangeInterface() {
		SootClass s = getOrCreateClass(DEFAULT_COMPONENTDATAEXCHANGENAME);
		s.setModifiers(Modifier.PUBLIC | Modifier.INTERFACE);
		componentDataExchangeInterface = s;

		RefType intent = RefType.v("android.content.Intent");
		Scene sc = Scene.v();
		String getResultIntentName = findUniqueMethodName("getResultIntent", allComponentClasses);
		String setResultIntentName = findUniqueMethodName("setResultIntent", allComponentClasses);
		// just choose a different name other than "getIntent"
		String getIntentName = findUniqueMethodName("getDataIntent", allComponentClasses);
		String setIntentName = findUniqueMethodName("setDataIntent", allComponentClasses);

		SootMethod getResultIntentMethod = sc.makeSootMethod(getResultIntentName, Collections.emptyList(), intent,
				Modifier.PUBLIC | Modifier.ABSTRACT);
		componentDataExchangeInterface.addMethod(getResultIntentMethod);
		SootMethod getIntentMethod = sc.makeSootMethod(getIntentName, Collections.emptyList(), intent,
				Modifier.PUBLIC | Modifier.ABSTRACT);
		componentDataExchangeInterface.addMethod(getIntentMethod);
		SootMethod setIntentMethod = sc.makeSootMethod(setIntentName, Arrays.asList(intent), VoidType.v(),
				Modifier.PUBLIC | Modifier.ABSTRACT);
		componentDataExchangeInterface.addMethod(setIntentMethod);
		SootMethod setResultIntentMethod = sc.makeSootMethod(setResultIntentName, Arrays.asList(intent), VoidType.v(),
				Modifier.PUBLIC | Modifier.ABSTRACT);
		componentDataExchangeInterface.addMethod(setResultIntentMethod);

		ComponentExchangeInfo info = new ComponentExchangeInfo(componentDataExchangeInterface, getIntentMethod,
				setIntentMethod, getResultIntentMethod, setResultIntentMethod);
		componentToInfo.setComponentExchangeInfo(info);
		return info;

	}

	private String findUniqueMethodName(String name, Collection<SootClass> classes) {
		String tryName = name;
		int i = 1;
		nextTry: while (true) {
			for (SootClass clz : classes) {
				if (clz.getMethodByNameUnsafe(tryName) != null) {
					i++;
					tryName = name + i;
					continue nextTry;
				}
			}
			return tryName;
		}
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		// Make sure that we don't have any leftover state
		// from previous runs
		reset();
		additionalMethods = new HashSet<>();

		logger.info(String.format("Creating Android entry point for %d components...", components.size()));

		// If the application tag in the manifest specifies a appComponentFactory, it
		// needs to be called first
		initializeApplComponentFactory();

		// If we have an implementation of android.app.Application, this needs
		// special treatment
		initializeApplicationClass();
		Jimple j = Jimple.v();

		// due to app component factories, that could be another application class!
		// The application class gets instantiated first. Note that although it's
		// created now, it's onCreate method gets called *after*
		// all content providers.
		SootClass applicationClassUse = Scene.v().getSootClass(AndroidEntryPointConstants.APPLICATIONCLASS);
		boolean generateApplicationCode = applicationClass != null || applicationComponentFactoryClass != null;
		if (generateApplicationCode) {
			if (applicationComponentFactoryClass != null) {
				Local factory = generateClassConstructor(applicationComponentFactoryClass);
				SootMethodRef mrInstantiate = Scene.v().makeMethodRef(
						Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.APPCOMPONENTFACTORYCLASS),
						AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEAPPLICATION, false);
				SootMethodRef mrInstantiateClassLoader = Scene.v().makeMethodRef(
						Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.APPCOMPONENTFACTORYCLASS),
						AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATECLASSLOADER, false);
				SootMethodRef mrGetClassLoader = Scene.v().makeMethodRef(Scene.v().getSootClass("java.lang.Class"),
						"java.lang.ClassLoader getClassLoader()", false);
				applicationLocal = j.newLocal("application", RefType.v(AndroidEntryPointConstants.APPLICATIONCLASS));
				body.getLocals().add(applicationLocal);
				String classAppl = "android.app.Application";
				if (applicationClass != null)
					classAppl = applicationClass.getName();
				applicationClass = Scene.v().forceResolve(classAppl, SootClass.SIGNATURES);
				Local clazzL = j.newLocal("clazz", RefType.v("java.lang.Class"));
				body.getLocals().add(clazzL);
				Local classLoader = j.newLocal("classLoader", RefType.v("java.lang.ClassLoader"));
				body.getLocals().add(classLoader);

				body.getUnits()
						.add(j.newAssignStmt(clazzL, ClassConstant.v(SootToDexUtils.getDexClassName(dummyClassName))));
				body.getUnits().add(j.newAssignStmt(classLoader, j.newVirtualInvokeExpr(clazzL, mrGetClassLoader)));

				AssignStmt instantiateCL = j.newAssignStmt(classLoader, j.newVirtualInvokeExpr(factory,
						mrInstantiateClassLoader, Arrays.asList(classLoader, createApplicationInfo())));
				body.getUnits().add(instantiateCL);

				if (classLoaderField == null)
					classLoaderField = createField(RefType.v("java.lang.ClassLoader"), "cl");
				if (instantiatorField == null)
					instantiatorField = createField(RefType.v("android.app.AppComponentFactory"), "cl");

				AssignStmt instantiate = j.newAssignStmt(applicationLocal, j.newVirtualInvokeExpr(factory,
						mrInstantiate, Arrays.asList(classLoader, StringConstant.v(classAppl))));
				instantiate.addTag(new ExpectedTypeTag(applicationClass.getType()));
				body.getUnits().add(instantiate);
				body.getUnits().add(j.newAssignStmt(j.newStaticFieldRef(classLoaderField.makeRef()), classLoader));
				body.getUnits().add(j.newAssignStmt(j.newStaticFieldRef(instantiatorField.makeRef()), factory));
			} else {
				// Create the application
				applicationLocal = generateClassConstructor(applicationClass);
				// we know for sure that there is no other application class in question
				applicationClassUse = applicationClass;
			}
			localVarsForClasses.put(applicationClass, applicationLocal);
			SootClass cw = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.CONTEXT_WRAPPER);
			if (cw == null) {
				//use application local type as a fallback, since this class also implements context wrapper
				cw = ((RefType) applicationLocal.getType()).getSootClass();
			}
			if (cw != null)
				body.getUnits().add(j.newInvokeStmt(j.newVirtualInvokeExpr(applicationLocal,
						Scene.v().makeMethodRef(cw, AndroidEntryPointConstants.ATTACH_BASE_CONTEXT, false))));
		}

		Map<SootClass, ContentProviderEntryPointCreator> cpComponents = new HashMap<>();
		// For some weird reason unknown to anyone except the flying spaghetti
		// monster, the onCreate() methods of content providers run even before
		// the application object's onCreate() is called (but after the creation of the
		// application).
		// See https://issuetracker.google.com/issues/36917845#comment4
		{
			boolean hasContentProviders = false;
			NopStmt beforeContentProvidersStmt = Jimple.v().newNopStmt();
			body.getUnits().add(beforeContentProvidersStmt);
			for (SootClass currentClass : components) {
				if (entryPointUtils.getComponentType(currentClass) == ComponentType.ContentProvider) {
					// Create an instance of the content provider
					ContentProviderEntryPointCreator cpc = new ContentProviderEntryPointCreator(currentClass,
							applicationClassUse, this.manifest, instantiatorField, classLoaderField,
							componentToInfo.getComponentExchangeInfo());
					SootMethod m = cpc.createInit();
					Local cpLocal = generator.generateLocal(RefType.v(AndroidEntryPointConstants.CONTENTPROVIDERCLASS));
					AssignStmt cp = Jimple.v().newAssignStmt(cpLocal, Jimple.v().newStaticInvokeExpr(m.makeRef()));
					cp.addTag(new ExpectedTypeTag(currentClass.getType()));
					body.getUnits().add(cp);
					localVarsForClasses.put(currentClass, cpLocal);
					cpComponents.put(currentClass, cpc);

					hasContentProviders = true;
				}
			}
			// Jump back to the beginning of this section to overapproximate the
			// order in which the methods are called
			if (hasContentProviders)
				createIfStmt(beforeContentProvidersStmt);
		}

		// If we have an application, we need to start it in the very beginning
		if (generateApplicationCode) {
			if (applicationLocal != null) {
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
				searchAndBuildMethod(AndroidEntryPointConstants.APPLICATION_ONCREATE, applicationLocal);

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

		// We need to create methods for all fragments, because they can be used by
		// multiple activities
		Map<SootClass, SootMethod> fragmentToMainMethod = new HashMap<>();
		for (SootClass parentActivity : fragmentClasses.keySet()) {
			Set<SootClass> fragments = fragmentClasses.get(parentActivity);
			for (SootClass fragment : fragments) {
				FragmentEntryPointCreator entryPointCreator = new FragmentEntryPointCreator(fragment, applicationClass,
						this.manifest, instantiatorField, classLoaderField, componentToInfo.getComponentExchangeInfo());
				entryPointCreator.setDummyClassName(mainMethod.getDeclaringClass().getName());
				entryPointCreator.setCallbacks(callbackFunctions.get(fragment));

				SootMethod fragmentMethod = entryPointCreator.createDummyMain();
				fragmentToMainMethod.put(fragment, fragmentMethod);
				componentToInfo.put(fragment, fragmentMethod);
			}
		}

		for (SootClass currentClass : components) {
			currentClass.setApplicationClass();

			// Get the callbacks and component type of the current component
			ComponentType componentType = entryPointUtils.getComponentType(currentClass);

			// Before-class marker
			Stmt beforeComponentStmt = Jimple.v().newNopStmt();
			Stmt afterComponentStmt = Jimple.v().newNopStmt();
			body.getUnits().add(beforeComponentStmt);

			// Generate the lifecycles for the different kinds of Android
			// classes
			AbstractComponentEntryPointCreator componentCreator = null;
			List<Value> params = Collections.singletonList(NullConstant.v());
			switch (componentType) {
			case Activity:
				Map<SootClass, SootMethod> curActivityToFragmentMethod = new HashMap<>();
				if (fragmentClasses != null) {
					Set<SootClass> fragments = fragmentClasses.get(currentClass);
					if (fragments != null && !fragments.isEmpty()) {
						for (SootClass fragment : fragments)
							curActivityToFragmentMethod.put(fragment, fragmentToMainMethod.get(fragment));
					}
				}
				componentCreator = new ActivityEntryPointCreator(currentClass, applicationClassUse,
						activityLifecycleCallbacks, callbackClassToField, curActivityToFragmentMethod, this.manifest,
						instantiatorField, classLoaderField, componentToInfo.getComponentExchangeInfo());
				break;
			case Service:
			case GCMBaseIntentService:
			case GCMListenerService:
			case HostApduService:
				componentCreator = new ServiceEntryPointCreator(currentClass, applicationClassUse, this.manifest,
						instantiatorField, classLoaderField, componentToInfo.getComponentExchangeInfo());
				break;
			case ServiceConnection:
				componentCreator = new ServiceConnectionEntryPointCreator(currentClass, applicationClassUse,
						this.manifest, instantiatorField, classLoaderField, componentToInfo.getComponentExchangeInfo());
				break;
			case BroadcastReceiver:
				componentCreator = new BroadcastReceiverEntryPointCreator(currentClass, applicationClassUse,
						this.manifest, instantiatorField, classLoaderField, componentToInfo.getComponentExchangeInfo());
				break;
			case ContentProvider:
				componentCreator = cpComponents.get(currentClass);
				//We need to pass on the content provider instance
				params = Arrays.asList(NullConstant.v(), localVarsForClasses.get(currentClass));
				break;
			default:
				componentCreator = null;
				break;
			}

			// We may skip the complete component
			createIfStmt(afterComponentStmt);

			// Create a call to the component's lifecycle method
			if (componentCreator != null) {
				componentCreator.setDummyClassName(mainMethod.getDeclaringClass().getName());
				componentCreator.setCallbacks(callbackFunctions.get(currentClass));
				SootMethod lifecycleMethod = componentCreator.createDummyMain();
				componentToInfo.put(currentClass, componentCreator.getComponentInfo());

				additionalMethods.addAll(componentCreator.getAdditionalMethods());
				// dummyMain(component, intent)
				if (shouldAddLifecycleCall(currentClass)) {
					body.getUnits().add(Jimple.v()
							.newInvokeStmt(Jimple.v().newStaticInvokeExpr(lifecycleMethod.makeRef(), params)));
				}
			}

			// Jump back to the front of the component
			createIfStmt(beforeComponentStmt);
			body.getUnits().add(afterComponentStmt);
		}

		// Add conditional calls to the application callback methods
		if (applicationLocal != null) {
			Unit beforeAppCallbacks = Jimple.v().newNopStmt();
			body.getUnits().add(beforeAppCallbacks);
			addApplicationCallbackMethods();
			createIfStmt(beforeAppCallbacks);
		}
		createJavascriptCallbacks();

		createIfStmt(outerStartStmt);

		// Add a call to application.onTerminate()
		if (applicationLocal != null)
			searchAndBuildMethod(AndroidEntryPointConstants.APPLICATION_ONTERMINATE, applicationLocal);

		body.getUnits().add(Jimple.v().newReturnVoidStmt());

		// Optimize and check the generated main method
		NopEliminator.v().transform(body);
		eliminateSelfLoops();
		eliminateFallthroughIfs(body);

		if (DEBUG || Options.v().validate())
			mainMethod.getActiveBody().validate();

		return mainMethod;
	}

	/**
	 * Initializes the methods intended for transferring data (usually intents) between components.
	 * @param info contains information about the commonly used method names for the interface methods
	 */
	private void initializeComponentDataTransferMethods(ComponentExchangeInfo info) {

		for (SootClass s : allComponentClasses) {

			s.addInterface(componentDataExchangeInterface);
			Scene sc = Scene.v();
			Jimple j = Jimple.v();

			// Create a name for a field for the result intent of this component
			String fieldName = "ipcResultIntent";
			int fieldIdx = 0;
			while (s.declaresFieldByName(fieldName))
				fieldName = "ipcResultIntent_" + fieldIdx++;

			// Create the field itself
			SootField resultIntentField = Scene.v().makeSootField(fieldName, RefType.v("android.content.Intent"),
					Modifier.PUBLIC);
			resultIntentField.addTag(SimulatedCodeElementTag.TAG);
			s.addField(resultIntentField);
			SootMethod getResultIntentMethod = sc.makeSootMethod(info.getResultIntentMethod.getName(),
					info.getResultIntentMethod.getParameterTypes(), info.getResultIntentMethod.getReturnType(),
					Modifier.PUBLIC);
			getResultIntentMethod.addTag(SimulatedCodeElementTag.TAG);
			JimpleBody jb = j.newBody(getResultIntentMethod);
			getResultIntentMethod.setActiveBody(jb);
			s.addMethod(getResultIntentMethod);

			jb.insertIdentityStmts();
			Local lcl = j.newLocal("ret", getResultIntentMethod.getReturnType());
			jb.getLocals().add(lcl);
			jb.getUnits()
					.add(j.newAssignStmt(lcl, j.newInstanceFieldRef(jb.getThisLocal(), resultIntentField.makeRef())));
			jb.getUnits().add(j.newReturnStmt(lcl));

			// Create a name for a field for the intent with which the component is started
			fieldName = "ipcIntent";
			fieldIdx = 0;
			while (s.declaresFieldByName(fieldName))
				fieldName = "ipcIntent_" + fieldIdx++;

			// Create the field itself
			SootField intentField = Scene.v().makeSootField(fieldName, RefType.v("android.content.Intent"),
					Modifier.PUBLIC);
			intentField.addTag(SimulatedCodeElementTag.TAG);
			s.addField(intentField);

			SootMethod setResultIntentMethod = sc.makeSootMethod(info.setResultIntentMethod.getName(),
					info.setResultIntentMethod.getParameterTypes(), info.setResultIntentMethod.getReturnType(),
					Modifier.PUBLIC);

			jb = j.newBody(setResultIntentMethod);
			setResultIntentMethod.setActiveBody(jb);
			s.addMethod(setResultIntentMethod);
			setResultIntentMethod.addTag(SimulatedCodeElementTag.TAG);
			jb.insertIdentityStmts();
			jb.getUnits().add(j.newAssignStmt(j.newInstanceFieldRef(jb.getThisLocal(), resultIntentField.makeRef()),
					jb.getParameterLocal(0)));
			jb.getUnits().add(j.newReturnVoidStmt());
			SootMethod getIntentMethod = sc.makeSootMethod(info.getIntentMethod.getName(),
					info.getIntentMethod.getParameterTypes(), info.getIntentMethod.getReturnType(), Modifier.PUBLIC);
			jb = j.newBody(getIntentMethod);
			getIntentMethod.addTag(SimulatedCodeElementTag.TAG);
			getIntentMethod.setActiveBody(jb);
			s.addMethod(getIntentMethod);
			jb.insertIdentityStmts();
			lcl = j.newLocal("retValue", getIntentMethod.getReturnType());
			jb.getLocals().add(lcl);
			jb.getUnits().add(j.newAssignStmt(lcl, j.newInstanceFieldRef(jb.getThisLocal(), intentField.makeRef())));
			jb.getUnits().add(j.newReturnStmt(lcl));

			SootMethod setIntentMethod = sc.makeSootMethod(info.setIntentMethod.getName(),
					info.setIntentMethod.getParameterTypes(), info.setIntentMethod.getReturnType(), Modifier.PUBLIC);
			jb = j.newBody(setIntentMethod);
			setIntentMethod.setActiveBody(jb);
			s.addMethod(setIntentMethod);
			setIntentMethod.addTag(SimulatedCodeElementTag.TAG);
			jb.insertIdentityStmts();
			jb.getUnits().add(j.newAssignStmt(j.newInstanceFieldRef(jb.getThisLocal(), intentField.makeRef()),
					jb.getParameterLocal(0)));
			jb.getUnits().add(j.newReturnVoidStmt());

		}
	}

	private Value createApplicationInfo() {
		SootClass p = Scene.v().getSootClassUnsafe("android.content.pm.ApplicationInfo");
		if (p != null) {
			return generateClassConstructor(p);
		} else {
			return NullConstant.v();
		}
	}

	private void initializeApplComponentFactory() {

		IAndroidApplication app = manifest.getApplication();
		if (app != null) {
			String componentFactoryName = app.getAppComponentFactory();
			// We can only look for callbacks if we have an application class
			if (componentFactoryName == null || componentFactoryName.isEmpty())
				return;

			// Find the application class
			for (SootClass currentClass : components) {
				// Is this the application class?
				if (entryPointUtils.isComponentFactoryClass(currentClass)
						&& currentClass.getName().equals(componentFactoryName)) {
					applicationComponentFactoryClass = currentClass;
					break;
				}
			}
		}

		// We can only look for callbacks if we have an application class
		if (applicationClass == null)
			return;

		// Look into the application class' callbacks
		Collection<SootMethod> callbacks = callbackFunctions.get(applicationClass);
		if (callbacks != null) {
			for (SootMethod smCallback : callbacks) {
				if (smCallback != null) {
					applicationCallbackClasses.put(smCallback.getDeclaringClass(), smCallback.getSignature());
				}
			}
		}

		// Create fields for the activity lifecycle classes
		for (SootClass callbackClass : activityLifecycleCallbacks.keySet()) {
			String baseName = callbackClass.getName();
			if (baseName.contains("."))
				baseName = baseName.substring(baseName.lastIndexOf(".") + 1);

			// Generate a fresh field name
			SootField fld = createField(RefType.v(callbackClass), baseName);
			callbackClassToField.put(callbackClass, fld);
		}
	}

	private void createJavascriptCallbacks() {
		Jimple j = Jimple.v();
		for (SootMethod m : javascriptInterfaceStmts.keySet()) {
			Set<Stmt> statements = javascriptInterfaceStmts.get(m);
			for (Stmt s : statements) {
				UnitPatchingChain units = m.retrieveActiveBody().getUnits();
				SootField f = null;
				Value arg = s.getInvokeExpr().getArg(0);
				DummyMainFieldElementTag dm = (DummyMainFieldElementTag) s.getTag(DummyMainFieldElementTag.TAG_NAME);
				if (dm != null) {
					f = mainMethod.getDeclaringClass().getFieldByNameUnsafe(dm.getFieldName());
				}
				if (f == null) {
					// create field
					f = createField(arg.getType(), "jsInterface");
					AssignStmt assign = j.newAssignStmt(j.newStaticFieldRef(f.makeRef()), arg);
					assign.addTag(SimulatedCodeElementTag.TAG);
					s.addTag(new DummyMainFieldElementTag(f.getName()));
					units.insertAfter(assign, s);
				}

				Local l = j.newLocal(f.getName(), f.getType());
				body.getLocals().add(l);
				Stmt assignF = j.newAssignStmt(l, j.newStaticFieldRef(f.makeRef()));
				body.getUnits().add(assignF);
				SootClass cbtype = ((RefType) f.getType()).getSootClass();

				for (SootClass c : TypeUtils.getAllDerivedClasses(cbtype)) {
					for (SootMethod cbm : c.getMethods()) {
						if (AndroidEntryPointUtils.isCallableFromJS(cbm)) {
							List<Value> args = new ArrayList<>();
							for (Type t : cbm.getParameterTypes())
								args.add(getSimpleDefaultValue(t));
							InvokeStmt st = j.newInvokeStmt(j.newVirtualInvokeExpr(l, cbm.makeRef(), args));
							body.getUnits().add(st);
						}
					}

				}
				createIfStmt(assignF);

			}
		}
	}

	/**
	 * Checks whether a lifecycle call should be added to the given SootClass,
	 * designed to be overridden by different implementations
	 *
	 * @param clazz the SootClass in question
	 * @return True if a lifecycle call should be added in
	 *         {@link #createDummyMainInternal()}
	 */
	protected boolean shouldAddLifecycleCall(SootClass clazz) {
		return true;
	}

	/**
	 * Find the application class and its callbacks
	 */
	private void initializeApplicationClass() {

		IAndroidApplication app = manifest.getApplication();
		if (app != null) {
			String applicationName = app.getName();
			// We can only look for callbacks if we have an application class
			if (applicationName == null || applicationName.isEmpty())
				return;

			// Find the application class
			for (SootClass currentClass : components) {
				// Is this the application class?
				if (entryPointUtils.isApplicationClass(currentClass)
						&& currentClass.getName().equals(applicationName)) {
					if (applicationClass != null && currentClass != applicationClass)
						throw new RuntimeException("Multiple application classes in app");
					applicationClass = currentClass;
					break;
				}
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
				if (smCallback != null) {
					// Is this a special callback class? We have callbacks that model activity
					// lifecycle events and ones that model generic events (e.g., low memory)
					if (scActCallbacks != null && Scene.v().getOrMakeFastHierarchy()
							.canStoreType(smCallback.getDeclaringClass().getType(), scActCallbacks.getType()))
						activityLifecycleCallbacks.put(smCallback.getDeclaringClass(), smCallback.getSignature());
					else
						applicationCallbackClasses.put(smCallback.getDeclaringClass(), smCallback.getSignature());
				}
			}
		}

		// Create fields for the activity lifecycle classes
		for (SootClass callbackClass : activityLifecycleCallbacks.keySet()) {
			String baseName = callbackClass.getName();
			if (baseName.contains("."))
				baseName = baseName.substring(baseName.lastIndexOf(".") + 1);

			// Generate a fresh field name
			SootField fld = createField(RefType.v(callbackClass), baseName);
			callbackClassToField.put(callbackClass, fld);
		}
	}

	protected SootField createField(Type type, String baseName) {
		SootClass dummyMainClass = mainMethod.getDeclaringClass();
		int idx = 0;
		String fieldName = baseName;
		while (dummyMainClass.declaresFieldByName(fieldName)) {
			fieldName = baseName + "_" + idx;
			idx++;
		}
		SootField fld = Scene.v().makeSootField(fieldName, type, Modifier.PRIVATE | Modifier.STATIC);
		mainMethod.getDeclaringClass().addField(fld);
		return fld;
	}

	/**
	 * Removes if statements that jump to the fall-through successor
	 * 
	 * @param body The body from which to remove unnecessary if statements
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
	 * @param applicationClass The class in which the user-defined application is
	 *                         implemented
	 * @param applicationLocal The local containing the instance of the user-defined
	 *                         application
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

		List<String> lifecycleMethods = AndroidEntryPointConstants.getApplicationLifecycleMethods();
		for (SootClass sc : applicationCallbackClasses.keySet())
			for (String methodSig : applicationCallbackClasses.get(sc)) {
				SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v().parseSootMethodString(methodSig);
				String subSig = methodAndClass.getSubSignature();
				SootMethod method = SootUtils.findMethod(Scene.v().getSootClass(sc.getName()), subSig);

				// We do not consider lifecycle methods which are directly
				// inserted at their respective positions
				if (sc == applicationClass && lifecycleMethods.contains(subSig))
					continue;

				// If this is an activity lifecycle method, we skip it as well
				// TODO: can be removed once we filter it in general
				if (activityLifecycleCallbacks.containsKey(sc))
					if (lifecycleMethods.contains(subSig))
						continue;

				// If we found no implementation or if the implementation we found is in a
				// system class, we skip it. Note that null methods may happen since all
				// callback interfaces for application callbacks are registered under the name
				// of the application class.
				if (method == null)
					continue;
				if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
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
				buildMethodCall(method, local);
				body.getUnits().add(thenStmt);
			}
	}

	@Override
	public Collection<String> getRequiredClasses() {
		Set<String> requiredClasses = new HashSet<String>(components.size());
		for (SootClass sc : components)
			requiredClasses.add(sc.getName());
		return requiredClasses;
	}

	public void setFragments(MultiMap<SootClass, SootClass> fragments) {
		fragmentClasses = fragments;
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		List<SootMethod> r = new ArrayList<>(componentToInfo.getLifecycleMethods());
		if (additionalMethods != null)
			r.addAll(additionalMethods);
		return r;
	}

	@Override
	public Collection<SootField> getAdditionalFields() {
		return componentToInfo.getAdditionalFields();
	}

	public ComponentEntryPointCollection getComponentToEntryPointInfo() {
		return componentToInfo;
	}

	/**
	 * Sets the list of callback functions to be integrated into the Android
	 * lifecycle
	 * 
	 * @param callbackFunctions The list of callback functions to be integrated into
	 *                          the Android lifecycle. This is a mapping from the
	 *                          Android element class (activity, service, etc.) to
	 *                          the list of callback methods for that element.
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
	public void reset() {
		super.reset();

		// Get rid of the generated component methods
		for (SootMethod sm : getAdditionalMethods()) {
			if (sm.isDeclared())
				sm.getDeclaringClass().removeMethod(sm);
		}
		for (SootField sf : getAdditionalFields()) {
			if (sf.isDeclared())
				sf.getDeclaringClass().removeField(sf);
		}

		// Get rid of the generated fields
		for (SootField fld : callbackClassToField.values()) {
			if (fld.isDeclared())
				fld.getDeclaringClass().removeField(fld);
		}

		componentToInfo.clear();
		callbackClassToField.clear();
	}

	/**
	 * Sets the Android components for which a dummy main method shall be created
	 * 
	 * @param components The Android components for which a dummy main method shall
	 *                   be created
	 */
	public void setComponents(Collection<SootClass> components) {
		this.components = components;
	}

	/**
	 * Removes all methods that have been generated by this entry point creator
	 * 
	 * @param removeClass True if the generated class shall also be removed. False
	 *                    to only remove the methods, but keep the class
	 */
	public void removeGeneratedMethods(boolean removeClass) {
		// Remove the dummy main method itself
		final SootClass mainClass = mainMethod.getDeclaringClass();
		if (removeClass)
			Scene.v().removeClass(mainClass);
		else
			mainClass.removeMethod(mainMethod);

		// Remove the additional methods
		for (SootMethod sm : getAdditionalMethods()) {
			if (sm.isDeclared()) {
				final SootClass declaringClass = sm.getDeclaringClass();
				if (declaringClass.isInScene())
					declaringClass.removeMethod(sm);
			}
		}
	}

	public void setJavaScriptInterfaces(MultiMap<SootMethod, Stmt> javascriptInterfaceStmts) {
		this.javascriptInterfaceStmts = javascriptInterfaceStmts;
	}

}
