package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.Collections;
import java.util.List;

import heros.TwoElementSet;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.ComponentExchangeInfo;
import soot.jimple.infoflow.android.manifest.IManifestHandler;

/**
 * Entry point creator for Android fragments
 * 
 * @author Steven Arzt
 *
 */
public class FragmentEntryPointCreator extends AbstractComponentEntryPointCreator {

	public FragmentEntryPointCreator(SootClass component, SootClass applicationClass, IManifestHandler manifest,
			SootField instantiatorField, SootField classLoaderField, ComponentExchangeInfo componentExchangeInfo) {
		super(component, applicationClass, manifest, instantiatorField, classLoaderField, componentExchangeInfo);
	}

	@Override
	protected void generateComponentLifecycle() {
		// We need the local for the parent activity
		Local lcActivity = body.getParameterLocal(getDefaultMainMethodParams().size());
		if (!(lcActivity.getType() instanceof RefType))
			throw new RuntimeException("Activities must be reference types");
		RefType rtActivity = (RefType) lcActivity.getType();
		SootClass scActivity = rtActivity.getSootClass();

		// The onAttachFragment() callbacks tells the activity that a
		// new fragment was attached
		TwoElementSet<SootClass> classAndFragment = new TwoElementSet<SootClass>(component, scActivity);
		Stmt afterOnAttachFragment = Jimple.v().newNopStmt();
		createIfStmt(afterOnAttachFragment);
		searchAndBuildMethod(AndroidEntryPointConstants.ACTIVITY_ONATTACHFRAGMENT, thisLocal, classAndFragment);
		body.getUnits().add(afterOnAttachFragment);

		// Render the fragment lifecycle
		generateFragmentLifecycle(component, thisLocal, scActivity);
	}

	/**
	 * Generates the lifecycle for an Android Fragment class
	 * 
	 * @param currentClass The class for which to build the fragment lifecycle
	 * @param classLocal   The local referencing an instance of the current class
	 * 
	 */
	private void generateFragmentLifecycle(SootClass currentClass, Local classLocal, SootClass activity) {
		NopStmt endFragmentStmt = Jimple.v().newNopStmt();
		createIfStmt(endFragmentStmt);

		// 1. onAttach:
		Stmt onAttachStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONATTACH, classLocal,
				Collections.singleton(activity));
		if (onAttachStmt == null)
			body.getUnits().add(onAttachStmt = Jimple.v().newNopStmt());

		// 2. onCreate:
		Stmt onCreateStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONCREATE, classLocal);
		if (onCreateStmt == null)
			body.getUnits().add(onCreateStmt = Jimple.v().newNopStmt());

		// 3. onCreateView:
		Stmt onCreateViewStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONCREATEVIEW, classLocal);
		if (onCreateViewStmt == null)
			body.getUnits().add(onCreateViewStmt = Jimple.v().newNopStmt());

		Stmt onViewCreatedStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONVIEWCREATED, classLocal);
		if (onViewCreatedStmt == null)
			body.getUnits().add(onViewCreatedStmt = Jimple.v().newNopStmt());

		// 0. onActivityCreated:
		Stmt onActCreatedStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONACTIVITYCREATED, classLocal);
		if (onActCreatedStmt == null)
			body.getUnits().add(onActCreatedStmt = Jimple.v().newNopStmt());

		// 4. onStart:
		Stmt onStartStmt = searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSTART, classLocal);
		if (onStartStmt == null)
			body.getUnits().add(onStartStmt = Jimple.v().newNopStmt());

		// 5. onResume:
		Stmt onResumeStmt = Jimple.v().newNopStmt();
		body.getUnits().add(onResumeStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONRESUME, classLocal);

		// Add the fragment callbacks
		addCallbackMethods();

		// 6. onPause:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONPAUSE, classLocal);
		createIfStmt(onResumeStmt);

		// 7. onSaveInstanceState:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSAVEINSTANCESTATE, classLocal);

		// 8. onStop:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONSTOP, classLocal);
		createIfStmt(onCreateViewStmt);
		createIfStmt(onStartStmt);

		// 9. onDestroyView:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDESTROYVIEW, classLocal);
		createIfStmt(onCreateViewStmt);

		// 10. onDestroy:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDESTROY, classLocal);

		// 11. onDetach:
		searchAndBuildMethod(AndroidEntryPointConstants.FRAGMENT_ONDETACH, classLocal);
		createIfStmt(onAttachStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
		body.getUnits().add(endFragmentStmt);
	}

	@Override
	protected List<Type> getAdditionalMainMethodParams() {
		return Collections.singletonList((Type) RefType.v(AndroidEntryPointConstants.ACTIVITYCLASS));
	}

	@Override
	protected SootClass getModelledClass() {
		return Scene.v().getSootClass(AndroidEntryPointConstants.FRAGMENTCLASS);
	}

}
