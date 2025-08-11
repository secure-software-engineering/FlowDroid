package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import soot.BooleanConstant;
import soot.BooleanType;
import soot.Hierarchy;
import soot.Local;
import soot.LocalGenerator;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.UnitPatchingChain;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.util.Chain;

//The generated implementations of this class are semantically equivalent to the AppComponentFactory in Android:
//https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/AppComponentFactory.java
public class AndroidLibraryClassPatcher extends LibraryClassPatcher {

	@Override
	public void patchLibraries() {
		super.patchLibraries();

		patchComponentFactory();
	}

	protected void patchComponentFactory() {
		SootClass sc = Scene.v().forceResolve(AndroidEntryPointConstants.APPCOMPONENTFACTORYCLASS,
				SootClass.SIGNATURES);

		patchInstantiate(sc, AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEAPPLICATION,
				getAllNames(AndroidEntryPointConstants.APPLICATIONCLASS));
		patchInstantiate(sc, AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEACTIVITY,
				getAllNames(AndroidEntryPointConstants.ACTIVITYCLASS));
		patchInstantiate(sc, AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATEPROVIDER,
				getAllNames(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS));
		patchInstantiate(sc, AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATERECEIVER,
				getAllNames(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS));

		patchInstantiateClassLoader(sc);

	}

	private void patchInstantiateClassLoader(SootClass sc) {
		SootMethod smInstantiate = getOrCreateMethod(sc,
				AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATECLASSLOADER);
		JimpleBody body = Jimple.v().newBody(smInstantiate);
		smInstantiate.setActiveBody(body);
		body.insertIdentityStmts();
		body.getUnits().add(Jimple.v().newReturnStmt(body.getParameterLocal(0)));

	}

	protected String[] getAllNames(String className) {
		List<String> names = new ArrayList<>();
		SootClass sc = Scene.v().getSootClassUnsafe(className);
		if (sc == null)
			return new String[0];
		Hierarchy fh = Scene.v().getActiveHierarchy();
		List<SootClass> components;
		if (sc.isInterface()) {
			components = fh.getImplementersOf(sc);
		} else {
			components = fh.getSubclassesOf(sc);

		}
		for (SootClass c : components) {
			if (c.isConcrete())
				names.add(c.getName());
		}
		return names.toArray(new String[names.size()]);
	}

	private void patchInstantiate(SootClass sc, String subsig, String... name) {

		if (!sc.isLibraryClass())
			sc.setLibraryClass();

		// We sometimes seem to be missing the constructor
		SootMethod smInstantiate = getOrCreateMethod(sc, subsig);
		Jimple j = Jimple.v();
		JimpleBody body = j.newBody(smInstantiate);
		if (smInstantiate.isPhantom())
			smInstantiate.setPhantom(false);
		smInstantiate.setActiveBody(body);
		body.insertIdentityStmts();
		Chain<Local> locals = body.getLocals();
		UnitPatchingChain units = body.getUnits();
		Scene scene = Scene.v();
		Local ret = j.newLocal("returnVal", smInstantiate.getReturnType());
		Local obj = j.newLocal("obj", scene.getObjectType());
		Local cls = j.newLocal("clazz", RefType.v("java.lang.Class"));
		locals.add(ret);
		locals.add(obj);
		locals.add(cls);
		LocalGenerator generator = Scene.v().createLocalGenerator(body);

		Local cmp = null;
		NopStmt next = null;
		for (String n : name) {
			if (n != null) {
				RefType p = RefType.v(n);
				if (p.hasSootClass() && p.getSootClass().isApplicationClass()) {
					SootMethod ctor = p.getSootClass().getMethodUnsafe("void <init>()");
					if (ctor != null) {
						if (cmp == null) {
							cmp = j.newLocal("bool", BooleanType.v());
							locals.add(cmp);
						}
						if (next != null)
							units.add(next);
						units.add(j.newAssignStmt(cmp,
								j.newVirtualInvokeExpr(body.getParameterLocal(1),
										scene.makeMethodRef(RefType.v("java.lang.String").getSootClass(),
												"boolean equals(java.lang.Object)", false),
										StringConstant.v(p.getClassName()))));
						next = j.newNopStmt();
						units.add(j.newIfStmt(j.newEqExpr(cmp, BooleanConstant.v(false)), next));
						Local c = generator.generateLocal(p);
						units.add(j.newAssignStmt(c, j.newNewExpr(p)));
						units.add(j.newInvokeStmt(j.newSpecialInvokeExpr(c, ctor.makeRef())));
						units.add(j.newReturnStmt(c));
					}
				}
			}
		}
		if (next != null)
			units.add(next);
		units.add(
				j.newAssignStmt(cls,
						j.newVirtualInvokeExpr(body.getParameterLocal(0),
								scene.makeMethodRef(RefType.v("java.lang.ClassLoader").getSootClass(),
										"java.lang.Class loadClass(java.lang.String)", false),
								body.getParameterLocal(1))));
		units.add(j.newAssignStmt(obj, j.newVirtualInvokeExpr(cls, scene
				.makeMethodRef(RefType.v("java.lang.Class").getSootClass(), "java.lang.Object newInstance()", false))));
		units.add(j.newAssignStmt(ret, j.newCastExpr(obj, obj.getType())));
		units.add(j.newReturnStmt(ret));
		NopEliminator.v().transform(body);

	}

	private static SootMethod getOrCreateMethod(SootClass sc, String subsig) {
		SootMethod p = sc.getMethodUnsafe(subsig);
		if (p != null)
			return p;

		SootMethodRepresentationParser parser = SootMethodRepresentationParser.v();
		String name = parser.getMethodNameFromSubSignature(subsig);

		Scene scene = Scene.v();
		String sreturnType = parser.getReturnTypeFromSubSignature(subsig);

		String[] paramTypes = parser.getParameterTypesFromSubSignature(subsig);
		Type returnType = scene.getTypeUnsafe(sreturnType, false);
		Type[] aparamTypes = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			aparamTypes[i] = scene.getTypeUnsafe(paramTypes[i], false);
		}
		p = Scene.v().makeSootMethod(name, Arrays.<Type>asList(aparamTypes), returnType, Modifier.PUBLIC);
		return sc.getOrAddMethod(p);
	}
}
