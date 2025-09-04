package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.ComponentExchangeInfo;
import soot.jimple.infoflow.android.manifest.IManifestHandler;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.util.MultiMap;

/**
 * Entry point creator for Android activities
 * 
 * @author Steven Arzt
 *
 */
public class ActivityEntryPointCreator extends AbstractComponentEntryPointCreator {

	private final MultiMap<SootClass, String> activityLifecycleCallbacks;
	private final Map<SootClass, SootField> callbackClassToField;
	private final Map<SootClass, SootMethod> fragmentToMainMethod;

	public ActivityEntryPointCreator(SootClass component, SootClass applicationClass,
			MultiMap<SootClass, String> activityLifecycleCallbacks, Map<SootClass, SootField> callbackClassToField,
			Map<SootClass, SootMethod> fragmentToMainMethod, IManifestHandler manifest, SootField instantiatorField,
			SootField classLoaderField, ComponentExchangeInfo componentExchangeInfo) {
		super(component, applicationClass, manifest, instantiatorField, classLoaderField, componentExchangeInfo);
		this.activityLifecycleCallbacks = activityLifecycleCallbacks;
		this.callbackClassToField = callbackClassToField;
		this.fragmentToMainMethod = fragmentToMainMethod;
	}

	@Override
	protected Local generateClassConstructor(SootClass createdClass) {
		if (createdClass == component && instantiatorField != null) {
			return super.generateInstantiator(createdClass,
					AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEACTIVITY, body.getParameterLocal(0));
		}
		return super.generateClassConstructor(createdClass);
	}

	@Override
	protected void generateComponentLifecycle() {
		SootClass activityClass = getModelledClass();
		Set<SootClass> currentClassSet = Collections.singleton(component);
		final Body body = mainMethod.getActiveBody();

		Set<SootClass> referenceClasses = new HashSet<>();
		if (applicationClass != null)
			referenceClasses.add(applicationClass);
		if (this.activityLifecycleCallbacks != null)
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet())
				referenceClasses.add(callbackClass);
		referenceClasses.add(component);

		// Get the application class
		Local applicationLocal = null;
		if (applicationClass != null) {
			applicationLocal = generator.generateLocal(RefType.v("android.app.Application"));
			SootClass scApplicationHolder = LibraryClassPatcher.createOrGetApplicationHolder();
			body.getUnits().add(Jimple.v().newAssignStmt(applicationLocal,
					Jimple.v().newStaticFieldRef(scApplicationHolder.getFieldByName("application").makeRef())));
			localVarsForClasses.put(applicationClass, applicationLocal);
		}

		// Get the callback classes
		for (SootClass sc : callbackClassToField.keySet()) {
			Local callbackLocal = generator.generateLocal(RefType.v(sc));
			body.getUnits().add(Jimple.v().newAssignStmt(callbackLocal,
					Jimple.v().newStaticFieldRef(callbackClassToField.get(sc).makeRef())));
			localVarsForClasses.put(sc, callbackLocal);
		}

		// 1. attachBaseContext
		searchAndBuildMethod(AndroidEntryPointConstants.ATTACH_BASE_CONTEXT, thisLocal);

