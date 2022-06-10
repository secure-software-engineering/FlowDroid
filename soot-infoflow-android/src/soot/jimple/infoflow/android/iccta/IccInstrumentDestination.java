package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.LocalGenerator;
import soot.Modifier;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;

/**
 * One ICC Link contain one source component and one destination component. this
 * class is used to collect all the assist methods which instrument destination
 * component.
 *
 */
public class IccInstrumentDestination {
	private static IccInstrumentDestination s = null;

	private IccInstrumentDestination() {
	}

	public static IccInstrumentDestination v() {
		if (s == null) {
			s = new IccInstrumentDestination();
		}
		return s;
	}

	private static RefType INTENT_TYPE = RefType.v("android.content.Intent");
	private IccLink iccLink = null;

	public SootClass instrumentDestinationForContentProvider(String destination) {
		return Scene.v().getSootClass(destination);
	}

	/**
	 * generate construct method for this component, this construct method should
	 * take one Intent as their parameter.
	 * 
	 * the best way is to use a place holder parameter to avoid exist construct
	 * method and use exist construct to build the new <init> method.
	 * 
	 * @param compSootClass
	 * @param intentSootField
	 * @return
	 */
	public SootMethod generateInitMethod(SootClass compSootClass, SootField intentSootField) {
		String name = "<init>";
		List<Type> parameters = new ArrayList<Type>();
		parameters.add(INTENT_TYPE);
		Type returnType = VoidType.v();
		int modifiers = Modifier.PUBLIC;
		SootMethod newConstructor = new SootMethod(name, parameters, returnType, modifiers);
		compSootClass.addMethod(newConstructor);
		{
			Body b = Jimple.v().newBody(newConstructor);
			newConstructor.setActiveBody(b);
			LocalGenerator lg = Scene.v().createLocalGenerator(b);
			Local thisLocal = lg.generateLocal(compSootClass.getType());
			Unit thisU = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(compSootClass.getType()));
			Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
			Unit intentParameterU = Jimple.v().newIdentityStmt(intentParameterLocal,
					Jimple.v().newParameterRef(INTENT_TYPE, 0));

			boolean noDefaultConstructMethod = false;
			Unit superU = null;
			try {
				superU = (Unit) Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal,
						compSootClass.getMethod(name, new ArrayList<Type>(), VoidType.v()).makeRef()));
			} catch (Exception ex) {
				// It is possible that a class doesn't have a default construct
				// method (<init>()).
				noDefaultConstructMethod = true;
			}

			if (noDefaultConstructMethod) {
				List<SootMethod> sootMethods = compSootClass.getMethods();
				for (SootMethod sm : sootMethods) {
					if (sm.getName().equals("<init>")) {
						if (sm.getParameterCount() == 1 && sm.getParameterType(0).equals(INTENT_TYPE)) {
							List<Value> args = new ArrayList<Value>();
							args.add(intentParameterLocal);
							superU = Jimple.v()
									.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal, sm.makeRef(), args));
							continue;
						}

						List<Value> args = new ArrayList<Value>();
						for (int i = 0; i < sm.getParameterCount(); i++) {
							args.add(NullConstant.v());
						}

						superU = Jimple.v()
								.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal, sm.makeRef(), args));
						break;
					}
				}
			}

			Unit storeIntentU = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(intentSootField.makeRef()),
					intentParameterLocal);
			b.getUnits().add(thisU);
			b.getUnits().add(intentParameterU);
			b.getUnits().add(superU);
			b.getUnits().add(storeIntentU);
			b.getUnits().add(Jimple.v().newReturnVoidStmt());
		}

		return newConstructor;
	}

	public SootField getMessageForIPCField() {
		SootClass sc = iccLink.fromSM.getDeclaringClass();
		if (!sc.declaresField("message_for_ipc_static", RefType.v("android.os.Messenge"))) {
			fieldSendingMessage(iccLink.fromSM);
		}

		return sc.getFieldByName("message_for_ipc_static");
	}

	/**
	 * To extract the real binder type, Thus, a more precision way is to perform a
	 * type analysis for IBinder reference
	 * 
	 * @return
	 */
	public Type extractBinderType(SootClass sootClass) {
		SootMethod onBindMethod = null;
		try {
			onBindMethod = sootClass.getMethodByName("onBind");
		} catch (RuntimeException ex) {
		}

		if (null == onBindMethod) {
			return null;
		}

		Body body = onBindMethod.retrieveActiveBody();
		PatchingChain<Unit> units = body.getUnits();
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			Stmt stmt = (Stmt) iter.next();

			if (stmt instanceof ReturnStmt) {
				ReturnStmt rtStmt = (ReturnStmt) stmt;
				Value rtValue = rtStmt.getOp();

				if (rtValue.toString().equals("null")) {
					return onBindMethod.getReturnType();
				}

				return rtValue.getType();
			}

		}

		return onBindMethod.getReturnType();
	}

	public void fieldSendingMessage(SootMethod fromSM) {
		SootClass fromC = fromSM.getDeclaringClass();

		if (fromC.declaresField("message_for_ipc_static", RefType.v("android.os.Message"))) {
			return;
		}

		int m = Modifier.PUBLIC | Modifier.STATIC;
		SootField sf = Scene.v().makeSootField("message_for_ipc_static", RefType.v("android.os.Message"), m);
		fromC.addField(sf);

		for (SootMethod sm : fromC.getMethods()) {
			if (sm.isConcrete()) {
				Body body = sm.retrieveActiveBody();
				PatchingChain<Unit> units = body.getUnits();

				for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					Stmt stmt = (Stmt) iter.next();

					if (stmt.containsInvokeExpr()) {
						SootMethod toBeCheckedMethod = stmt.getInvokeExpr().getMethod();
						if (toBeCheckedMethod.getName().equals("send")
								&& equalsOrSubclassOf(toBeCheckedMethod.getDeclaringClass(),
										Scene.v().getSootClass("android.os.Messenger"))) {
							Value arg0 = stmt.getInvokeExpr().getArg(0);
							Unit assignUnit = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(sf.makeRef()),
									arg0);
							assignUnit.addTag(SimulatedCodeElementTag.TAG);
							units.insertBefore(assignUnit, stmt);
						}
					}
				}
			}
		}
	}

	public boolean equalsOrSubclassOf(SootClass testClass, SootClass parentClass) {
		if (testClass.getName().equals(parentClass.getName())) {
			return true;
		}

		while (testClass.hasSuperclass()) {
			testClass = testClass.getSuperclass();

			return equalsOrSubclassOf(testClass, parentClass);
		}

		return false;
	}
}
