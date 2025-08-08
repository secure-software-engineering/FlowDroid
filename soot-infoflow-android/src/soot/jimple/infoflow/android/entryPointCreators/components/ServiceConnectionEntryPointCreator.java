package soot.jimple.infoflow.android.entryPointCreators.components;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.ComponentExchangeInfo;
import soot.jimple.infoflow.android.manifest.IManifestHandler;

/**
 * Entry point creator for Android service connections
 * 
 * @author Steven Arzt
 *
 */
public class ServiceConnectionEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ServiceConnectionEntryPointCreator(SootClass component, SootClass applicationClass,
			IManifestHandler manifest, SootField instantiatorField, SootField classLoaderField,
			ComponentExchangeInfo componentExchangeInfo) {
		super(component, applicationClass, manifest, instantiatorField, classLoaderField, componentExchangeInfo);
	}

	@Override
	protected void generateComponentLifecycle() {
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICECONNECTED, thisLocal);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);
		addCallbackMethods();
		body.getUnits().add(endWhileStmt);
		createIfStmt(startWhileStmt);

		searchAndBuildMethod(AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICEDISCONNECTED, thisLocal);
	}

	@Override
	protected SootClass getModelledClass() {
		return Scene.v().getSootClass(AndroidEntryPointConstants.SERVICECONNECTIONINTERFACE);
	}

}
