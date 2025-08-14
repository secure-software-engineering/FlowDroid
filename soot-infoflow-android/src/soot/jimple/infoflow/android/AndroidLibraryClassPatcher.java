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

/**
 * In addition to the normal JVM library classes, this class also patches
 * certain Android library classes.
 */
public class AndroidLibraryClassPatcher extends LibraryClassPatcher {

	@Override
	public void patchLibraries() {
		super.patchLibraries();

		patchComponentFactory();
	}

	/**
	 * The generated implementation of this method are semantically equivalent to the AppComponentFactory in Android.
	 * @see https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/AppComponentFactory.java
	 */
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

	/**
	 * Patches the instantiate classloader class.
	 * It returns the default class loader unmodified.
	 * @param sc the class of the app component factory
	 */
	private void patchInstantiateClassLoader(SootClass sc) {
		SootMethod smInstantiate = getOrCreateMethod(sc,
				AndroidEntryPointConstants.APPCOMPONENTFACTORY_INSTANTIATECLASSLOADER);
		JimpleBody body = Jimple.v().newBody(smInstantiate);
		smInstantiate.setActiveBody(body);
		body.insertIdentityStmts();
		body.getUnits().add(Jimple.v().newReturnStmt(body.getParameterLocal(0)));

	}

	/**
	 * Returns all class names that could be instantiated when
	 * instantiating a class with the given class name, i.e. all subclasses/implementers.
	 * @param className the class name (could also represent an interface)
	 * @return a string array of all possible names.
	 */
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

	/**
	 * Patches an instantiate method. Generates code equivalent to the following:
	 * 
	 * <code>
	 * public void instantiateActivity(ClassLoader cl, String className, Intent intent)
	 * {
	 * 
	 * 	if (className.equals("foo.bar.MainActivity"))
	 * 		return new foo.bar.MainActivity(); //(1)
	 * 	if (className.equals("foo.bar.FooActivity"))
	 * 		return new foo.bar.FooActivity();  //(2)
	 *  return cl.loadClass(className).newInstance(); //(3)
	 *  
	 * }
	 * </code>
	 * The instantiation statements (1) and (2) are used to help SPARK and other static algorithms to find
	 * allocation sites. (3) is the fallback that would normally be the implementation when using Android's default 
	 * app component factory.
	 * @param sc the class of the app component factory
	 * @param subsig the sub signature of the method, in our example case instantiateActivity
	 * @param names the names for each possible class instantiation, in our example case "foo.bar.MainActivity", "foo.bar.FooActivity"
	 */
	protected void patchInstantiate(SootClass sc, String subsig, String... names) {

		if (!sc.isLibraryClass())
			sc.setLibraryClass();

		// We sometimes seem to be missing the constructor
		SootMethod smInstantiate = getOrCreateMethod(sc, subsig);
		Jimple j = Jimple.v();
		JimpleBody body = j.newBody(smInstantiate);
		if (smInstantiate.isPhantom())
			smInstantiate.setPhantom(false);
		smInstantiate.setModifiers(Modifier.PUBLIC);
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
		for (String n : names) {
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

	/**
	 * Creates a method if it doesn't exist. Otherwise, it returns the existing method
	 * @param sc the class where the method is being looked for
	 * @param subsig the sub signature of the method
	 * @return the method
	 */
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
