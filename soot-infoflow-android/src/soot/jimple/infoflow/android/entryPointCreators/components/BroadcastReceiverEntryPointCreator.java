package soot.jimple.infoflow.android.entryPointCreators.components;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.ComponentExchangeInfo;
import soot.jimple.infoflow.android.manifest.IManifestHandler;

/**
 * Entry point creator for Android broadcast receivers
 * 
 * @author Steven Arzt
 *
 */
public class BroadcastReceiverEntryPointCreator extends AbstractComponentEntryPointCreator {

	public BroadcastReceiverEntryPointCreator(SootClass component, SootClass applicationClass,
			IManifestHandler manifest, SootField instantiatorField, SootField classLoaderField,
			ComponentExchangeInfo componentExchangeInfo) {
		super(component, applicationClass, manifest, instantiatorField, classLoaderField, componentExchangeInfo);
	}

	@Override
	protected Local generateClassConstructor(SootClass createdClass) {
		if (createdClass == component && instantiatorField != null) {
			return super.generateInstantiator(createdClass,
					AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATERECEIVER, body.getParameterLocal(0));
		}
		return super.generateClassConstructor(createdClass);
	}

	@Override
	protected void generateComponentLifecycle() {
		Stmt onReceiveStmt = searchAndBuildMethod(AndroidEntryPointConstants.BROADCAST_ONRECEIVE, thisLocal);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		addCallbackMethods();

		body.getUnits().add(endWhileStmt);
		createIfStmt(onReceiveStmt);
	}

	@Override
	protected void createAdditionalMethods() {
		super.createAdditionalMethods();

		createGetIntentMethod();
	}

	@Override
	protected SootClass getModelledClass() {
		return Scene.v().getSootClass(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS);
	}
}
