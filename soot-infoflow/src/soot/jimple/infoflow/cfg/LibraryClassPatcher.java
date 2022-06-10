package soot.jimple.infoflow.cfg;

import java.util.Collections;

import soot.Body;
import soot.IntType;
import soot.Local;
import soot.LocalGenerator;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Class for patching OS libraries such as java.lang.Thread so that we get
 * complete callgraphs
 * 
 * @author Steven Arzt
 *
 */
public class LibraryClassPatcher {

	/**
	 * Helper interface to inject code into the Message.obtain() overloads
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static interface IMessageObtainCodeInjector {

		public void injectCode(Body body, Local messageLocal);

	}

	public LibraryClassPatcher() {

	}

	/**
	 * Patches all supported system libraries
	 */
	public void patchLibraries() {
		// Patch the android.os.Handler implementation
		patchHandlerImplementation();

		// Patch the java.lang.Thread implementation
		patchThreadImplementation();

		// Patch the android.app.Activity implementation (getApplication())
		patchActivityImplementation();

		// Patch the java.util.Timer implementation
		patchTimerImplementation();

		// Patch activity getFragmentManager()
		patchActivityGetFragmentManager();

		// Patch the various overloads of Message.obtain()
		patchMessageObtainImplementation();
	}

	/**
	 * Patches the implementations of the various overloads of Message.obtain()
	 */
	private void patchMessageObtainImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("android.os.Message");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES)
			return;
		sc.setLibraryClass();

		// We sometimes seem to be missing the constructor
		SootMethod smMessageConstructor = Scene.v().grabMethod("<android.os.Message: void <init>()>");
		if (smMessageConstructor == null)
			return;

		// Make sure that we have all fields
		SootField tmp = sc.getFieldUnsafe("int what");
		if (tmp == null) {
			tmp = Scene.v().makeSootField("what", IntType.v());
			sc.addField(tmp);
		}
		final SootField fldWhat = tmp;

		tmp = sc.getFieldUnsafe("int arg1");
		if (tmp == null) {
			tmp = Scene.v().makeSootField("arg1", IntType.v());
			sc.addField(tmp);
		}
		final SootField fldArg1 = tmp;

		tmp = sc.getFieldUnsafe("int arg2");
		if (tmp == null) {
			tmp = Scene.v().makeSootField("arg2", IntType.v());
			sc.addField(tmp);
		}
		final SootField fldArg2 = tmp;

		tmp = sc.getFieldUnsafe("java.lang.Object obj");
		if (tmp == null) {
			tmp = Scene.v().makeSootField("obj", Scene.v().getObjectType());
			sc.addField(tmp);
		}
		final SootField fldObj = tmp;

		final SystemClassHandler systemClassHandler = SystemClassHandler.v();

		// Method obtain(Handler,int)
		SootMethod smObtain1 = sc.getMethodUnsafe("android.os.Message obtain(android.os.Handler,int)");
		if (smObtain1 != null
				&& (!smObtain1.hasActiveBody() || systemClassHandler.isStubImplementation(smObtain1.getActiveBody()))) {
			generateMessageObtainMethod(smObtain1, new IMessageObtainCodeInjector() {

				@Override
				public void injectCode(Body body, Local messageLocal) {
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldWhat.makeRef()),
									body.getParameterLocal(1)));
				}

			});
		}

		// Method obtain(Handler,int,int,int,Object)
		SootMethod smObtain2 = sc
				.getMethodUnsafe("android.os.Message obtain(android.os.Handler,int,int,int,java.lang.Object)");
		if (smObtain2 != null
				&& (!smObtain2.hasActiveBody() || systemClassHandler.isStubImplementation(smObtain2.getActiveBody()))) {
			generateMessageObtainMethod(smObtain2, new IMessageObtainCodeInjector() {

				@Override
				public void injectCode(Body body, Local messageLocal) {
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldWhat.makeRef()),
									body.getParameterLocal(1)));
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldArg1.makeRef()),
									body.getParameterLocal(2)));
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldArg2.makeRef()),
									body.getParameterLocal(3)));
					body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(messageLocal, fldObj.makeRef()), body.getParameterLocal(4)));
				}

			});
		}

		// Method obtain(Handler,int,int,int)
		SootMethod smObtain3 = sc.getMethodUnsafe("android.os.Message obtain(android.os.Handler,int,int,int)");
		if (smObtain3 != null
				&& (!smObtain3.hasActiveBody() || systemClassHandler.isStubImplementation(smObtain3.getActiveBody()))) {
			generateMessageObtainMethod(smObtain3, new IMessageObtainCodeInjector() {

				@Override
				public void injectCode(Body body, Local messageLocal) {
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldWhat.makeRef()),
									body.getParameterLocal(1)));
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldArg1.makeRef()),
									body.getParameterLocal(2)));
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldArg2.makeRef()),
									body.getParameterLocal(3)));
				}

			});
		}

		// Method obtain(Handler,int,Object)
		SootMethod smObtain4 = sc.getMethodUnsafe("android.os.Message obtain(android.os.Handler,int,java.lang.Object)");
		if (smObtain4 != null
				&& (!smObtain4.hasActiveBody() || systemClassHandler.isStubImplementation(smObtain4.getActiveBody()))) {
			generateMessageObtainMethod(smObtain4, new IMessageObtainCodeInjector() {

				@Override
				public void injectCode(Body body, Local messageLocal) {
					body.getUnits()
							.add(Jimple.v().newAssignStmt(
									Jimple.v().newInstanceFieldRef(messageLocal, fldWhat.makeRef()),
									body.getParameterLocal(1)));
					body.getUnits().add(Jimple.v().newAssignStmt(
							Jimple.v().newInstanceFieldRef(messageLocal, fldObj.makeRef()), body.getParameterLocal(2)));
				}

			});
		}
	}

	/**
	 * Creates a new implementation for the given method
	 * 
	 * @param sm       The method for which to generate a new implementation
	 * @param injector Callback to inject additional code into the new method
	 */
	private void generateMessageObtainMethod(SootMethod sm, IMessageObtainCodeInjector injector) {
		// Create the method
		RefType tpMessage = RefType.v("android.os.Message");
		sm.getDeclaringClass().setLibraryClass();
		sm.setPhantom(false);
		sm.addTag(new FlowDroidEssentialMethodTag());

		// Create a body for the method
		JimpleBody body = Jimple.v().newBody(sm);
		sm.setActiveBody(body);
		body.insertIdentityStmts();

		// This method must exist, since we checked it before attempting any kind of
		// message mocking
		SootMethod smMessageConstructor = Scene.v().grabMethod("<android.os.Message: void <init>()>");

		LocalGenerator lg = Scene.v().createLocalGenerator(body);
		Local messageLocal = lg.generateLocal(tpMessage);
		body.getUnits().add(Jimple.v().newAssignStmt(messageLocal, Jimple.v().newNewExpr(tpMessage)));
		body.getUnits().add(Jimple.v()
				.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(messageLocal, smMessageConstructor.makeRef())));

		if (injector != null)
			injector.injectCode(body, messageLocal);

		body.getUnits().add(Jimple.v().newReturnStmt(messageLocal));
	}

	/**
	 * Patch android.app.Activity getApplication method in order to return the
	 * singleton Application instance created in the dummyMainMethod.
	 */
	private void patchActivityImplementation() {
		SootClass scApplicationHolder = createOrGetApplicationHolder();

		SootClass sc = Scene.v().getSootClassUnsafe("android.app.Activity");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES || scApplicationHolder == null)
			return;
		sc.setLibraryClass();

		SootMethod smRun = sc.getMethodUnsafe("android.app.Application getApplication()");
		if (smRun == null
				|| (smRun.hasActiveBody() && !SystemClassHandler.v().isStubImplementation(smRun.getActiveBody())))
			return;
		smRun.setPhantom(false);
		smRun.addTag(new FlowDroidEssentialMethodTag());

		Body b = Jimple.v().newBody(smRun);
		smRun.setActiveBody(b);

		// add "this" local
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		SootFieldRef appStaticFieldRef = scApplicationHolder.getFieldByName("application").makeRef();
		// creating local to store the mApplication field
		Local targetLocal = Jimple.v().newLocal("retApplication", appStaticFieldRef.type());
		b.getLocals().add(targetLocal);

		b.getUnits().add(Jimple.v().newAssignStmt(targetLocal, Jimple.v().newStaticFieldRef(appStaticFieldRef)));

		Unit retStmt = Jimple.v().newReturnStmt(targetLocal);
		b.getUnits().add(retStmt);
	}

	/**
	 * return the ApplicationHolder class (created if needed) This is a class with 1
	 * static field to save the application instance in
	 */
	public static SootClass createOrGetApplicationHolder() {
		SootClass scApplication = Scene.v().getSootClassUnsafe("android.app.Application");

		if (scApplication == null || scApplication.resolvingLevel() < SootClass.SIGNATURES)
			return null;
		String applicationHolderClassName = "il.ac.tau.MyApplicationHolder";
		SootClass scApplicationHolder;
		if (!Scene.v().containsClass(applicationHolderClassName)) {
			scApplicationHolder = Scene.v().makeSootClass(applicationHolderClassName, Modifier.PUBLIC);
			scApplicationHolder.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

			Scene.v().addClass(scApplicationHolder);
			scApplicationHolder.addField(
					Scene.v().makeSootField("application", scApplication.getType(), Modifier.PUBLIC | Modifier.STATIC));

			scApplicationHolder.validate();
		} else {
			scApplicationHolder = Scene.v().getSootClassUnsafe(applicationHolderClassName);
		}

		return scApplicationHolder;
	}

	/**
	 * Creates a synthetic minimal implementation of the java.lang.Thread class to
	 * model threading correctly even if we don't have a real implementation.
	 */
	private void patchThreadImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("java.lang.Thread");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES)
			return;
		sc.setLibraryClass();

		final SystemClassHandler systemClassHandler = SystemClassHandler.v();

		SootMethod smRun = sc.getMethodUnsafe("void run()");
		if (smRun == null || (smRun.hasActiveBody() && !systemClassHandler.isStubImplementation(smRun.getActiveBody())))
			return;
		smRun.addTag(new FlowDroidEssentialMethodTag());

		SootMethod smCons = sc.getMethodUnsafe("void <init>(java.lang.Runnable)");
		if (smCons == null
				|| (smCons.hasActiveBody() && !systemClassHandler.isStubImplementation(smCons.getActiveBody())))
			return;
		smCons.addTag(new FlowDroidEssentialMethodTag());

		SootClass runnable = Scene.v().getSootClassUnsafe("java.lang.Runnable");
		if (runnable == null || runnable.resolvingLevel() < SootClass.SIGNATURES)
			return;

		// Create a field for storing the runnable
		int fieldIdx = 0;
		SootField fldTarget = null;
		while ((fldTarget = sc.getFieldByNameUnsafe("target" + fieldIdx)) != null)
			fieldIdx++;
		fldTarget = Scene.v().makeSootField("target" + fieldIdx, runnable.getType());
		sc.addField(fldTarget);

		// Create a new constructor
		patchThreadConstructor(smCons, runnable, fldTarget);

		// Create a new Thread.start() method
		patchThreadRunMethod(smRun, runnable, fldTarget);
	}

	/**
	 * Creates a synthetic "java.lang.Thread.run()" method implementation that calls
	 * the target previously passed in when the constructor was called
	 * 
	 * @param smRun     The run() method for which to create a synthetic
	 *                  implementation
	 * @param runnable  The "java.lang.Runnable" interface
	 * @param fldTarget The field containing the target object
	 */
	private void patchThreadRunMethod(SootMethod smRun, SootClass runnable, SootField fldTarget) {
		SootClass sc = smRun.getDeclaringClass();
		sc.setLibraryClass();
		smRun.setPhantom(false);

		Body b = Jimple.v().newBody(smRun);
		smRun.setActiveBody(b);

		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		Local targetLocal = Jimple.v().newLocal("target", runnable.getType());
		b.getLocals().add(targetLocal);
		b.getUnits().add(
				Jimple.v().newAssignStmt(targetLocal, Jimple.v().newInstanceFieldRef(thisLocal, fldTarget.makeRef())));

		Unit retStmt = Jimple.v().newReturnVoidStmt();

		// If (this.target == null) return;
		b.getUnits().add(Jimple.v().newIfStmt(Jimple.v().newEqExpr(targetLocal, NullConstant.v()), retStmt));

		// Invoke target.run()
		b.getUnits().add(Jimple.v().newInvokeStmt(
				Jimple.v().newInterfaceInvokeExpr(targetLocal, runnable.getMethod("void run()").makeRef())));

		b.getUnits().add(retStmt);
	}

	/**
	 * Creates a synthetic "<init>(java.lang.Runnable)" method implementation that
	 * stores the given runnable into a field for later use
	 * 
	 * @param smCons    The <init>() method for which to create a synthetic
	 *                  implementation
	 * @param runnable  The "java.lang.Runnable" interface
	 * @param fldTarget The field receiving the Runnable
	 */
	private void patchThreadConstructor(SootMethod smCons, SootClass runnable, SootField fldTarget) {
		SootClass sc = smCons.getDeclaringClass();
		sc.setLibraryClass();
		smCons.setPhantom(false);

		Body b = Jimple.v().newBody(smCons);
		smCons.setActiveBody(b);

		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		Local param0Local = Jimple.v().newLocal("p0", runnable.getType());
		b.getLocals().add(param0Local);
		b.getUnits().add(Jimple.v().newIdentityStmt(param0Local, Jimple.v().newParameterRef(runnable.getType(), 0)));

		b.getUnits().add(
				Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal, fldTarget.makeRef()), param0Local));

		b.getUnits().add(Jimple.v().newReturnVoidStmt());
	}

	/**
	 * Creates a dummy implementation of android.os.Handler if we don't have one
	 */
	private void patchHandlerImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("android.os.Handler");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES)
			return;
		sc.setLibraryClass();

		SootClass runnable = Scene.v().getSootClassUnsafe("java.lang.Runnable");
		if (runnable == null || runnable.resolvingLevel() < SootClass.SIGNATURES)
			return;

		SootMethod smPost = sc.getMethodUnsafe("boolean post(java.lang.Runnable)");
		SootMethod smPostAtFrontOfQueue = sc.getMethodUnsafe("boolean postAtFrontOfQueue(java.lang.Runnable)");
		SootMethod smPostAtTimeWithToken = sc
				.getMethodUnsafe("boolean postAtTime(java.lang.Runnable,java.lang.Object,long)");
		SootMethod smPostAtTime = sc.getMethodUnsafe("boolean postAtTime(java.lang.Runnable,long)");
		SootMethod smPostDelayed = sc.getMethodUnsafe("boolean postDelayed(java.lang.Runnable,long)");
		SootMethod smDispatchMessage = sc.getMethodUnsafe("void dispatchMessage(android.os.Message)");

		final SystemClassHandler systemClassHandler = SystemClassHandler.v();

		if (smPost != null
				&& (!smPost.hasActiveBody() || systemClassHandler.isStubImplementation(smPost.getActiveBody()))) {
			patchHandlerPostBody(smPost, runnable);
			smPost.addTag(new FlowDroidEssentialMethodTag());
		}

		if (smPostAtFrontOfQueue != null && (!smPostAtFrontOfQueue.hasActiveBody()
				|| systemClassHandler.isStubImplementation(smPostAtFrontOfQueue.getActiveBody()))) {
			patchHandlerPostBody(smPostAtFrontOfQueue, runnable);
			smPostAtFrontOfQueue.addTag(new FlowDroidEssentialMethodTag());
		}

		if (smPostAtTime != null && (!smPostAtTime.hasActiveBody()
				|| systemClassHandler.isStubImplementation(smPostAtTime.getActiveBody()))) {
			patchHandlerPostBody(smPostAtTime, runnable);
			smPostAtTime.addTag(new FlowDroidEssentialMethodTag());
		}

		if (smPostAtTimeWithToken != null && (!smPostAtTimeWithToken.hasActiveBody()
				|| systemClassHandler.isStubImplementation(smPostAtTimeWithToken.getActiveBody()))) {
			patchHandlerPostBody(smPostAtTimeWithToken, runnable);
			smPostAtTimeWithToken.addTag(new FlowDroidEssentialMethodTag());
		}

		if (smPostDelayed != null && (!smPostDelayed.hasActiveBody()
				|| systemClassHandler.isStubImplementation(smPostDelayed.getActiveBody()))) {
			patchHandlerPostBody(smPostDelayed, runnable);
			smPostDelayed.addTag(new FlowDroidEssentialMethodTag());
		}

		if (smDispatchMessage != null && (!smDispatchMessage.hasActiveBody()
				|| systemClassHandler.isStubImplementation(smDispatchMessage.getActiveBody()))) {
			patchHandlerDispatchBody(smDispatchMessage);
			smDispatchMessage.addTag(new FlowDroidEssentialMethodTag());
		}
	}

	/**
	 * Creates a new body for one of the dispatchMessage method in
	 * android.os.Handler
	 * 
	 * @param method The method for which to create the implementation
	 *               (dispatchMessage)
	 * @return The newly created body
	 */
	private Body patchHandlerDispatchBody(SootMethod method) {
		SootClass sc = method.getDeclaringClass();
		sc.setLibraryClass();
		method.setPhantom(false);

		Body b = Jimple.v().newBody(method);
		method.setActiveBody(b);

		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		// Assign the parameters
		Local firstParam = null;
		for (int i = 0; i < method.getParameterCount(); ++i) {
			Local paramLocal = Jimple.v().newLocal("param" + i, method.getParameterType(i));
			b.getLocals().add(paramLocal);
			b.getUnits().add(
					Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(method.getParameterType(i), i)));
			if (i == 0)
				firstParam = paramLocal;
		}

		// Invoke handler.handleMessage(Message)
		b.getUnits()
				.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(thisLocal,
						Scene.v().makeMethodRef(sc, "handleMessage",
								Collections.<Type>singletonList(method.getParameterType(0)), VoidType.v(), false),
						firstParam)));

		Unit retStmt = Jimple.v().newReturnVoidStmt();
		b.getUnits().add(retStmt);
		return b;
	}

	/**
	 * Creates a new body for one of the postXXX methods in android.os.Handler
	 * 
	 * @param method   The method for which to create the implementation
	 * @param runnable The java.lang.Runnable class
	 * @return The newly created body
	 */
	private Body patchHandlerPostBody(SootMethod method, SootClass runnable) {
		SootClass sc = method.getDeclaringClass();
		sc.setLibraryClass();
		method.setPhantom(false);

		Body b = Jimple.v().newBody(method);
		method.setActiveBody(b);

		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		// Assign the parameters
		Local firstParam = null;
		for (int i = 0; i < method.getParameterCount(); i++) {
			Local paramLocal = Jimple.v().newLocal("param" + i, method.getParameterType(i));
			b.getLocals().add(paramLocal);
			b.getUnits().add(
					Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(method.getParameterType(i), i)));
			if (i == 0)
				firstParam = paramLocal;
		}

		// Invoke p0.run()
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(firstParam,
				Scene.v().makeMethodRef(runnable, "run", Collections.<Type>emptyList(), VoidType.v(), false))));

		Unit retStmt = Jimple.v().newReturnStmt(IntConstant.v(1));
		b.getUnits().add(retStmt);
		return b;
	}

	/**
	 * Creates a dummy implementation of java.util.Timer if we don't have one
	 */
	private void patchTimerImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("java.util.Timer");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES)
			return;
		sc.setLibraryClass();

		SootMethod smSchedule1 = sc.getMethodUnsafe("void schedule(java.util.TimerTask,long)");
		if (smSchedule1 != null && !smSchedule1.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule1);
			smSchedule1.addTag(new FlowDroidEssentialMethodTag());
		}

		SootMethod smSchedule2 = sc.getMethodUnsafe("void schedule(java.util.TimerTask,java.util.Date)");
		if (smSchedule2 != null && !smSchedule2.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule2);
			smSchedule2.addTag(new FlowDroidEssentialMethodTag());
		}

		SootMethod smSchedule3 = sc.getMethodUnsafe("void schedule(java.util.TimerTask,java.util.Date,long)");
		if (smSchedule3 != null && !smSchedule3.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule3);
			smSchedule3.addTag(new FlowDroidEssentialMethodTag());
		}

		SootMethod smSchedule4 = sc.getMethodUnsafe("void schedule(java.util.TimerTask,long,long)");
		if (smSchedule4 != null && !smSchedule4.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule4);
			smSchedule4.addTag(new FlowDroidEssentialMethodTag());
		}

		SootMethod smSchedule5 = sc
				.getMethodUnsafe("void scheduleAtFixedRate(java.util.TimerTask,java.util.Date,long)");
		if (smSchedule5 != null && !smSchedule5.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule5);
			smSchedule5.addTag(new FlowDroidEssentialMethodTag());
		}

		SootMethod smSchedule6 = sc.getMethodUnsafe("void scheduleAtFixedRate(java.util.TimerTask,long,long)");
		if (smSchedule6 != null && !smSchedule6.hasActiveBody()) {
			patchTimerScheduleMethod(smSchedule6);
			smSchedule6.addTag(new FlowDroidEssentialMethodTag());
		}
	}

	/**
	 * Patches the schedule() method of java.util.Timer by providing a fake
	 * implementation
	 * 
	 * @param method The method to patch
	 */
	private void patchTimerScheduleMethod(SootMethod method) {
		SootClass sc = method.getDeclaringClass();
		sc.setLibraryClass();
		method.setPhantom(false);

		Body b = Jimple.v().newBody(method);
		method.setActiveBody(b);

		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		// Assign the parameters
		Local firstParam = null;
		for (int i = 0; i < method.getParameterCount(); i++) {
			Local paramLocal = Jimple.v().newLocal("param" + i, method.getParameterType(i));
			b.getLocals().add(paramLocal);
			b.getUnits().add(
					Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(method.getParameterType(i), i)));
			if (i == 0)
				firstParam = paramLocal;
		}

		// Invoke the run() method on the first parameter local
		SootMethod runMethod = Scene.v().grabMethod("<java.util.TimerTask: void run()>");
		if (runMethod != null) {
			Stmt invokeStmt = Jimple.v()
					.newInvokeStmt(Jimple.v().newVirtualInvokeExpr(firstParam, runMethod.makeRef()));
			b.getUnits().add(invokeStmt);
		}

		// Add the return statement
		b.getUnits().add(Jimple.v().newReturnVoidStmt());
	}

	/**
	 * Modifies Activity::getFragmentManager() to return new
	 * MyFragmentManager(this);
	 */
	private void patchActivityGetFragmentManager() {
		SootClass sc = Scene.v().getSootClassUnsafe("android.app.Activity");
		if (sc == null || sc.resolvingLevel() < SootClass.SIGNATURES)
			return;
		sc.setLibraryClass();
		SootMethod smGetFM = sc.getMethodUnsafe("android.app.FragmentManager getFragmentManager()");
		if (smGetFM == null || smGetFM.hasActiveBody())
			return;
		Body b = Jimple.v().newBody(smGetFM);
		smGetFM.setActiveBody(b);
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sc.getType())));

		SootClass scFragmentTransaction = Scene.v().forceResolve("android.app.FragmentManager", SootClass.SIGNATURES);
		Local retLocal = Jimple.v().newLocal("retFragMan",
				Scene.v().getSootClassUnsafe("android.app.FragmentManager").getType());
		b.getLocals().add(retLocal);
		b.getUnits().add(Jimple.v().newAssignStmt(retLocal, Jimple.v().newNewExpr(scFragmentTransaction.getType())));
		b.getUnits().add(Jimple.v().newReturnStmt(retLocal));
	}

}
