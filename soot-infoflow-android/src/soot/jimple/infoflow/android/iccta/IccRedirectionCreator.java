package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Local;
import soot.Modifier;
import soot.NullType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.entryPointCreators.android.components.ActivityEntryPointInfo;
import soot.jimple.infoflow.entryPointCreators.android.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.entryPointCreators.android.components.ServiceEntryPointInfo;

public class IccRedirectionCreator {

	private static int num = 0;

	private static RefType INTENT_TYPE = RefType.v("android.content.Intent");
	private static RefType IBINDER_TYPE = RefType.v("android.os.IBinder");

	private Map<String, SootMethod> source2RedirectMethod = new HashMap<>();

	private final SootClass dummyMainClass;
	private final ComponentEntryPointCollection componentToEntryPoint;

	public IccRedirectionCreator(SootClass dummyMainClass, ComponentEntryPointCollection componentToEntryPoint) {
		this.componentToEntryPoint = componentToEntryPoint;
		this.dummyMainClass = dummyMainClass;
	}

	public void redirectToDestination(IccLink link) {
		if (Scene.v().getSootClass(link.getDestinationC()).isPhantom()) {
			return;
		}

		// 1) generate redirect method
		SootMethod redirectSM = getRedirectMethod(link);

		// 2) instrument the source to call the generated redirect method after
		// ICC methods
		IccInstrumentSource.v().instrumentSource(link, redirectSM);
	}

	/**
	 * Redirect ICC call at unit in sm to the right component
	 * 
	 * @param link
	 * @return
	 */
	protected SootMethod getRedirectMethod(IccLink link) {
		SootClass instrumentedDestinationSC = Scene.v().getSootClass(link.getDestinationC());
		SootMethod redirectMethod = source2RedirectMethod.get(link.toString());

		if (redirectMethod == null) {
			// build up source2RedirectMethod map
			String source = link.toString();
			Stmt stmt = (Stmt) link.getFromU();

			if (stmt.containsInvokeExpr()) {
				if (stmt.getInvokeExpr().getMethod().getName().equals("startActivityForResult")) {
					Value expr = stmt.getInvokeExprBox().getValue();
					if (expr instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) expr;
						Type tp = iiexpr.getBase().getType();
						if (tp instanceof RefType) {
							RefType rt = (RefType) tp;
							redirectMethod = generateRedirectMethodForStartActivityForResult(rt.getSootClass(),
									instrumentedDestinationSC);
						}
					}
				} else if (stmt.getInvokeExpr().getMethod().getName().equals("bindService")) {
					Value v = stmt.getInvokeExpr().getArg(1);
					if (v.getType() instanceof RefType) {
						RefType rt = (RefType) v.getType();
						redirectMethod = generateRedirectMethodForBindService(rt.getSootClass(),
								instrumentedDestinationSC);
					}
				} else {
					redirectMethod = generateRedirectMethod(instrumentedDestinationSC);
				}
			}

			if (redirectMethod == null) {
				throw new RuntimeException("Wrong IccLink [" + link.toString() + "]");
			}

			source2RedirectMethod.put(source, redirectMethod);
		}

