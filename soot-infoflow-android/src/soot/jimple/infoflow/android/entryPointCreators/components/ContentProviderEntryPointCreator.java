package soot.jimple.infoflow.android.entryPointCreators.components;

import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;

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
	protected void generateComponentLifecycle() {
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
	}

}
