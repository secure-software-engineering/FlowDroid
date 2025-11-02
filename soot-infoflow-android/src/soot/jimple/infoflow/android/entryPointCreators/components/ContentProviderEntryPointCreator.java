package soot.jimple.infoflow.android.entryPointCreators.components;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import soot.Body;
import soot.DefaultLocalGenerator;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.IdentityStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.ParameterRef;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.entryPointCreators.ComponentExchangeInfo;
import soot.jimple.infoflow.android.manifest.IManifestHandler;
import soot.jimple.infoflow.util.SootUtils;
import soot.tagkit.ExpectedTypeTag;

/**
 * Entry point creator for content providers
 * 
 * @author Steven Arzt
 *
 */
public class ContentProviderEntryPointCreator extends AbstractComponentEntryPointCreator {

	private SootMethod initMethod;
	private String initCPMethodName = "initCP";

	public ContentProviderEntryPointCreator(SootClass component, SootClass applicationClass, IManifestHandler manifest,
			SootField instantiatorField, SootField classLoaderField, ComponentExchangeInfo componentExchangeInfo) {
		super(component, applicationClass, manifest, instantiatorField, classLoaderField, componentExchangeInfo);
	}

	public SootMethod createInit() {
		SootMethod m = createEmptyInitMethod();
		body = m.retrieveActiveBody();
		generator = new DefaultLocalGenerator(body);
		Local l = generateClassConstructor(component);
		searchAndBuildMethod(AndroidEntryPointConstants.CONTENTPROVIDER_ONCREATE, l);

		body.getUnits().add(Jimple.v().newReturnStmt(l));
		//make sure the body doesn't get used when creating the actual dummy main
		body = null;
		generator = null;
		return m;
	}

	@Override
	protected List<Type> getAdditionalMainMethodParams() {
		return Arrays.asList(RefType.v(AndroidEntryPointConstants.CONTENTPROVIDERCLASS));
	}

	private SootMethod createEmptyInitMethod() {
		int methodIndex = 0;
		String methodName = initCPMethodName;
		SootClass mainClass = getOrCreateDummyMainClass();
		while (mainClass.declaresMethodByName(methodName))
			methodName = initCPMethodName + "_" + methodIndex++;

		Body body;

		// Create the method
		SootMethod initMethod = Scene.v().makeSootMethod(methodName, Collections.emptyList(),
				RefType.v(AndroidEntryPointConstants.CONTENTPROVIDERCLASS));

		// Create the body
		body = Jimple.v().newBody();
		body.setMethod(initMethod);
		initMethod.setActiveBody(body);

		// Add the method to the class
		mainClass.addMethod(initMethod);

		// First add class to scene, then make it an application class
		// as addClass contains a call to "setLibraryClass"
		mainClass.setApplicationClass();
		initMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

		this.initMethod = initMethod;
		return initMethod;
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		List<SootMethod> list = new ArrayList<>(super.getAdditionalMethods());
		if (initMethod != null)
			list.add(initMethod);
		return list;
	}

	@Override
	protected void createEmptyMainMethod() {
		super.createEmptyMainMethod();
		//the parameter with the content provider local
		JimpleBody jb = (JimpleBody) mainMethod.getActiveBody();
		thisLocal = jb.getParameterLocal(1);
		for (Unit i : jb.getUnits()) {
			if (i instanceof IdentityStmt && ((IdentityStmt) i).getRightOp() instanceof ParameterRef) {
				ParameterRef paramRef = (ParameterRef) ((IdentityStmt) i).getRightOp();
				if (paramRef.getIndex() == 1) {
					i.addTag(new ExpectedTypeTag(component.getType()));
				}
			}
		}
	}

	@Override
	protected Local generateClassConstructor(SootClass createdClass) {
		if (createdClass == component && instantiatorField != null) {
			return super.generateInstantiator(createdClass,
					AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEPROVIDER);
		}
		return super.generateClassConstructor(createdClass);
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
		SootClass providerClass = getModelledClass();
		for (String methodSig : AndroidEntryPointConstants.getContentproviderLifecycleMethods()) {
			SootMethod sm = SootUtils.findMethod(providerClass, methodSig);
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

	@Override
	protected SootClass getModelledClass() {
		return Scene.v().getSootClass(AndroidEntryPointConstants.CONTENTPROVIDERCLASS);
	}
}
