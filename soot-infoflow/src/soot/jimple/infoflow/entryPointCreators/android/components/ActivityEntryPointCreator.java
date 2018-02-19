package soot.jimple.infoflow.entryPointCreators.android.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import heros.TwoElementSet;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointConstants;
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
	private final Set<SootClass> fragmentClasses;

	public ActivityEntryPointCreator(SootClass component, SootClass applicationClass,
			MultiMap<SootClass, String> activityLifecycleCallbacks, Set<SootClass> fragmentClasses,
			Map<SootClass, SootField> callbackClassToField) {
		super(component, applicationClass);
		this.activityLifecycleCallbacks = activityLifecycleCallbacks;
		this.fragmentClasses = fragmentClasses;
		this.callbackClassToField = callbackClassToField;
	}

	@Override
	protected void generateComponentLifecycle(Local localVal) {
		generateActivityLifecycle(component, localVal);
	}

	/**
	 * Generates the lifecycle for an Android activity
	 * 
	 * @param currentClass
	 *            The class for which to build the activity lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateActivityLifecycle(SootClass currentClass, Local classLocal) {
		Set<SootClass> currentClassSet = Collections.singleton(currentClass);
		final Body body = mainMethod.getActiveBody();

		Set<SootClass> referenceClasses = new HashSet<>();
		if (applicationClass != null)
			referenceClasses.add(applicationClass);
		if (this.activityLifecycleCallbacks != null)
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet())
				referenceClasses.add(callbackClass);
		if (this.fragmentClasses != null)
			for (SootClass callbackClass : this.fragmentClasses)
				referenceClasses.add(callbackClass);
		referenceClasses.add(currentClass);

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

		// Create the instances of the fragment classes
		if (fragmentClasses != null && !fragmentClasses.isEmpty()) {
			NopStmt beforeCbCons = Jimple.v().newNopStmt();
			body.getUnits().add(beforeCbCons);

			createClassInstances(fragmentClasses);

			// Jump back to overapproximate the order in which the
			// constructors are called
			createIfStmt(beforeCbCons);
		}

		// 1. onCreate:
		{
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATE, currentClass, classLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED,
						callbackClass, localVarsForClasses.get(callbackClass), currentClassSet);
			}
		}

		// Adding the lifecycle of the Fragments that belong to this Activity:
		// iterate through the fragments detected in the CallbackAnalyzer
		if (fragmentClasses != null && !fragmentClasses.isEmpty()) {
			for (SootClass scFragment : fragmentClasses) {
				// Get a class local
				Local fragmentLocal = localVarsForClasses.get(scFragment);
				Set<Local> tempLocals = new HashSet<>();
				if (fragmentLocal == null) {
					fragmentLocal = generateClassConstructor(scFragment, body, new HashSet<SootClass>(),
							referenceClasses, tempLocals);
					if (fragmentLocal == null)
						continue;
					localVarsForClasses.put(scFragment, fragmentLocal);
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
				generateFragmentLifecycle(scFragment, fragmentLocal, currentClass);

				// Get rid of the locals
				body.getUnits().add(Jimple.v().newAssignStmt(fragmentLocal, NullConstant.v()));
				for (Local tempLocal : tempLocals)
					body.getUnits().add(Jimple.v().newAssignStmt(tempLocal, NullConstant.v()));
			}
		}

		// 2. onStart:
		Stmt onStartStmt;
		{
			onStartStmt = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSTART, currentClass, classLocal);
			for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
				Stmt s = searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
						callbackClass, localVarsForClasses.get(callbackClass), currentClassSet);
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
						callbackClass, localVarsForClasses.get(callbackClass), currentClassSet);
			}
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPOSTRESUME, currentClass, classLocal);

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

		// 4. onPause:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONPAUSE, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED, callbackClass,
					localVarsForClasses.get(callbackClass), currentClassSet);
		}
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONCREATEDESCRIPTION, currentClass, classLocal);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONSAVEINSTANCESTATE, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
					callbackClass, localVarsForClasses.get(callbackClass), currentClassSet);
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

		// 6. onRestart:
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONRESTART, currentClass, classLocal);
		createIfStmt(onStartStmt); // jump to onStart(), fall through to
									// onDestroy()

		// 7. onDestroy
		body.getUnits().add(stopToDestroyStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONDESTROY, currentClass, classLocal);
		for (SootClass callbackClass : this.activityLifecycleCallbacks.keySet()) {
			searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED,
					callbackClass, localVarsForClasses.get(callbackClass), currentClassSet);
		}

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

	/**
	 * Generates the lifecycle for an Android Fragment class
	 * 
	 * @param currentClass
	 *            The class for which to build the fragment lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 * 
	 */
	private void generateFragmentLifecycle(SootClass currentClass, Local classLocal, SootClass activity) {
		NopStmt endFragmentStmt = Jimple.v().newNopStmt();
		createIfStmt(endFragmentStmt);

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
		body.getUnits().add(endFragmentStmt);
	}

}