		return redirectMethod;
	}

	protected SootMethod generateRedirectMethodForStartActivityForResult(SootClass originActivity, SootClass destComp) {
		String newSM_name = "redirector" + num++;

		List<Type> newSM_parameters = new ArrayList<>();
		newSM_parameters.add(originActivity.getType());
		newSM_parameters.add(INTENT_TYPE);

		SootMethod newSM = new SootMethod(newSM_name, newSM_parameters, VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		final JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = new LocalGenerator(b);

		Local originActivityParameterLocal = lg.generateLocal(originActivity.getType());
		Unit originActivityParameterU = Jimple.v().newIdentityStmt(originActivityParameterLocal,
				Jimple.v().newParameterRef(originActivity.getType(), 0));
		b.getUnits().add(originActivityParameterU);

		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 1)));

		// call onCreate
		Local componentLocal = lg.generateLocal(destComp.getType());
		ActivityEntryPointInfo entryPointInfo = (ActivityEntryPointInfo) componentToEntryPoint.get(destComp);
		{
			SootMethod targetDummyMain = componentToEntryPoint.getEntryPoint(destComp);
			if (targetDummyMain == null)
				throw new RuntimeException(
						String.format("Destination component %s has no dummy main method", destComp.getName()));
			b.getUnits().add(Jimple.v().newAssignStmt(componentLocal, Jimple.v()
					.newStaticInvokeExpr(targetDummyMain.makeRef(), Collections.singletonList(intentParameterLocal))));
		}

		// Get the activity result
		Local arIntentLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newAssignStmt(arIntentLocal,
				Jimple.v().newInstanceFieldRef(componentLocal, entryPointInfo.getResultIntentField().makeRef())));

		// some apps do not have an onActivityResult method even they use
		// startActivityForResult to communicate with other components.
		SootMethod method = originActivity.getMethodUnsafe("void onActivityResult(int,int,android.content.Intent)");
		if (method != null) {
			List<Value> args = new ArrayList<>();
			args.add(IntConstant.v(-1));
			args.add(IntConstant.v(-1));
			args.add(arIntentLocal);
			b.getUnits().add(Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(originActivityParameterLocal, method.makeRef(), args)));
		}

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;
	}

	protected SootMethod generateRedirectMethod(SootClass wrapper) {
		String newSM_name = "redirector" + num++;
		SootMethod newSM = new SootMethod(newSM_name, Collections.<Type>singletonList(INTENT_TYPE), VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = new LocalGenerator(b);

		// identity
		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 0)));

		// call dummyMainMethod
		{
			SootMethod targetDummyMain = componentToEntryPoint.getEntryPoint(wrapper);
			if (targetDummyMain == null)
				throw new RuntimeException(
						String.format("Destination component %s has no dummy main method", wrapper.getName()));
			b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(targetDummyMain.makeRef(),
					Collections.singletonList(intentParameterLocal))));

		}

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;

	}

	protected SootMethod generateRedirectMethodForStartActivity(SootClass wrapper) {
		String newSM_name = "redirector" + num++;
		SootMethod newSM = new SootMethod(newSM_name, Collections.<Type>singletonList(INTENT_TYPE), VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = new LocalGenerator(b);

		// identity
		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 0)));

		// call dummyMainMethod
		{
			SootMethod targetDummyMain = componentToEntryPoint.getEntryPoint(wrapper);
			if (targetDummyMain == null)
				throw new RuntimeException(
						String.format("Destination component %s has no dummy main method", wrapper.getName()));
			b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(targetDummyMain.makeRef(),
					Collections.singletonList(intentParameterLocal))));

		}

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;

	}

	protected SootMethod generateRedirectMethodForBindService(SootClass serviceConnection, SootClass destComp) {
		String newSM_name = "redirector" + num++;

		List<Type> newSM_parameters = new ArrayList<>();
		newSM_parameters.add(serviceConnection.getType());
		newSM_parameters.add(INTENT_TYPE);

		SootMethod newSM = new SootMethod(newSM_name, newSM_parameters, VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = new LocalGenerator(b);

		Local originActivityParameterLocal = lg.generateLocal(serviceConnection.getType());
		b.getUnits().add(Jimple.v().newIdentityStmt(originActivityParameterLocal,
				Jimple.v().newParameterRef(serviceConnection.getType(), 0)));

		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 1)));

		// call dummy main method
		Local componentLocal = lg.generateLocal(destComp.getType());
		ServiceEntryPointInfo entryPointInfo = (ServiceEntryPointInfo) componentToEntryPoint.get(destComp);
		{
			SootMethod targetDummyMain = entryPointInfo.getEntryPoint();
			if (targetDummyMain == null)
				throw new RuntimeException(
						String.format("Destination component %s has no dummy main method", destComp.getName()));
			b.getUnits().add(Jimple.v().newAssignStmt(componentLocal, Jimple.v()
					.newStaticInvokeExpr(targetDummyMain.makeRef(), Collections.singletonList(intentParameterLocal))));

		}

		// get IBinder
		Local ibinderLocal = lg.generateLocal(IBINDER_TYPE);
		b.getUnits().add(Jimple.v().newAssignStmt(ibinderLocal,
				Jimple.v().newInstanceFieldRef(componentLocal, entryPointInfo.getBinderField().makeRef())));

		// anonymous inner class problem, cannot get correct stmt
		List<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("android.content.ComponentName"));
		paramTypes.add(RefType.v("android.os.IBinder"));
		SootMethod method = serviceConnection.getMethod("onServiceConnected", paramTypes);

		Local iLocal1 = lg.generateLocal(NullType.v());
		b.getUnits().add(Jimple.v().newAssignStmt(iLocal1, NullConstant.v()));

		List<Value> args = new ArrayList<Value>();
		args.add(iLocal1);
		args.add(ibinderLocal);
		SootClass sc = Scene.v().getSootClass(originActivityParameterLocal.getType().toString());
		InvokeExpr invoke;
		if (sc.isInterface()) {
			invoke = Jimple.v().newInterfaceInvokeExpr(originActivityParameterLocal, method.makeRef(), args);
		} else {
			invoke = Jimple.v().newVirtualInvokeExpr(originActivityParameterLocal, method.makeRef(), args);
		}
		b.getUnits().add(Jimple.v().newInvokeStmt(invoke));

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;
	}

	protected SootMethod generateRedirectMethodForContentProvider(Stmt iccStmt, SootClass destProvider) {
		SootMethod iccMethod = iccStmt.getInvokeExpr().getMethod();
		String newSM_name = "redirector" + num++;
		SootMethod newSM = new SootMethod(newSM_name, iccMethod.getParameterTypes(), iccMethod.getReturnType(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = new LocalGenerator(b);

		// all parameters
		List<Local> locals = new ArrayList<>();
		for (int i = 0; i < iccMethod.getParameterCount(); i++) {
			Type type = iccMethod.getParameterType(i);
			Local local = lg.generateLocal(type);
			locals.add(local);
			b.getUnits().add(Jimple.v().newIdentityStmt(local, Jimple.v().newParameterRef(type, i)));
		}

		// new
		Local al = lg.generateLocal(destProvider.getType());
		b.getUnits().add(Jimple.v().newAssignStmt(al, Jimple.v().newNewExpr(destProvider.getType())));

		// init
		List<Type> parameters = new ArrayList<Type>();
		List<Value> args = new ArrayList<Value>();
		SootMethod method = destProvider.getMethod("<init>", parameters, VoidType.v());
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(al, method.makeRef(), args)));

		Local rtLocal = lg.generateLocal(iccMethod.getReturnType());

		// call related method and assign the result to return local, may
		// optimize it to dummyMain method as well
		parameters = iccMethod.getParameterTypes();
		method = destProvider.getMethodByName(iccMethod.getName());
		InvokeExpr invoke = Jimple.v().newVirtualInvokeExpr(al, method.makeRef(), locals);
		b.getUnits().add(Jimple.v().newAssignStmt(rtLocal, invoke));

		// return statement
		b.getUnits().add(Jimple.v().newReturnStmt(rtLocal));
		return newSM;

	}
}
