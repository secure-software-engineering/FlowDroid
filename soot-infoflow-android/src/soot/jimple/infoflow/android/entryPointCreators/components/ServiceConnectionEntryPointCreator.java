package soot.jimple.infoflow.android.entryPointCreators.components;

import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.manifest.IManifestHandler;

/**
 * Entry point creator for Android service connections
 * 
 * @author Steven Arzt
 *
 */
public class ServiceConnectionEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ServiceConnectionEntryPointCreator(SootClass component, SootClass applicationClass,
			IManifestHandler manifest) {
		super(component, applicationClass, manifest);
	}

	@Override
	protected void generateComponentLifecycle() {
		searchAndBuildMethod(AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICECONNECTED, component, thisLocal);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);
		addCallbackMethods();
		body.getUnits().add(endWhileStmt);
		createIfStmt(startWhileStmt);

		searchAndBuildMethod(AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICEDISCONNECTED, component, thisLocal);
	}

}
