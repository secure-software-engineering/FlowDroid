package soot.jimple.infoflow.entryPointCreators.android.components;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointConstants;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointUtils.ComponentType;

/**
 * Entry point creator for Android services
 * 
 * @author Steven Arzt
 *
 */
public class ServiceEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ServiceEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	@Override
	protected void generateComponentLifecycle(Local localVal) {
		generateServiceLifecycle(component, localVal);
	}

	/**
	 * Generates the lifecycle for an Android service class
	 * 
	 * @param currentClass
	 *            The class for which to build the service lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateServiceLifecycle(SootClass currentClass, Local classLocal) {
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
		if (componentType == ComponentType.GCMBaseIntentService) {
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null && !sm.getDeclaringClass().getName()
						.equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					if (createPlainMethodCall(classLocal, sm))
						hasAdditionalMethods = true;
			}
		} else if (componentType == ComponentType.GCMListenerService) {
			for (String sig : AndroidEntryPointConstants.getGCMListenerServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null
						&& !sm.getDeclaringClass().getName().equals(AndroidEntryPointConstants.GCMLISTENERSERVICECLASS))
					if (createPlainMethodCall(classLocal, sm))
						hasAdditionalMethods = true;
			}
		}
		addCallbackMethods();
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
		if (componentType == ComponentType.GCMBaseIntentService)
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(currentClass, sig);
				if (sm != null && !sm.getName().equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					if (createPlainMethodCall(classLocal, sm))
						hasAdditionalMethods = true;
			}
		addCallbackMethods();
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
	}

}
