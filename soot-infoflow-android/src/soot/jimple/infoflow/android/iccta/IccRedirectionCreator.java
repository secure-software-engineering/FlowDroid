package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.LocalGenerator;
import soot.Modifier;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.entryPointCreators.components.ActivityEntryPointInfo;
import soot.jimple.infoflow.android.entryPointCreators.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.android.entryPointCreators.components.ServiceEntryPointInfo;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.tagkit.Tag;
import soot.util.HashMultiMap;
import soot.util.MultiMap;
import soot.util.NumberedString;

public class IccRedirectionCreator {

	/**
	 * Interface to notify external code that a new invocation statement for a
	 * redirector method has been inserted
	 * 
	 * @author Steven Arzt
	 *
	 */
	public interface IRedirectorCallInserted {

		/**
		 * Method that is called when a new invocation to a redirector statement has
		 * been inserted
		 * 
		 * @param link             The inter-component link for which a statement has
		 *                         been injected
		 * @param callStmt         The statement that has been injected
		 * @param redirectorMethod The redirector method that is being called
		 */
		public void onRedirectorCallInserted(IccLink link, Stmt callStmt, SootMethod redirectorMethod);

	}

	private static int num = 0;

	private final static Logger logger = LoggerFactory.getLogger(IccRedirectionCreator.class);

	private final RefType INTENT_TYPE = RefType.v("android.content.Intent");
	private final RefType IBINDER_TYPE = RefType.v("android.os.IBinder");

	protected final Map<String, SootMethod> source2RedirectMethod = new HashMap<>();
	protected final MultiMap<Body, Unit> instrumentedUnits = new HashMultiMap<>();

	protected final SootClass dummyMainClass;
	protected final ComponentEntryPointCollection componentToEntryPoint;
	protected final NumberedString subsigStartActivityForResult;

	protected IRedirectorCallInserted instrumentationCallback = null;

	public IccRedirectionCreator(SootClass dummyMainClass, ComponentEntryPointCollection componentToEntryPoint) {
		this.componentToEntryPoint = componentToEntryPoint;
		this.dummyMainClass = dummyMainClass;

		subsigStartActivityForResult = Scene.v().getSubSigNumberer()
				.findOrAdd("void startActivityForResult(android.content.Intent,int)");
	}

	public void redirectToDestination(IccLink link) {
		if (link.getDestinationC().isPhantom())
			return;

		// Do not instrument code into system methods
		if (SystemClassHandler.v().isClassInSystemPackage(link.getFromSM().getDeclaringClass()))
			return;

		// 1) generate redirect method
		SootMethod redirectSM = getRedirectMethod(link);
		if (redirectSM == null)
			return;

		// 2) instrument the source to call the generated redirect method after
		// ICC methods
		insertRedirectMethodCallAfterIccMethod(link, redirectSM);
	}

