package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.IntType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.util.Chain;

public class MessengerInstrumenter implements PreAnalysisHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String CLASS_MESSENGER = "android.os.Messenger";
	public static final String CLASS_MESSAGE = "android.os.Message";

	@Override
	public void onBeforeCallgraphConstruction() {
		logger.info("Launching Messenger Transformer...");

		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		for (Iterator<SootClass> iter = applicationClasses.snapshotIterator(); iter.hasNext();) {
			SootClass sootClass = iter.next();

			// We copy the list of methods to emulate a snapshot iterator which
			// doesn't exist for methods in Soot
			List<SootMethod> methodCopyList = new ArrayList<>(sootClass.getMethods());
			for (SootMethod sootMethod : methodCopyList) {
				if (sootMethod.isConcrete()) {
					Body body = sootMethod.retrieveActiveBody();
					Map<Stmt, List<Unit>> stmt2InjectedStmtList = new HashMap<Stmt, List<Unit>>();

					if (body.toString().contains(CLASS_MESSENGER) || body.toString().contains(CLASS_MESSAGE)) {
						for (Iterator<Unit> unitIter = body.getUnits().snapshotIterator(); unitIter.hasNext();) {
							Stmt stmt = (Stmt) unitIter.next();

							if (stmt.containsInvokeExpr()) {
								String methodSignature = stmt.getInvokeExpr().getMethod().getSignature().toString();

								LocalGenerator lg = new LocalGenerator(body);

								if (methodSignature.startsWith("<android.os.Messenger:")) {
									List<Unit> injectedStmts = new ArrayList<Unit>();

									Set<SootClass> handlers = MessageHandler.v().getAllHandlers();
									for (SootClass handler : handlers) {
										Local handlerLocal = lg.generateLocal(handler.getType());

										Unit newU = Jimple.v().newAssignStmt(handlerLocal,
												Jimple.v().newNewExpr(handler.getType()));
										injectedStmts.add(newU);

										SootMethod initMethod = handler.getMethodByName("<init>");
										Unit initU = Jimple.v().newInvokeStmt(
												Jimple.v().newVirtualInvokeExpr(handlerLocal, initMethod.makeRef()));
										injectedStmts.add(initU);

										SootMethod hmMethod = handler.getMethodByName("handleMessage");
										Unit callHMU = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
												handlerLocal, hmMethod.makeRef(), stmt.getInvokeExpr().getArg(0)));
										injectedStmts.add(callHMU);
									}

									put(stmt2InjectedStmtList, stmt, injectedStmts);
								}

								if (methodSignature.startsWith("<android.os.Message:")) {
									// Message related instrumentation
									SootClass messageCls = Scene.v().getSootClass("android.os.Message");
									SootField whatField = messageCls.getFieldByName("what");
									SootField arg1Field = messageCls.getFieldByName("arg1");
									SootField arg2Field = messageCls.getFieldByName("arg2");
									SootField objField = messageCls.getFieldByName("obj");

									// handler, what, arg1, arg2, obj
									if (methodSignature.equals(
											"<android.os.Message: android.os.Message obtain(android.os.Handler,int,int,int,java.lang.Object)>")) {
										if (stmt instanceof AssignStmt) {
											List<Unit> injectedStmts = new ArrayList<Unit>();

											AssignStmt assignStmt = (AssignStmt) stmt;
											Value messageObj = assignStmt.getLeftOp();

											Value whatValue = stmt.getInvokeExpr().getArg(1);
											Unit assignWhatU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, whatField.makeRef()),
													whatValue);
											injectedStmts.add(assignWhatU);

											Value arg1Value = stmt.getInvokeExpr().getArg(2);
											Unit assignArg1U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg1Field.makeRef()),
													arg1Value);
											injectedStmts.add(assignArg1U);

											Value arg2Value = stmt.getInvokeExpr().getArg(3);
											Unit assignArg2U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg2Field.makeRef()),
													arg2Value);
											injectedStmts.add(assignArg2U);

											Value objValue = stmt.getInvokeExpr().getArg(4);
											Unit assignObjU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, objField.makeRef()),
													objValue);
											injectedStmts.add(assignObjU);

											put(stmt2InjectedStmtList, stmt, injectedStmts);
										}
									}
									// handler, what, arg1, arg2
									else if (methodSignature.equals(
											"<android.os.Message: android.os.Message obtain(android.os.Handler,int,int,int)>")) {
										if (stmt instanceof AssignStmt) {
											List<Unit> injectedStmts = new ArrayList<Unit>();

											AssignStmt assignStmt = (AssignStmt) stmt;
											Value messageObj = assignStmt.getLeftOp();

											Value whatValue = stmt.getInvokeExpr().getArg(1);
											Unit assignWhatU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, whatField.makeRef()),
													whatValue);
											injectedStmts.add(assignWhatU);

											Value arg1Value = stmt.getInvokeExpr().getArg(2);
											Unit assignArg1U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg1Field.makeRef()),
													arg1Value);
											injectedStmts.add(assignArg1U);

											Value arg2Value = stmt.getInvokeExpr().getArg(3);
											Unit assignArg2U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg2Field.makeRef()),
													arg2Value);
											injectedStmts.add(assignArg2U);

											put(stmt2InjectedStmtList, stmt, injectedStmts);
										}

									} else if (methodSignature.equals(
											"<android.os.Message: android.os.Message obtain(android.os.Handler,int,java.lang.Object)>")) {
										if (stmt instanceof AssignStmt) {
											List<Unit> injectedStmts = new ArrayList<Unit>();

											AssignStmt assignStmt = (AssignStmt) stmt;
											Value messageObj = assignStmt.getLeftOp();

											Value whatValue = stmt.getInvokeExpr().getArg(1);
											Unit assignWhatU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, whatField.makeRef()),
													whatValue);
											injectedStmts.add(assignWhatU);

											Value objValue = stmt.getInvokeExpr().getArg(2);
											Unit assignObjU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, objField.makeRef()),
													objValue);
											injectedStmts.add(assignObjU);

											put(stmt2InjectedStmtList, stmt, injectedStmts);
										}
									} else if (methodSignature.equals(
											"<android.os.Message: android.os.Message obtain(android.os.Handler,int)>")) {
										if (stmt instanceof AssignStmt) {
											List<Unit> injectedStmts = new ArrayList<Unit>();

											AssignStmt assignStmt = (AssignStmt) stmt;
											Value messageObj = assignStmt.getLeftOp();

											Value whatValue = stmt.getInvokeExpr().getArg(1);
											Unit assignWhatU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, whatField.makeRef()),
													whatValue);
											injectedStmts.add(assignWhatU);

											put(stmt2InjectedStmtList, stmt, injectedStmts);
										}
									} else if (methodSignature.equals(
											"<android.os.Message: android.os.Message obtain(android.os.Message)>")) {
										if (stmt instanceof AssignStmt) {
											List<Unit> injectedStmts = new ArrayList<Unit>();

											Value messageParam = stmt.getInvokeExpr().getArg(0);

											AssignStmt assignStmt = (AssignStmt) stmt;
											Value messageObj = assignStmt.getLeftOp();

											// copy what
											Local whatValue = lg.generateLocal(IntType.v());
											Unit assignWhatU = Jimple.v().newAssignStmt(whatValue,
													Jimple.v().newInstanceFieldRef(messageParam, whatField.makeRef()));
											injectedStmts.add(assignWhatU);

											Unit assignBackWhatU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, whatField.makeRef()),
													whatValue);
											injectedStmts.add(assignBackWhatU);

											// copy arg1
											Local arg1Value = lg.generateLocal(IntType.v());
											Unit assignArg1U = Jimple.v().newAssignStmt(arg1Value,
													Jimple.v().newInstanceFieldRef(messageParam, arg1Field.makeRef()));
											injectedStmts.add(assignArg1U);

											Unit assignBackArg1U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg1Field.makeRef()),
													arg1Value);
											injectedStmts.add(assignBackArg1U);

											// copy arg2
											Local arg2Value = lg.generateLocal(IntType.v());
											Unit assignArg2U = Jimple.v().newAssignStmt(arg2Value,
													Jimple.v().newInstanceFieldRef(messageParam, arg2Field.makeRef()));
											injectedStmts.add(assignArg2U);

											Unit assignBackArg2U = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, arg2Field.makeRef()),
													arg2Value);
											injectedStmts.add(assignBackArg2U);

											// copy obj
											Local objValue = lg.generateLocal(RefType.v("java.lang.Object"));
											Unit assignObjU = Jimple.v().newAssignStmt(objValue,
													Jimple.v().newInstanceFieldRef(messageParam, objField.makeRef()));
											injectedStmts.add(assignObjU);

											Unit assignBackObjU = Jimple.v().newAssignStmt(
													Jimple.v().newInstanceFieldRef(messageObj, objField.makeRef()),
													objValue);
											injectedStmts.add(assignBackObjU);

											put(stmt2InjectedStmtList, stmt, injectedStmts);
										}
									}
								}
							}

						}
					}

					for (Map.Entry<Stmt, List<Unit>> entry : stmt2InjectedStmtList.entrySet()) {
						for (int i = entry.getValue().size() - 1; i >= 0; i--) {
							body.getUnits().insertAfter(entry.getValue().get(i), entry.getKey());
						}
					}
					body.validate();
				}

			}
		}

	}

	@Override
	public void onAfterCallgraphConstruction() {
	}

	public void put(Map<Stmt, List<Unit>> stmt2InjectedStmtList, Stmt stmt, List<Unit> injectedStmts) {
		if (stmt2InjectedStmtList.containsKey(stmt)) {
			List<Unit> units = stmt2InjectedStmtList.get(stmt);
			units.addAll(injectedStmts);

			stmt2InjectedStmtList.put(stmt, units);
		} else {
			stmt2InjectedStmtList.put(stmt, injectedStmts);
		}
	}
}
