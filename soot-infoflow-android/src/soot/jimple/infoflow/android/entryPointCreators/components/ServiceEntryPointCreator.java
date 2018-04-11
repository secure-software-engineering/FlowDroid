package soot.jimple.infoflow.android.entryPointCreators.components;

import java.lang.reflect.Modifier;
import java.util.Collections;

import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils.ComponentType;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;

/**
 * Entry point creator for Android services
 * 
 * @author Steven Arzt
 *
 */
public class ServiceEntryPointCreator extends AbstractComponentEntryPointCreator {

	protected SootField binderField = null;

	public ServiceEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	@Override
	protected void generateComponentLifecycle() {
		// 1. onCreate:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONCREATE, component, thisLocal);

		// service has two different lifecycles:
		// lifecycle1:
		// 2. onStart:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONSTART1, component, thisLocal);

		// onStartCommand can be called an arbitrary number of times, or never
		NopStmt beforeStartCommand = Jimple.v().newNopStmt();
		NopStmt afterStartCommand = Jimple.v().newNopStmt();
		body.getUnits().add(beforeStartCommand);
		createIfStmt(afterStartCommand);

		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONSTART2, component, thisLocal);
		createIfStmt(beforeStartCommand);
		body.getUnits().add(afterStartCommand);

		// methods:
		// all other entryPoints of this class:
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		ComponentType componentType = entryPointUtils.getComponentType(component);
		boolean hasAdditionalMethods = false;
		if (componentType == ComponentType.GCMBaseIntentService) {
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(component, sig);
				if (sm != null && !sm.getDeclaringClass().getName()
						.equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					if (createPlainMethodCall(thisLocal, sm))
						hasAdditionalMethods = true;
			}
		} else if (componentType == ComponentType.GCMListenerService) {
			for (String sig : AndroidEntryPointConstants.getGCMListenerServiceMethods()) {
				SootMethod sm = findMethod(component, sig);
				if (sm != null
						&& !sm.getDeclaringClass().getName().equals(AndroidEntryPointConstants.GCMLISTENERSERVICECLASS))
					if (createPlainMethodCall(thisLocal, sm))
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
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONBIND, component, thisLocal);

		NopStmt beforemethodsStmt = Jimple.v().newNopStmt();
		body.getUnits().add(beforemethodsStmt);
		// methods
		NopStmt startWhile2Stmt = Jimple.v().newNopStmt();
		NopStmt endWhile2Stmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhile2Stmt);
		hasAdditionalMethods = false;
		if (componentType == ComponentType.GCMBaseIntentService)
			for (String sig : AndroidEntryPointConstants.getGCMIntentServiceMethods()) {
				SootMethod sm = findMethod(component, sig);
				if (sm != null && !sm.getName().equals(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS))
					if (createPlainMethodCall(thisLocal, sm))
						hasAdditionalMethods = true;
			}
		addCallbackMethods();
		body.getUnits().add(endWhile2Stmt);
		if (hasAdditionalMethods)
			createIfStmt(startWhile2Stmt);

		// onUnbind:
		Stmt onDestroyStmt = Jimple.v().newNopStmt();
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONUNBIND, component, thisLocal);
		createIfStmt(onDestroyStmt); // fall through to rebind or go to destroy

		// onRebind:
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONREBIND, component, thisLocal);
		createIfStmt(beforemethodsStmt);

		// lifecycle2 end

		// onDestroy:
		body.getUnits().add(onDestroyStmt);
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICE_ONDESTROY, component, thisLocal);
	}

	@Override
	protected void createAdditionalFields() {
		super.createAdditionalFields();

		// Create a name for a field for the binder (passed to us in onBind())
		String fieldName = "ipcIntent";
		int fieldIdx = 0;
		while (component.declaresFieldByName(fieldName))
			fieldName = "ipcBinder_" + fieldIdx++;

		// Create the field itself
		binderField = Scene.v().makeSootField(fieldName, RefType.v("android.os.IBinder"), Modifier.PUBLIC);
		component.addField(binderField);
	}

	@Override
	protected void createAdditionalMethods() {
		super.createAdditionalMethods();

		// We need to instrument the onBind() method to store the binder in the field
		instrumentOnBind();
	}

	/**
	 * Modifies the onBind() method such that it stores the IBinder, which gets
	 * passed in as an argument, in the global field
	 */
	private void instrumentOnBind() {
		SootMethod sm = component.getMethodUnsafe("android.os.IBinder onBind(android.content.Intent)");
		final Type intentType = RefType.v("android.content.Intent");
		final Type binderType = RefType.v("android.os.IBinder");
		if (sm == null || !sm.hasActiveBody()) {
			// Create a new onBind() method
			if (sm == null) {
				sm = Scene.v().makeSootMethod("onBind", Collections.singletonList(intentType), binderType,
						Modifier.PUBLIC);
				component.addMethod(sm);
				sm.addTag(SimulatedCodeElementTag.TAG);
			}

			// Create the body
			final JimpleBody b = Jimple.v().newBody(sm);
			sm.setActiveBody(b);
			b.insertIdentityStmts();

			final Local thisLocal = b.getThisLocal();
			final Local binderLocal = b.getParameterLocal(0);

			b.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal, binderField.makeRef()),
					binderLocal));
			b.getUnits().add(Jimple.v().newReturnStmt(binderLocal));
		} else {
			// Modify the existing onBind() method
			JimpleBody b = (JimpleBody) sm.getActiveBody();
			Stmt firstNonIdentityStmt = b.getFirstNonIdentityStmt();

			final Local thisLocal = b.getThisLocal();
			final Local binderLocal = b.getParameterLocal(0);

			b.getUnits().insertAfter(Jimple.v()
					.newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal, binderField.makeRef()), binderLocal),
					firstNonIdentityStmt);
		}
	}

	@Override
	protected void reset() {
		super.reset();

		component.removeField(binderField);
		binderField = null;
	}

	@Override
	public ComponentEntryPointInfo getComponentInfo() {
		ServiceEntryPointInfo serviceInfo = new ServiceEntryPointInfo(mainMethod);
		serviceInfo.setIntentField(intentField);
		serviceInfo.setBinderField(binderField);
		return serviceInfo;
	}

}