		// 2. onCreate:
		{
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATE, thisLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED,
						localVarsForClasses.get(callbackClass), currentClassSet);
			}
		}

		// Adding the lifecycle of the Fragments that belong to this Activity:
		// iterate through the fragments detected in the CallbackAnalyzer
		if (fragmentToMainMethod != null && !fragmentToMainMethod.isEmpty()) {
			for (SootClass scFragment : fragmentToMainMethod.keySet()) {
				// Call the fragment's main method
				SootMethod smFragment = fragmentToMainMethod.get(scFragment);
				List<Value> args = new ArrayList<>();
				args.add(intentLocal);
				args.add(thisLocal);
				body.getUnits()
						.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(smFragment.makeRef(), args)));
			}
		}

		// 3. onStart:
		Stmt onStartStmt;
		{
			onStartStmt = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSTART, thisLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				Stmt s = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
						localVarsForClasses.get(callbackClass), currentClassSet);
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
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESTOREINSTANCESTATE, thisLocal,
					currentClassSet);
			body.getUnits().add(afterOnRestore);
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPOSTCREATE, thisLocal);

		// 4. onResume:
		Stmt onResumeStmt = Jimple.v().newNopStmt();
		body.getUnits().add(onResumeStmt);
		{
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESUME, thisLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED,
						localVarsForClasses.get(callbackClass), currentClassSet);
			}
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPOSTRESUME, thisLocal);

		// Scan for other entryPoints of this class:
		if (this.callbacks != null && !this.callbacks.isEmpty()) {
			NopStmt startWhileStmt = Jimple.v().newNopStmt();
			NopStmt endWhileStmt = Jimple.v().newNopStmt();
			body.getUnits().add(startWhileStmt);
			createIfStmt(endWhileStmt);

			// Add the callbacks
			addCallbackMethods();

			body.getUnits().add(endWhileStmt);
			createIfStmt(startWhileStmt);
		}

		// 5. onPause:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPAUSE, thisLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED,
					localVarsForClasses.get(callbackClass), currentClassSet);
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATEDESCRIPTION, thisLocal);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSAVEINSTANCESTATE, thisLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
					localVarsForClasses.get(callbackClass), currentClassSet);
		}

		// goTo Stop, Resume or Create:
		// (to stop is fall-through, no need to add)
		createIfStmt(onResumeStmt);
		// createIfStmt(onCreateStmt); // no, the process gets killed in between

		// 6. onStop:
		Stmt onStop = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSTOP, thisLocal);
		boolean hasAppOnStop = false;
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			Stmt onActStoppedStmt = searchAndBuildMethod(
					AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED,
					localVarsForClasses.get(callbackClass), currentClassSet);
			hasAppOnStop |= onActStoppedStmt != null;
		}
		if (hasAppOnStop && onStop != null)
			createIfStmt(onStop);

		// goTo onDestroy, onRestart or onCreate:
		// (to restart is fall-through, no need to add)
		NopStmt stopToDestroyStmt = Jimple.v().newNopStmt();
		createIfStmt(stopToDestroyStmt);
		// createIfStmt(onCreateStmt); // no, the process gets killed in between

		// 7. onRestart:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESTART, thisLocal);
		body.getUnits().add(Jimple.v().newGotoStmt(onStartStmt)); // jump to onStart()

		// 8. onDestroy
		body.getUnits().add(stopToDestroyStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONDESTROY, thisLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED,
					localVarsForClasses.get(callbackClass), currentClassSet);
		}
	}

	@Override
	protected void createAdditionalMethods() {
		super.createAdditionalMethods();
		if (InfoflowAndroidConfiguration.getCreateActivityEntryMethods()) {

			createGetIntentMethod();
			createSetIntentMethod();
			createSetResultMethod();
		}
	}

	/**
	 * Creates an implementation of setIntent() that writes the given intent into
	 * the correct field
	 */
	private void createSetIntentMethod() {
		// We need to create an implementation of "setIntent". If there is already such
		// an implementation, we don't touch it.
		if (component.declaresMethod("void setIntent(android.content.Intent)"))
			return;

		Type intentType = RefType.v("android.content.Intent");
		SootMethod sm = Scene.v().makeSootMethod("setIntent", Collections.singletonList(intentType), VoidType.v(),
				Modifier.PUBLIC);
		component.addMethod(sm);
		sm.addTag(SimulatedCodeElementTag.TAG);

		JimpleBody b = Jimple.v().newBody(sm);
		sm.setActiveBody(b);
		b.insertIdentityStmts();

		Local lcIntent = b.getParameterLocal(0);
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(b.getThisLocal(),
				componentExchangeInfo.setIntentMethod.makeRef(), Arrays.asList(lcIntent))));
		b.getUnits().add(Jimple.v().newReturnVoidStmt());
	}

	/**
	 * Creates an implementation of setResult() that writes the given intent into
	 * the correct field
	 */
	private void createSetResultMethod() {
		// We need to create an implementation of "getIntent". If there is already such
		// an implementation, we don't touch it.
		if (component.declaresMethod("void setResult(int,android.content.Intent)"))
			return;

		Type intentType = RefType.v("android.content.Intent");
		List<Type> params = new ArrayList<>();
		params.add(IntType.v());
		params.add(intentType);
		SootMethod sm = Scene.v().makeSootMethod("setResult", params, VoidType.v(), Modifier.PUBLIC);
		component.addMethod(sm);
		sm.addTag(SimulatedCodeElementTag.TAG);

		JimpleBody b = Jimple.v().newBody(sm);
		sm.setActiveBody(b);
		b.insertIdentityStmts();

		Local lcIntent = b.getParameterLocal(1);
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(b.getThisLocal(),
				componentExchangeInfo.setResultIntentMethod.makeRef(), Arrays.asList(lcIntent))));
		b.getUnits().add(Jimple.v().newReturnVoidStmt());

		// Activity.setResult() is final. We need to change that
		SootMethod smSetResult = Scene.v()
				.grabMethod("<android.app.Activity: void setResult(int,android.content.Intent)>");
		if (smSetResult != null && smSetResult.getDeclaringClass().isApplicationClass())
			smSetResult.setModifiers(smSetResult.getModifiers() & ~Modifier.FINAL);
	}

	@Override
	protected SootClass getModelledClass() {
		return Scene.v().getSootClass(AndroidEntryPointConstants.ACTIVITYCLASS);
	}

}
