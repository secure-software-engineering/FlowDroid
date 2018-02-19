package soot.jimple.infoflow.entryPointCreators.android.components;

import soot.Local;
import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.android.AndroidEntryPointConstants;

/**
 * Entry point creator for Android broadcast receivers
 * 
 * @author Steven Arzt
 *
 */
public class BroadcastReceiverEntryPointCreator extends AbstractComponentEntryPointCreator {

	public BroadcastReceiverEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	protected void generateComponentLifecycle(Local localVal) {
		generateBroadcastReceiverLifecycle(component, localVal);
	}

	/**
	 * Generates the lifecycle for an Android broadcast receiver class
	 * 
	 * @param currentClass
	 *            The class for which to build the broadcast receiver lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateBroadcastReceiverLifecycle(SootClass currentClass, Local classLocal) {
		Stmt onReceiveStmt = searchAndBuildMethod(AndroidEntryPointConstants.BROADCAST_ONRECEIVE, currentClass,
				classLocal);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		addCallbackMethods();

		body.getUnits().add(endWhileStmt);
		createIfStmt(onReceiveStmt);

		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

}
