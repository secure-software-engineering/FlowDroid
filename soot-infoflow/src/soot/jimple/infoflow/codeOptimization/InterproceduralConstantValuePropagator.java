package soot.jimple.infoflow.codeOptimization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.solver.Pair;
import soot.Body;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LocalGenerator;
import soot.LongType;
import soot.MethodOrMethodContext;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.dexpler.DalvikThrowAnalysis;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.scalar.ConditionalBranchFolder;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;
import soot.jimple.toolkits.scalar.DeadAssignmentEliminator;
import soot.jimple.toolkits.scalar.UnconditionalBranchFolder;
import soot.jimple.toolkits.scalar.UnreachableCodeEliminator;
import soot.options.Options;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.exceptions.ThrowableSet;
import soot.toolkits.exceptions.UnitThrowAnalysis;
import soot.toolkits.scalar.UnusedLocalEliminator;
import soot.util.queue.QueueReader;

public class InterproceduralConstantValuePropagator extends SceneTransformer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final InfoflowManager manager;
	private final Set<SootMethod> excludedMethods;
	private final ISourceSinkManager sourceSinkManager;
	private final ITaintPropagationWrapper taintWrapper;
	private boolean removeSideEffectFreeMethods = true;
	private boolean excludeSystemClasses = true;

	protected final Map<SootMethod, Boolean> methodSideEffects = new ConcurrentHashMap<>();
	protected final Map<SootMethod, Boolean> methodSinks = new ConcurrentHashMap<>();
	protected final Map<SootMethod, Boolean> methodFieldReads = new ConcurrentHashMap<>();

	protected SootClass exceptionClass = null;
	protected final Map<SootClass, SootMethod> exceptionThrowers = new HashMap<>();

	private final List<SootMethod> propagationWorklist = new ArrayList<>();
	private final Set<Pair<SootMethod, Integer>> propagatedParameters = new HashSet<>();

	/**
	 * Creates a new instance of the {@link InterproceduralConstantValuePropagator}
	 * class
	 * 
	 * @param manager The data flow manager for interacting with the solver
	 */
	public InterproceduralConstantValuePropagator(InfoflowManager manager) {
		this.manager = manager;
		this.excludedMethods = null;
		this.sourceSinkManager = null;
		this.taintWrapper = null;
	}

	/**
	 * Creates a new instance of the {@link InterproceduralConstantValuePropagator}
	 * class
	 * 
	 * @param manager           The data flow manager for interacting with the
	 *                          solver
	 * @param excludedMethods   The methods that shall be excluded. If one of these
	 *                          methods calls another method with a constant
	 *                          argument, this argument will not be propagated into
	 *                          the callee.
	 * @param sourceSinkManager The SourceSinkManager to be used for not propagating
	 *                          constants out of source methods
	 * @param taintWrapper      The taint wrapper to be used for not breaking dummy
	 *                          values that will later be replaced by artificial
	 *                          taints
	 */
	public InterproceduralConstantValuePropagator(InfoflowManager manager, Collection<SootMethod> excludedMethods,
			ISourceSinkManager sourceSinkManager, ITaintPropagationWrapper taintWrapper) {
		this.manager = manager;
		this.excludedMethods = new HashSet<>(excludedMethods);
		this.sourceSinkManager = sourceSinkManager;
		this.taintWrapper = taintWrapper;
	}

	/**
	 * Sets whether side-effect free methods that do not call sinks shall be removed
	 * 
	 * @param removeSideEffectFreeMethods The if side-effect free methods that do
	 *                                    not call sinks shall be removed, otherwise
	 *                                    false
	 */
	public void setRemoveSideEffectFreeMethods(boolean removeSideEffectFreeMethods) {
		this.removeSideEffectFreeMethods = removeSideEffectFreeMethods;
	}

	/**
	 * Sets whether methods in system classes shall be excluded from constraint
	 * propagation
	 * 
	 * @param excludeSystemClasses True if methods in system classes shall be
	 *                             excluded from constraint propagation, otherwise
	 *                             false
	 */
	public void setExcludeSystemClasses(boolean excludeSystemClasses) {
		this.excludeSystemClasses = excludeSystemClasses;
	}

	/**
	 * Checks whether optimizations are possible for the given method and, if so,
	 * adds it to the global worklist
	 * 
	 * @param sm The method to check and add to the worklist
	 */
	private void checkAndAddMethod(SootMethod sm) {
		if (sm == null || !sm.hasActiveBody())
			return;

		// If this callee is excluded, we do not propagate out of it
		if (excludedMethods != null && excludedMethods.contains(sm))
			return;
		if (excludeSystemClasses && SystemClassHandler.v().isClassInSystemPackage(sm.getDeclaringClass()))
			return;

		if (sm.getReturnType() != VoidType.v() || sm.getParameterCount() > 0) {
			if (!propagationWorklist.contains(sm))
				propagationWorklist.add(sm);
		}
	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		logger.info("Removing side-effect free methods is " + (removeSideEffectFreeMethods ? "enabled" : "disabled"));

		// Clear up any potential old state
		propagationWorklist.clear();
		propagatedParameters.clear();

		// Collect all application methods that take parameters or return values
		// and place them in the initial worklist.
		for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
			MethodOrMethodContext mom = rdr.next();
			SootMethod sm = mom.method();
			checkAndAddMethod(sm);
		}

		while (!propagationWorklist.isEmpty()) {
			SootMethod sm = propagationWorklist.remove(0);

			// Propagate constants from caller into callee
			if (sm.getParameterCount() > 0)
				propagateConstantsIntoCallee(sm);

			// Propagate constant return values from callee to caller
			if (typeSupportsConstants(sm.getReturnType()))
				propagateReturnValueIntoCallers(sm);
		}

		for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
			MethodOrMethodContext mom = rdr.next();
			SootMethod sm = mom.method();
			if (sm.hasActiveBody()) {
				List<Unit> oldCallSites = DeadCodeEliminator.getCallsInMethod(sm);

				Body body = sm.retrieveActiveBody();
				ConditionalBranchFolder.v().transform(body);
				UnconditionalBranchFolder.v().transform(body);
				DeadAssignmentEliminator.v().transform(body);
				UnreachableCodeEliminator.v().transform(body);
				UnusedLocalEliminator.v().transform(body);

				// We need to be careful and patch the cfg so
				// that it does not retain edges for call statements we have deleted
				DeadCodeEliminator.removeDeadCallgraphEdges(sm, oldCallSites);
			}
		}

		// Check for calls we can remove altogether
		if (removeSideEffectFreeMethods) {
			int callEdgesRemoved = 0;
			for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
				MethodOrMethodContext mom = rdr.next();
				SootMethod sm = mom.method();
				if (sm == null || !sm.hasActiveBody())
					continue;

				// Do not touch excluded methods
				if (excludedMethods != null && excludedMethods.contains(sm))
					continue;

				// Check for call sites
				for (Iterator<Unit> unitIt = sm.getActiveBody().getUnits().snapshotIterator(); unitIt.hasNext();) {
					Stmt s = (Stmt) unitIt.next();
					if (!sm.getActiveBody().getUnits().contains(s))
						continue;
					if (!(s instanceof InvokeStmt))
						continue;

					// If this is a fixed exception method, we must keep it
					if (exceptionClass != null
							&& ((InvokeExpr) s.getInvokeExpr()).getMethod().getDeclaringClass() == exceptionClass)
						continue;

					// If none of our pre-conditions are satisfied, there is no
					// need to look at concrete callees
					if (getNonConstParamCount(s) > 0)
						continue;

					boolean allCalleesRemoved = true;
					Set<SootClass> exceptions = new HashSet<>();
					for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(s); edgeIt.hasNext();) {
						Edge edge = edgeIt.next();
						SootMethod callee = edge.tgt();

						// If this method returns nothing, is side-effect free and does not call a sink,
						// we can remove it altogether. No data can ever flow out of it.
						boolean remove = callee.getReturnType() == VoidType.v() && !hasSideEffectsOrReadsThis(callee);
						remove |= !hasSideEffectsOrCallsSink(callee);

						if (remove) {
							Scene.v().getCallGraph().removeEdge(edge);
							callEdgesRemoved++;

							// If this callee threw an exception, we have to
							// make up for it
							fixExceptions(sm, s, exceptions);
						} else if (!sm.getName().equals("<clinit>"))
							allCalleesRemoved = false;
					}

					// If all call edges have been removed from a call site, we
					// can kill the call site altogether
					if (allCalleesRemoved && !isSourceSinkOrTaintWrapped(s))
						removeCallSite(s, sm);
				}
			}
			logger.info("Removed %d call edges", callEdgesRemoved);
		}

		// If we introduced a new class, we have to reset the hierarchy
		if (exceptionClass != null) {
			Scene.v().releaseActiveHierarchy();
			Scene.v().releaseFastHierarchy();
			Scene.v().getOrMakeFastHierarchy();
		}
	}

	/**
	 * Gets the number of non-constant arguments to the given method call
	 * 
	 * @param s A call site
	 * @return The number of non-constant arguments in the given call site
	 */
	private int getNonConstParamCount(Stmt s) {
		int cnt = 0;
		for (Value val : s.getInvokeExpr().getArgs())
			if (!(val instanceof Constant))
				cnt++;
		return cnt;
	}

	/**
	 * Checks whether the given method is a source, a sink or is accepted by the
	 * taint wrapper
	 * 
	 * @param callSite The call site to check
	 * @return True if the given method is a source, a sink or is accepted by the
	 *         taint wrapper, otherwise false
	 */
	private boolean isSourceSinkOrTaintWrapped(Stmt callSite) {
		if (!callSite.containsInvokeExpr())
			return false;

		SootMethod method = callSite.getInvokeExpr().getMethod();

		// If this method is a source on its own, we must keep it
		if (sourceSinkManager != null && sourceSinkManager.getSourceInfo((Stmt) callSite, manager) != null) {
			methodFieldReads.put(method, true);
			return true;
		}

		// If this method is a sink, we must keep it as well
		if (sourceSinkManager != null && sourceSinkManager.getSinkInfo((Stmt) callSite, manager, null) != null) {
			methodSinks.put(method, true);
			return true;
		}

		// If this method is wrapped, we need to keep it
		if (taintWrapper != null && taintWrapper.supportsCallee(method)) {
			methodSideEffects.put(method, true);
			return true;
		}

		return false;
	}

	/**
	 * Removes a given call site
	 * 
	 * @param callSite The call site to be removed
	 * @param caller   The method containing the call site
	 */
	private void removeCallSite(Stmt callSite, SootMethod caller) {
		// Make sure that we don't access anything we have already removed
		if (!caller.getActiveBody().getUnits().contains(callSite))
			return;

		// Only remove actual call sites
		if (!((Stmt) callSite).containsInvokeExpr())
			return;

		// Remove the call
		caller.getActiveBody().getUnits().remove(callSite);

		// Fix the callgraph
		if (Scene.v().hasCallGraph())
			Scene.v().getCallGraph().removeAllEdgesOutOf(callSite);
	}

	/**
	 * Checks whether constant handling is supported for the given type
	 * 
	 * @param returnType The type to check
	 * @return True if a value of the given type can be represented as a constant,
	 *         otherwise false
	 */
	private boolean typeSupportsConstants(Type returnType) {
		if (returnType == IntType.v() || returnType == LongType.v() || returnType == FloatType.v()
				|| returnType == DoubleType.v())
			return true;

		if (returnType instanceof RefType)
			if (((RefType) returnType).getClassName().equals("java.lang.String"))
				return true;

		return false;
	}

	/**
	 * Propagates the return value of the given method into all of its callers if
	 * the value is constant
	 * 
	 * @param sm The method whose value to propagate
	 */
	private void propagateReturnValueIntoCallers(SootMethod sm) {
		final IInfoflowCFG icfg = manager.getICFG();
		// We need to make sure that all exit nodes agree on the same
		// constant value
		Constant value = null;
		for (Unit retSite : icfg.getEndPointsOf(sm)) {
			// Skip exceptional exits
			if (!(retSite instanceof ReturnStmt))
				continue;

			ReturnStmt retStmt = (ReturnStmt) retSite;
			if (!(retStmt.getOp() instanceof Constant))
				return;

			if (value != null && retStmt.getOp() != value)
				return;
			value = (Constant) retStmt.getOp();
		}

		// Propagate the return value into the callers
		if (value != null)
			for (Unit callSite : icfg.getCallersOf(sm))
				if (callSite instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) callSite;

					// If we have a taint wrapper, we need to keep the stub untouched since we don't
					// know what artificial taint the wrapper will come up with
					if (taintWrapper != null && taintWrapper.supportsCallee(assign))
						continue;

					// If this is a call to a source method, we do not propagate
					// constants out of the callee for not destroying data flows
					if (sourceSinkManager != null && sourceSinkManager.getSourceInfo(assign, manager) != null)
						continue;

					// Make sure that we don't access anything we have already
					// removed
					SootMethod caller = icfg.getMethodOf(assign);
					if (caller == null || !caller.getActiveBody().getUnits().contains(assign))
						continue;

					// If the call site has multiple callees, we cannot
					// propagate a single constant
					Collection<SootMethod> callees = icfg.getCalleesOfCallAt(callSite);
					if (callees != null && callees.size() > 1)
						continue;

					// If the call has no side effects, we can remove it altogether, otherwise we
					// can just propagate the return value
					Unit assignConst = Jimple.v().newAssignStmt(assign.getLeftOp(), value);
					if (!hasSideEffectsOrCallsSink(sm)) {
						// If this method threw an exception, we have to make up
						// for it
						fixExceptions(caller, callSite);

						// We don't have side effects, so we can just change
						// a = b.foo() into a = 0.
						caller.getActiveBody().getUnits().swapWith(assign, assignConst);
						if (excludedMethods == null || !excludedMethods.contains(caller)) {
							ConstantPropagatorAndFolder.v().transform(caller.getActiveBody());
							checkAndAddMethod(caller);
						}

						// Fix the callgraph
						if (Scene.v().hasCallGraph())
							Scene.v().getCallGraph().removeAllEdgesOutOf(assign);
					} else {
						// We have side effects, so we need to keep the method
						// call. Change
						// a = b.foo() into b.foo(); a = 0;
						caller.getActiveBody().getUnits().insertAfter(assignConst, assign);
						if (excludedMethods == null || !excludedMethods.contains(caller)) {
							ConstantPropagatorAndFolder.v().transform(caller.getActiveBody());
							checkAndAddMethod(caller);
						}
						caller.getActiveBody().getUnits().remove(assignConst);

						Stmt inv = Jimple.v().newInvokeStmt(assign.getInvokeExpr());
						caller.getActiveBody().getUnits().swapWith(assign, inv);

						// Fix the callgraph
						if (Scene.v().hasCallGraph())
							Scene.v().getCallGraph().swapEdgesOutOf(assign, inv);
					}
				}
	}

	private void fixExceptions(SootMethod caller, Unit callSite) {
		fixExceptions(caller, callSite, new HashSet<>());
	}

	private void fixExceptions(SootMethod caller, Unit callSite, Set<SootClass> doneSet) {
		ThrowAnalysis ta = Options.v().src_prec() == Options.src_prec_apk ? DalvikThrowAnalysis.v()
				: UnitThrowAnalysis.v();
		ThrowableSet throwSet = ta.mightThrow(callSite);

		for (final Trap t : caller.getActiveBody().getTraps())
			if (doneSet.add(t.getException()) && throwSet.catchableAs(t.getException().getType())) {
				SootMethod thrower = exceptionThrowers.get(t.getException());
				if (thrower == null) {
					if (exceptionClass == null) {
						exceptionClass = Scene.v().makeSootClass("FLOWDROID_EXCEPTIONS", Modifier.PUBLIC);
						exceptionClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
						exceptionClass.addTag(SimulatedCodeElementTag.TAG);
						Scene.v().addClass(exceptionClass);
					}

					IEntryPointCreator epc = new BaseEntryPointCreator() {

						@Override
						public Collection<String> getRequiredClasses() {
							return Collections.emptySet();
						}

						@Override
						protected SootMethod createDummyMainInternal() {
							LocalGenerator generator = Scene.v().createLocalGenerator(body);

							// Create the counter used for the opaque predicate
							int conditionCounter = 0;
							Value intCounter = generator.generateLocal(IntType.v());
							AssignStmt assignStmt = Jimple.v().newAssignStmt(intCounter,
									IntConstant.v(conditionCounter));
							body.getUnits().add(assignStmt);

							Stmt afterEx = Jimple.v().newReturnVoidStmt();
							IfStmt ifStmt = Jimple.v().newIfStmt(
									Jimple.v().newEqExpr(intCounter, IntConstant.v(conditionCounter)), afterEx);
							body.getUnits().add(ifStmt);
							conditionCounter++;

							Local lcEx = generator.generateLocal(t.getException().getType());
							AssignStmt assignNewEx = Jimple.v().newAssignStmt(lcEx,
									Jimple.v().newNewExpr(t.getException().getType()));
							body.getUnits().add(assignNewEx);

							InvokeStmt consNewEx = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(lcEx,
									Scene.v().makeConstructorRef(exceptionClass, Collections.<Type>emptyList())));
							body.getUnits().add(consNewEx);

							ThrowStmt throwNewEx = Jimple.v().newThrowStmt(lcEx);
							body.getUnits().add(throwNewEx);

							body.getUnits().add(afterEx);
							mainMethod.addTag(SimulatedCodeElementTag.TAG);
							return mainMethod;
						}

						@Override
						protected void createEmptyMainMethod() {
							// Make sure that we don't end up with duplicate method names
							int methodIdx = exceptionThrowers.size();
							String baseName = "throw_" + t.getException().getName().replaceAll("\\W+", "_") + "_";
							String methodName;
							do {
								methodName = baseName + methodIdx++;
							} while (exceptionClass.declaresMethodByName(methodName));

							// Create the new method
							SootMethod thrower = Scene.v().makeSootMethod(methodName, Collections.<Type>emptyList(),
									VoidType.v());
							thrower.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

							final Body body = Jimple.v().newBody(thrower);
							thrower.setActiveBody(body);

							// Register the new method
							exceptionThrowers.put(t.getException(), thrower);
							exceptionClass.addMethod(thrower);

							// Make it available to the entry point creator
							mainMethod = thrower;
						}

						@Override
						public Collection<SootMethod> getAdditionalMethods() {
							return null;
						}

						@Override
						public Collection<SootField> getAdditionalFields() {
							return null;
						}

					};

					epc.createDummyMain();
					thrower = epc.getGeneratedMainMethod();
				}

				// Call the exception thrower after the old call site
				Stmt throwCall = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(thrower.makeRef()));
				throwCall.addTag(SimulatedCodeElementTag.TAG);
				caller.getActiveBody().getUnits().insertBefore(throwCall, callSite);

			}
	}

	/**
	 * Checks whether the given method or one of its transitive callees has
	 * side-effects or calls a sink method
	 * 
	 * @param method The method to check
	 * @return True if the given method or one of its transitive callees has
	 *         side-effects or calls a sink method, otherwise false.
	 */
	private boolean hasSideEffectsOrCallsSink(SootMethod method) {
		return hasSideEffectsOrCallsSink(method, new HashSet<>());
	}

	/**
	 * Checks whether the given method or one of its transitive callees has
	 * side-effects or calls a sink method
	 * 
	 * @param method  The method to check
	 * @param runList A set to receive all methods that have already been processed
	 * @param cache   The cache in which to store the results
	 * @return True if the given method or one of its transitive callees has
	 *         side-effects or calls a sink method, otherwise false.
	 */
	private boolean hasSideEffectsOrCallsSink(SootMethod method, Set<SootMethod> runList) {
		// Without a body, we cannot say much
		if (!method.hasActiveBody())
			return false;

		// Do we already have an entry?
		Boolean hasSideEffects = methodSideEffects.get(method);
		if (hasSideEffects != null)
			return hasSideEffects;

		Boolean hasSink = methodSinks.get(method);
		if (hasSink != null)
			return hasSink;

		// Do not process the same method twice
		if (!runList.add(method))
			return false;

		// If this is an Android stub method that just throws a stub exception,
		// this will never happen in practice and can be removed
		if (methodIsAndroidStub(method)) {
			methodSideEffects.put(method, false);
			return false;
		}

		// Scan for references to this variable
		for (Unit u : method.getActiveBody().getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;
				if (assign.getLeftOp() instanceof FieldRef || assign.getLeftOp() instanceof ArrayRef) {
					methodSideEffects.put(method, true);
					return true;
				}
			}

			Stmt s = (Stmt) u;

			// If this method calls another method for which we have a taint
			// wrapper, we need to conservatively assume that the taint wrapper
			// can do anything
			if (taintWrapper != null && taintWrapper.supportsCallee(s)) {
				methodSideEffects.put(method, true);
				return true;
			}

			if (s.containsInvokeExpr()) {
				// If this method calls a sink, we need to keep it
				if (sourceSinkManager != null && sourceSinkManager.getSinkInfo((Stmt) u, manager, null) != null) {
					methodSinks.put(method, true);
					return true;
				}

				// Check the callees
				for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(u); edgeIt.hasNext();) {
					Edge e = edgeIt.next();
					if (hasSideEffectsOrCallsSink(e.getTgt().method(), runList))
						return true;
				}
			}
		}

		// Variable is not read
		methodSideEffects.put(method, false);
		return false;
	}

	/**
	 * Checks whether the given method or one of its transitive callees has
	 * side-effects or calls a sink method
	 * 
	 * @param method The method to check
	 * @return True if the given method or one of its transitive callees has
	 *         side-effects or calls a sink method, otherwise false.
	 */
	private boolean hasSideEffectsOrReadsThis(SootMethod method) {
		return hasSideEffectsOrReadsThis(method, new HashSet<SootMethod>());
	}

	/**
	 * Checks whether the given method or one of its transitive callees has
	 * side-effects or calls a sink method
	 * 
	 * @param method  The method to check
	 * @param runList A set to receive all methods that have already been processed
	 * @param cache   The cache in which to store the results
	 * @return True if the given method or one of its transitive callees has
	 *         side-effects or calls a sink method, otherwise false.
	 */
	private boolean hasSideEffectsOrReadsThis(SootMethod method, Set<SootMethod> runList) {
		// Without a body, we cannot say much
		if (!method.hasActiveBody())
			return false;

		// Do we already have an entry?
		Boolean hasSideEffects = methodSideEffects.get(method);
		if (hasSideEffects != null)
			return hasSideEffects;

		// Do not process the same method twice
		if (!runList.add(method))
			return false;

		// If this is an Android stub method that just throws a stub exception,
		// this will never happen in practice and can be removed
		if (methodIsAndroidStub(method)) {
			methodSideEffects.put(method, false);
			return false;
		}

		// Scan for references to this variable
		Local thisLocal = method.isStatic() ? null : method.getActiveBody().getThisLocal();
		for (Unit u : method.getActiveBody().getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;
				if (assign.getLeftOp() instanceof FieldRef || assign.getLeftOp() instanceof ArrayRef) {
					methodSideEffects.put(method, true);
					return true;
				}
			}

			Stmt s = (Stmt) u;

			// If this statement uses the "this" local, we have to
			// conservatively assume that is can read data
			if (thisLocal != null)
				for (ValueBox vb : s.getUseBoxes())
					if (vb.getValue() == thisLocal)
						return true;

			if (s.containsInvokeExpr()) {
				// Check the callees
				for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(u); edgeIt.hasNext();) {
					Edge e = edgeIt.next();
					if (hasSideEffectsOrReadsThis(e.getTgt().method(), runList))
						return true;
				}
			}
		}

		// Variable is not read
		methodSideEffects.put(method, false);
		return false;
	}

	/**
	 * Checks whether the given method is a library stub method
	 * 
	 * @param method The method to check
	 * @return True if the given method is an Android library stub, false otherwise
	 */
	private boolean methodIsAndroidStub(SootMethod method) {
		if (!(Options.v().src_prec() == Options.src_prec_apk && method.getDeclaringClass().isLibraryClass()
				&& SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass())))
			return false;

		// Check whether there is only a single throw statement
		for (Unit u : method.getActiveBody().getUnits()) {
			if (u instanceof DefinitionStmt) {
				DefinitionStmt defStmt = (DefinitionStmt) u;
				if (!(defStmt.getRightOp() instanceof ThisRef) && !(defStmt.getRightOp() instanceof ParameterRef)
						&& !(defStmt.getRightOp() instanceof NewExpr))
					return false;
			} else if (u instanceof InvokeStmt) {
				InvokeStmt stmt = (InvokeStmt) u;

				// Check for exception constructor invocations
				SootMethod callee = stmt.getInvokeExpr().getMethod();
				if (!callee.getSubSignature().equals("void <init>(java.lang.String)"))
					// Check for super class constructor invocation
					if (!(method.getDeclaringClass().hasSuperclass()
							&& callee.getDeclaringClass() == method.getDeclaringClass().getSuperclass()
							&& callee.isConstructor()))
						return false;
			} else if (!(u instanceof ThrowStmt))
				return false;
		}
		return true;
	}

	/**
	 * Checks whether all call sites for a specific callee agree on the same
	 * constant value for one or more arguments. If so, these constant values are
	 * propagated into the callee.
	 * 
	 * @param sm The method for which to look for call sites.
	 */
	private void propagateConstantsIntoCallee(SootMethod sm) {

		// icfg field is final in InfoflowManager, hence it can't change
		// and we can cache it here so we don't have to retrieve it again and again.
		final IInfoflowCFG icfg = manager.getICFG();
		Collection<Unit> callSites = icfg.getCallersOf(sm);
		if (callSites.isEmpty())
			return;

		boolean[] isConstant = new boolean[sm.getParameterCount()];
		Constant[] values = new Constant[sm.getParameterCount()];
		for (int i = 0; i < isConstant.length; i++)
			isConstant[i] = true;

		// Do all of our callees agree on one constant value?
		boolean hasCallSites = false;
		for (Unit callSite : callSites) {
			// If this call site is in an excluded method, we ignore it
			if (excludedMethods != null && icfg.isReachable(callSite)) {
				SootMethod caller = icfg.getMethodOf(callSite);
				// synthetic methods e.g. created by FlowDroid are excluded by default
				if (excludedMethods.contains(caller) || caller.hasTag(SimulatedCodeElementTag.TAG_NAME)) {
					logger.trace("Ignoring calls from {}", caller);
					continue;
				}
			}

			// We do not support special edges that do not provide a 1:1 argument mapping
			InvokeExpr iiExpr = ((Stmt) callSite).getInvokeExpr();
			if (iiExpr.getArgCount() != sm.getParameterCount())
				continue;

			hasCallSites = true;

			// If we have a reflective call site, we never have constant
			// arguments, because
			// they are always passed in using an array
			if (icfg.isReflectiveCallSite(callSite)) {
				for (int i = 0; i < isConstant.length; i++)
					isConstant[i] = false;
			} else {
				// Check whether we have constant parameter values
				for (int i = 0; i < iiExpr.getArgCount(); i++) {
					if (isConstant[i]) {
						final Value argVal = iiExpr.getArg(i);
						if (argVal instanceof Constant) {
							// If we already have a value for this argument and
							// the new one does not agree, this parameter is not
							// globally constant.
							if (values[i] != null && !values[i].equals(argVal))
								isConstant[i] = false;
							else
								values[i] = (Constant) argVal;
						} else {
							isConstant[i] = false;
						}
					}
				}
			}
		}

		if (hasCallSites) {
			// Get the constant parameters
			List<Unit> inserted = null;
			for (int i = 0; i < isConstant.length; i++) {
				if (isConstant[i] && values[i] != null && propagatedParameters.add(new Pair<>(sm, i))) {
					// Propagate the constant into the callee
					Local paramLocal = sm.getActiveBody().getParameterLocal(i);
					Unit point = getFirstNonIdentityStmt(sm);
					Unit assignConst = Jimple.v().newAssignStmt(paramLocal, values[i]);
					sm.getActiveBody().getUnits().insertBefore(assignConst, point);

					if (inserted == null)
						inserted = new ArrayList<>();
					inserted.add(assignConst);
				}
			}

			// Propagate the constant inside the callee
			if (inserted != null) {
				ConstantPropagatorAndFolder.v().transform(sm.getActiveBody());
				for (Unit u : inserted)
					sm.getActiveBody().getUnits().remove(u);

				// This might lead to more opportunities of constant propagation
				for (Unit u : sm.getActiveBody().getUnits())
					for (SootMethod callee : icfg.getCalleesOfCallAt(u))
						checkAndAddMethod(callee);
			}
		}
	}

	/**
	 * Gets the first statement in the body of the given method that does not assign
	 * the "this" local or a parameter local
	 * 
	 * @param sm The method in whose body to look
	 * @return The first non-identity statement in the body of the given method.
	 */
	private Unit getFirstNonIdentityStmt(SootMethod sm) {
		for (Unit u : sm.getActiveBody().getUnits()) {
			if (!(u instanceof IdentityStmt))
				return u;

			IdentityStmt id = (IdentityStmt) u;
			if (!(id.getRightOp() instanceof ThisRef) && !(id.getRightOp() instanceof ParameterRef))
				return u;
		}
		return null;
	}

}
