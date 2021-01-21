package soot.jimple.infoflow.android.entryPointCreators.components;

import soot.SootClass;
import soot.SootMethod;
import soot.UnitPatchingChain;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.manifest.IManifestHandler;

/**
 * Entry point creator for content providers
 * 
 * @author Steven Arzt
 *
 */
public class ContentProviderEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ContentProviderEntryPointCreator(SootClass component, SootClass applicationClass,
			IManifestHandler manifest) {
		super(component, applicationClass, manifest);
	}

	@Override
	protected void generateComponentLifecycle() {
		// ContentProvider.onCreate() runs before everything else, even before
		// Application.onCreate(). We must thus handle it elsewhere.
		// Stmt onCreateStmt =
		// searchAndBuildMethod(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE,
		// currentClass, entryPoints, classLocal);

		final UnitPatchingChain units = body.getUnits();

		// see:
		// http://developer.android.com/reference/android/content/ContentProvider.html
		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		units.add(startWhileStmt);
		createIfStmt(endWhileStmt);

		addCallbackMethods();

		NopStmt beforeCallbacksStmt = Jimple.v().newNopStmt();
		units.add(beforeCallbacksStmt);
		for (String methodSig : AndroidEntryPointConstants.getContentproviderLifecycleMethods()) {
			SootMethod sm = findMethod(component, methodSig);
			if (sm != null && !sm.getSubSignature().equals(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE)) {
				NopStmt afterMethodStmt = Jimple.v().newNopStmt();
				createIfStmt(afterMethodStmt);
				buildMethodCall(sm, thisLocal);
				units.add(afterMethodStmt);
			}
		}
		createIfStmt(beforeCallbacksStmt);

		units.add(endWhileStmt);
	}

}