	/**
	 * Redirect ICC call at unit in sm to the right component
	 * 
	 * @param link
	 * @return
	 */
	protected SootMethod getRedirectMethod(IccLink link) {
		// If the target component is, e.g., disabled in the manifest, we do not
		// have an
		// entry point for it
		SootClass instrumentedDestinationSC = link.getDestinationC();
		if (!componentToEntryPoint.hasEntryPointForComponent(instrumentedDestinationSC))
			return null;

		// Get the redirection method and create it if it doesn't exist
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
							if (redirectMethod == null)
								return null;
						}
					}
				} else if (stmt.getInvokeExpr().getMethod().getName().equals("bindService")) {
					Value v = stmt.getInvokeExpr().getArg(1);
					if (v.getType() instanceof RefType) {
						RefType rt = (RefType) v.getType();
						redirectMethod = generateRedirectMethodForBindService(rt.getSootClass(),
								instrumentedDestinationSC);
						if (redirectMethod == null)
							return null;
					}
				} else {
					redirectMethod = generateRedirectMethod(instrumentedDestinationSC);
					if (redirectMethod == null)
						return null;
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

		SootMethod newSM = Scene.v().makeSootMethod(newSM_name, newSM_parameters, VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		dummyMainClass.addMethod(newSM);
		final JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);
		newSM.addTag(SimulatedCodeElementTag.TAG);

		LocalGenerator lg = Scene.v().createLocalGenerator(b);

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
		SootMethod targetDummyMain = componentToEntryPoint.getEntryPoint(wrapper);
		if (targetDummyMain == null) {
			logger.warn("Destination component {} has no dummy main method", wrapper.getName());
			return null;
		}

		String newSM_name = "redirector" + num++;
		SootMethod newSM = Scene.v().makeSootMethod(newSM_name, Collections.<Type>singletonList(INTENT_TYPE),
				VoidType.v(), Modifier.STATIC | Modifier.PUBLIC);
		newSM.addTag(SimulatedCodeElementTag.TAG);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = Scene.v().createLocalGenerator(b);

		// identity
		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 0)));

		// call dummyMainMethod
		{
			b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(targetDummyMain.makeRef(),
					Collections.singletonList(intentParameterLocal))));

		}

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;

	}

	protected SootMethod generateRedirectMethodForStartActivity(SootClass wrapper) {
		SootMethod targetDummyMain = componentToEntryPoint.getEntryPoint(wrapper);
		if (targetDummyMain == null) {
			logger.warn("Destination component {} has no dummy main method", wrapper.getName());
			return null;
		}
		String newSM_name = "redirector" + num++;
		SootMethod newSM = Scene.v().makeSootMethod(newSM_name, Collections.<Type>singletonList(INTENT_TYPE),
				VoidType.v(), Modifier.STATIC | Modifier.PUBLIC);
		newSM.addTag(SimulatedCodeElementTag.TAG);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = Scene.v().createLocalGenerator(b);

		// identity
		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 0)));

		// call dummyMainMethod
		{
			b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(targetDummyMain.makeRef(),
					Collections.singletonList(intentParameterLocal))));

		}

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
		return newSM;

	}

	protected SootMethod generateRedirectMethodForBindService(SootClass serviceConnection, SootClass destComp) {
		ServiceEntryPointInfo entryPointInfo = (ServiceEntryPointInfo) componentToEntryPoint.get(destComp);
		if (entryPointInfo == null) {
			logger.warn("Destination component {} has no dummy main method", destComp.getName());
			return null;
		}
		SootMethod targetDummyMain = entryPointInfo.getEntryPoint();
		if (targetDummyMain == null) {
			logger.warn("Destination component {} has no dummy main method", destComp.getName());
			return null;
		}
		String newSM_name = "redirector" + num++;

		List<Type> newSM_parameters = new ArrayList<>();
		newSM_parameters.add(serviceConnection.getType());
		newSM_parameters.add(INTENT_TYPE);

		SootMethod newSM = Scene.v().makeSootMethod(newSM_name, newSM_parameters, VoidType.v(),
				Modifier.STATIC | Modifier.PUBLIC);
		newSM.addTag(SimulatedCodeElementTag.TAG);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = Scene.v().createLocalGenerator(b);

		Local originActivityParameterLocal = lg.generateLocal(serviceConnection.getType());
		b.getUnits().add(Jimple.v().newIdentityStmt(originActivityParameterLocal,
				Jimple.v().newParameterRef(serviceConnection.getType(), 0)));

		Local intentParameterLocal = lg.generateLocal(INTENT_TYPE);
		b.getUnits().add(Jimple.v().newIdentityStmt(intentParameterLocal, Jimple.v().newParameterRef(INTENT_TYPE, 1)));

		// call dummy main method
		Local componentLocal = lg.generateLocal(destComp.getType());
		{
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

		Local iLocal1 = lg.generateLocal(RefType.v("android.content.ComponentName"));
		b.getUnits().add(Jimple.v().newAssignStmt(iLocal1, NullConstant.v()));

		List<Value> args = new ArrayList<>();
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

	protected SootMethod generateRedirectMethodForContentProvider(Stmt iccStmt, SootMethod destCPMethod) {
		final SootClass destCPClass = destCPMethod.getDeclaringClass();
		SootMethod iccMethod = iccStmt.getInvokeExpr().getMethod();
		String newSM_name = "redirector" + num++;
		SootMethod newSM = Scene.v().makeSootMethod(newSM_name, iccMethod.getParameterTypes(),
				iccMethod.getReturnType(), Modifier.STATIC | Modifier.PUBLIC);
		newSM.addTag(SimulatedCodeElementTag.TAG);
		dummyMainClass.addMethod(newSM);
		JimpleBody b = Jimple.v().newBody(newSM);
		newSM.setActiveBody(b);

		LocalGenerator lg = Scene.v().createLocalGenerator(b);

		// all parameters
		List<Local> locals = new ArrayList<>();
		for (int i = 0; i < iccMethod.getParameterCount(); i++) {
			Type type = iccMethod.getParameterType(i);
			Local local = lg.generateLocal(type);
			locals.add(local);
			b.getUnits().add(Jimple.v().newIdentityStmt(local, Jimple.v().newParameterRef(type, i)));
		}

		// new
		Local al = lg.generateLocal(destCPClass.getType());
		b.getUnits().add(Jimple.v().newAssignStmt(al, Jimple.v().newNewExpr(destCPClass.getType())));

		// init
		List<Type> parameters = new ArrayList<Type>();
		List<Value> args = new ArrayList<Value>();
		SootMethod method = destCPClass.getMethod("<init>", parameters, VoidType.v());
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(al, method.makeRef(), args)));

		Local rtLocal = lg.generateLocal(iccMethod.getReturnType());

		// call related method and assign the result to return local, may
		// optimize it to dummyMain method as well
		parameters = iccMethod.getParameterTypes();
		method = destCPMethod;
		InvokeExpr invoke = Jimple.v().newVirtualInvokeExpr(al, method.makeRef(), locals);
		b.getUnits().add(Jimple.v().newAssignStmt(rtLocal, invoke));

		// return statement
		b.getUnits().add(Jimple.v().newReturnStmt(rtLocal));
		return newSM;

	}

	/**
	 * We have the intent in a register at this point, create a new statement to
	 * call the static method with the intent as parameter
	 * 
	 * @param link
	 * @param redirectMethod
	 */
	protected void insertRedirectMethodCallAfterIccMethod(IccLink link, SootMethod redirectMethod) {
		Stmt fromStmt = (Stmt) link.getFromU();
		if (fromStmt == null || !fromStmt.containsInvokeExpr())
			return;

		SootMethod callee = fromStmt.getInvokeExpr().getMethod();

		// specially deal with startActivityForResult since they have two
		// parameters
		List<Value> args = new ArrayList<>();
		if (callee.getNumberedSubSignature().equals(subsigStartActivityForResult)) {
			InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) fromStmt.getInvokeExpr();
			args.add(iiexpr.getBase());
			args.add(iiexpr.getArg(0));
		} else if (fromStmt.toString().contains("bindService")) {
			Value arg0 = fromStmt.getInvokeExpr().getArg(0); // intent
			Value arg1 = fromStmt.getInvokeExpr().getArg(1); // serviceConnection
			args.add(arg1);
			args.add(arg0);
		} else {
			// specially deal with ICC methods with no parameter, i.e., PendingIntent.send()
			if (fromStmt.getInvokeExpr().getArgCount() == 0) {
				return;
			}
			Value arg0 = fromStmt.getInvokeExpr().getArg(0);
			args.add(arg0);
		}

		if (redirectMethod == null) {
			return;
		}

		final Body body = addICCRedirectCall(link, redirectMethod, args);

		final PatchingChain<Unit> units = body.getUnits();
		// remove the real ICC methods call stmt
		// link.getFromSM().retrieveActiveBody().getUnits().remove(link.getFromU());
		// Please refer to AndroidIPCManager.postProcess() for this removing
		// process.

		NumberedString subsig = Scene.v().getSubSigNumberer()
				.find("android.content.Intent createChooser(android.content.Intent,java.lang.CharSequence)");
		SootClass clazz = Scene.v().getSootClassUnsafe("android.content.Intent");
		if (subsig != null && clazz != null) {
			// especially for createChooser method
			for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
				Stmt stmt = (Stmt) iter.next();
				if (stmt.containsInvokeExpr()) {
					InvokeExpr expr = stmt.getInvokeExpr();
					SootMethodRef mr = expr.getMethodRef();
					if (mr.getDeclaringClass().equals(clazz) && mr.getSubSignature().equals(subsig)) {
						List<ValueBox> vbs = stmt.getUseAndDefBoxes();
						Unit assignU = Jimple.v().newAssignStmt(vbs.get(0).getValue(), vbs.get(1).getValue());
						copyTags(stmt, assignU);
						assignU.addTag(SimulatedCodeElementTag.TAG);
						units.insertAfter(assignU, stmt);
						instrumentedUnits.put(body, assignU);
					}
				}
			}
		}
	}

	protected Body addICCRedirectCall(IccLink link, SootMethod redirectMethod, List<Value> args) {
		Stmt redirectCallU;
		Jimple jimp = Jimple.v();
		if (link.getFromU() instanceof AssignStmt)
			redirectCallU = jimp.newAssignStmt(((AssignStmt) link.getFromU()).getLeftOp(),
					Jimple.v().newStaticInvokeExpr(redirectMethod.makeRef(), args));
		else
			redirectCallU = jimp.newInvokeStmt(Jimple.v().newStaticInvokeExpr(redirectMethod.makeRef(), args));

		final Body body = link.getFromSM().retrieveActiveBody();

		copyTags(link.getFromU(), redirectCallU);
		redirectCallU.addTag(SimulatedCodeElementTag.TAG);
		body.getUnits().insertAfter(redirectCallU, link.getFromU());
		instrumentedUnits.put(body, redirectCallU);
		if (instrumentationCallback != null) {
			instrumentationCallback.onRedirectorCallInserted(link, redirectCallU, redirectMethod);
		}
		return body;
	}

	/**
	 * Copy all the tags of {from} to {to}, if {to} already contain the copied tag,
	 * then overwrite it.
	 * 
	 * @param from
	 * @param to
	 */
	protected static void copyTags(Unit from, Unit to) {
		List<Tag> tags = from.getTags();

		for (Tag tag : tags) {
			to.removeTag(tag.getName()); // exception??
			to.addTag(tag);
		}
	}

	/**
	 * Removes all units that have previously been created by the instrumenter
	 */
	public void undoInstrumentation() {
		// Remove the redirection methods
		for (SootMethod sm : source2RedirectMethod.values())
			sm.getDeclaringClass().removeMethod(sm);

		for (Body body : instrumentedUnits.keySet()) {
			for (Unit u : instrumentedUnits.get(body)) {
				body.getUnits().remove(u);
			}
		}
		instrumentedUnits.clear();
		source2RedirectMethod.clear();
	}

	/**
	 * Sets the callback that shall be notified when a new statement has been
	 * injected to model inter-component call relationships
	 * 
	 * @param instrumentationCallback The callback to notify of new instrumentation
	 *                                statements
	 */
	public void setInstrumentationCallback(IRedirectorCallInserted instrumentationCallback) {
		this.instrumentationCallback = instrumentationCallback;
	}

}
