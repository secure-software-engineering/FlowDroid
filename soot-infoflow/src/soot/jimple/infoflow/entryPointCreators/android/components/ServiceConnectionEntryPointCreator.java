package soot.jimple.infoflow.entryPointCreators.android.components;

import soot.Local;
import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointConstants;

/**
 * Entry point creator for Android service connections
 * 
 * @author Steven Arzt
 *
 */
public class ServiceConnectionEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ServiceConnectionEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	protected void generateComponentLifecycle(Local localVal) {
		generateServiceConnectionLifecycle(component, localVal);
	}

	private void generateServiceConnectionLifecycle(SootClass currentClass, Local classLocal) {
		Stmt onServiceConnectedStmt = searchAndBuildMethod(
				AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICECONNECTED, currentClass, classLocal);
		body.getUnits().add(onServiceConnectedStmt);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);
		addCallbackMethods();
		body.getUnits().add(endWhileStmt);
		createIfStmt(startWhileStmt);

		Stmt onServiceDisconnectedStmt = searchAndBuildMethod(
				AndroidEntryPointConstants.SERVICECONNECTION_ONSERVICEDISCONNECTED, currentClass, classLocal);
		body.getUnits().add(onServiceDisconnectedStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

}
