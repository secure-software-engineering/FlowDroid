package soot.jimple.infoflow.entryPointCreators.android.components;

import soot.Local;
import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;

/**
 * Entry point creator for content providers
 * 
 * @author Steven Arzt
 *
 */
public class ContentProviderEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ContentProviderEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	@Override
	protected void generateComponentLifecycle(Local localVal) {
		generateContentProviderLifecycle(component, localVal);
	}

	/**
	 * Generates the lifecycle for an Android content provider class
	 * 
	 * @param currentClass
	 *            The class for which to build the content provider lifecycle
	 * @param classLocal
	 *            The local referencing an instance of the current class
	 */
	private void generateContentProviderLifecycle(SootClass currentClass, Local classLocal) {
		// ContentProvider.onCreate() runs before everything else, even before
		// Application.onCreate(). We must thus handle it elsewhere.
		// Stmt onCreateStmt =
		// searchAndBuildMethod(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE,
		// currentClass, entryPoints, classLocal);

		// see:
		// http://developer.android.com/reference/android/content/ContentProvider.html
		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);

		addCallbackMethods();

		body.getUnits().add(endWhileStmt);
		body.getUnits().add(Jimple.v().newAssignStmt(classLocal, NullConstant.v()));
	}

}
