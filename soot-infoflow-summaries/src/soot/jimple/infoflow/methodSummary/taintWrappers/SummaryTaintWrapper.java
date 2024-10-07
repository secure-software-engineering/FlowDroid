package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.Hierarchy;
import soot.Local;
import soot.MethodSubSignature;
import soot.Modifier;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.DefinitionStmt;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.ICollectionsSupport;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.strategies.containers.DefaultConfigContainerStrategyFactory;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategyFactory;
import soot.jimple.infoflow.collections.util.NonNullHashSet;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.ContainerContext;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.sourceSink.ConstraintType;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.AbstractMethodSummary;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodClear;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.data.summary.SummaryMetaData;
import soot.jimple.infoflow.methodSummary.taintWrappers.resolvers.SummaryQuery;
import soot.jimple.infoflow.methodSummary.taintWrappers.resolvers.SummaryResolver;
import soot.jimple.infoflow.methodSummary.taintWrappers.resolvers.SummaryResponse;
import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.taintWrappers.IReversibleTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * Taint wrapper implementation that applies method summaries created by
 * StubDroid
 * 
 * @author Steven Arzt
 *
 */
public class SummaryTaintWrapper implements IReversibleTaintWrapper, ICollectionsSupport {

	protected InfoflowManager manager;
	private AtomicInteger wrapperHits = new AtomicInteger();
	private AtomicInteger wrapperMisses = new AtomicInteger();
	private boolean reportMissingSummaries = false;
	protected ITaintPropagationWrapper fallbackWrapper = null;

	protected IMethodSummaryProvider flows;

	private Hierarchy hierarchy;
	private FastHierarchy fastHierarchy;
	private SummaryResolver summaryResolver;

	protected MultiMap<Pair<Abstraction, SootMethod>, AccessPathPropagator> userCodeTaints = new ConcurrentHashMultiMap<>();

	protected IContainerStrategy containerStrategy;
	protected IContainerStrategyFactory containerStrategyFactory;

	/**
	 * Handler that is used for injecting taints from callbacks implemented in user
	 * code back into the summary application process
	 * 
	 * @author Steven Arzt
	 * 
	 */
	private class SummaryFRPSHandler implements IFollowReturnsPastSeedsHandler {

		@Override
		public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2) {
			// We first check whether we return from a gap into which we have previous
			// descended
			SootMethod sm = manager.getICFG().getMethodOf(u);
			Set<AccessPathPropagator> propagators = getUserCodeTaints(d1, sm);
			if (propagators != null && !propagators.isEmpty())
				handleFlowBackFromGap(u, d2, propagators);
			else
				handleFlowSourceInGap(u, d2);
		}

		/**
		 * Handles the case in which the source of the data flow was located inside a
		 * gap and the FRPS case now injects the taint into the analysis for the first
		 * time
		 * 
		 * @param u  The return statement inside the gap
		 * @param d2 The abstraction at the return statement inside the gap
		 */
		protected void handleFlowSourceInGap(Unit u, Abstraction d2) {
			final boolean reverseFlows = manager.getConfig()
					.getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Backwards;

			// We can only handle an FRPS case if the taint actually leaves the method
			boolean taintLeavesMethod = false;
			SootMethod callee = manager.getICFG().getMethodOf(u);
			if (reverseFlows) {
				taintLeavesMethod = callee.getActiveBody().getParameterLocals()
						.contains(d2.getAccessPath().getPlainValue());
			} else {
				if (u instanceof ReturnStmt) {
					ReturnStmt retStmt = (ReturnStmt) u;
					taintLeavesMethod = retStmt.getOp() == d2.getAccessPath().getPlainValue();
				}
			}
			if (!taintLeavesMethod)
				return;

			for (Unit callSite : manager.getICFG().getCallersOf(callee)) {
				Stmt sCallSite = (Stmt) callSite;
				ClassSummaries summaries = getFlowSummariesForMethod(sCallSite, sCallSite.getInvokeExpr().getMethod(),
						null);

				List<AccessPathPropagator> worklist = new ArrayList<>();
				for (MethodFlow flow : summaries.getAllFlows()) {
					FlowSource src = flow.source();
					FlowSink tgt = flow.sink();

					boolean flowMatchesTaint = false;
					if (!reverseFlows && src.isReturn() && src.hasGap()
							&& isImplementationOf(callee, src.getGap().getSignature())) {
						// We have a flow from the return value of the gap to somewhere
						flowMatchesTaint = true;
					} else if (reverseFlows && tgt.isParameter() && tgt.hasGap()
							&& isImplementationOf(callee, tgt.getGap().getSignature())) {
						// We have a backward flow to a parameter
						flowMatchesTaint = true;
					}
					if (!flowMatchesTaint)
						continue;

					// Create the taint from the incoming access path
					Set<Taint> returnTaints = createTaintFromAccessPathOnReturn(d2.getAccessPath(), (Stmt) u,
							reverseFlows ? tgt.getGap() : src.getGap());
					if (returnTaints == null)
						continue;

					// Create the new propagator, one for every taint
					for (Taint returnTaint : returnTaints) {
						AccessPathPropagator propagator = new AccessPathPropagator(returnTaint, null, null, sCallSite,
								manager.getMainSolver().getTabulationProblem().zeroValue(), d2);
						if (reverseFlows)
							propagator = propagator.deriveInversePropagator();
						worklist.add(propagator);
					}
				}

				if (!worklist.isEmpty()) {
					Set<AccessPath> newAPs = applyFlowsIterative(summaries.getMergedMethodSummaries(), worklist,
							reverseFlows, sCallSite, d2, true);
					if (newAPs != null && !newAPs.isEmpty()) {
						Abstraction zeroValue = manager.getMainSolver().getTabulationProblem().zeroValue();
						for (AccessPath ap : newAPs) {
							Abstraction abs = d2.deriveNewAbstraction(ap, reverseFlows ? sCallSite : (Stmt) u);
							if (reverseFlows)
								abs.setCorrespondingCallSite(sCallSite);
							for (Unit succUnit : manager.getICFG().getSuccsOf(callSite))
								manager.getMainSolver()
										.processEdge(new PathEdge<Unit, Abstraction>(zeroValue, succUnit, abs));
						}
					}
				}
			}
		}

		/**
		 * Handles data flows that were originally passed into a gap, have been
		 * propagated through the gap, and must now return to user code
		 * 
		 * @param u           The return statement in the gap
		 * @param d2          The abstraction the return statement in the gap
		 * @param propagators The propagators from which the taint propagation can
		 *                    continue after the gap
		 */
		protected void handleFlowBackFromGap(Unit u, Abstraction d2, Set<AccessPathPropagator> propagators) {
			final boolean reverseFlows = manager.getConfig()
					.getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Backwards;
			for (AccessPathPropagator propagator : propagators) {
				// Propagate these taints up. We leave the current gap
				AccessPathPropagator parent = safePopParent(propagator);
				GapDefinition parentGap = propagator.getParent() == null ? null : propagator.getParent().getGap();

				// Create taints from the abstractions
				Set<Taint> returnTaints = createTaintFromAccessPathOnReturn(d2.getAccessPath(), (Stmt) u,
						propagator.getGap());
				if (returnTaints == null)
					continue;

				// Get the correct set of flows to apply
				MethodSummaries flowsInTarget = parentGap == null ? getFlowsInOriginalCallee(propagator)
						: getFlowSummariesForGap(parentGap);

				// Create the new propagator, one for every taint
				Set<AccessPathPropagator> workSet = new HashSet<>();
				for (Taint returnTaint : returnTaints) {
					AccessPathPropagator newPropagator = new AccessPathPropagator(returnTaint, parentGap, parent,
							propagator.getParent() == null ? null : propagator.getParent().getStmt(),
							propagator.getParent() == null ? null : propagator.getParent().getD1(),
							propagator.getParent() == null ? null : propagator.getParent().getD2());
					workSet.add(newPropagator);
				}

				// Apply the aggregated propagators
				AccessPathPropagator rootPropagator = getOriginalCallSite(propagator);
				Set<AccessPath> resultAPs = applyFlowsIterative(flowsInTarget, new ArrayList<>(workSet), reverseFlows,
						rootPropagator.getStmt(), d2, true);

				// Propagate the access paths
				if (resultAPs != null && !resultAPs.isEmpty()) {
					for (AccessPath ap : resultAPs) {
						Abstraction newAbs = rootPropagator.getD2().deriveNewAbstraction(ap, rootPropagator.getStmt());
						if (rootPropagator.isInversePropagator())
							newAbs.setCorrespondingCallSite(rootPropagator.getStmt());
						for (Unit succUnit : manager.getICFG().getSuccsOf(rootPropagator.getStmt()))
							manager.getMainSolver().processEdge(
									new PathEdge<Unit, Abstraction>(rootPropagator.getD1(), succUnit, newAbs));
					}
				}
			}
		}

		/**
		 * Checks whether the given method may be an implementation of a callback method
		 * with the given signature
		 * 
		 * @param sm        The method that possibly implements the callbacks
		 * @param signature The signature of the original callback method
		 * @return True if the given method may be an implementation of the callback
		 *         with the given signature
		 */
		private boolean isImplementationOf(SootMethod sm, String signature) {
			SootMethodAndClass smac = SootMethodRepresentationParser.v().parseSootMethodString(signature);
			if (!smac.getMethodName().equals(sm.getName()))
				return false;
			if (smac.getParameters().size() != sm.getParameterCount())
				return false;

			// We perform a simple parameter check for now
			for (int i = 0; i < smac.getParameters().size(); i++) {
				if (!smac.getParameters().get(i).equals(sm.getParameterType(i).toString()))
					return false;
			}

			// The given method must be in a class that inherits the callback class
			SootClass callbackClass = Scene.v().getSootClassUnsafe(smac.getClassName());
			return Scene.v().getOrMakeFastHierarchy().canStoreClass(sm.getDeclaringClass(), callbackClass);
		}

		/**
		 * Gets the flows in the method that was originally called and from where the
		 * summary application was started
		 * 
		 * @param propagator A propagator somewhere in the call tree
		 * @return The summary flows inside the original callee
		 */
		private MethodSummaries getFlowsInOriginalCallee(AccessPathPropagator propagator) {
			Stmt originalCallSite = getOriginalCallSite(propagator).getStmt();

			// Get the flows in the original callee
			ClassSummaries flowsInCallee = getFlowSummariesForMethod(originalCallSite,
					originalCallSite.getInvokeExpr().getMethod(), null);
			// if (flowsInCallee.getClasses().size() != 1)
			// throw new RuntimeException("Original callee must only be one
			// method");

			String methodSig = originalCallSite.getInvokeExpr().getMethod().getSubSignature();
			return flowsInCallee.getAllSummariesForMethod(methodSig);
		}

		/**
		 * Gets the call site at which the taint application was originally started
		 * 
		 * @param propagator A propagator somewhere in the call tree
		 * @return The call site at which the taint application was originally started
		 *         if successful, otherwise null
		 */
		private AccessPathPropagator getOriginalCallSite(AccessPathPropagator propagator) {
			// Get the original call site
			AccessPathPropagator curProp = propagator;
			while (curProp != null) {
				if (curProp.getParent() == null)
					return curProp;
				curProp = curProp.getParent();
			}
			return null;
		}

	}

	/**
	 * Creates a new instance of the {@link SummaryTaintWrapper} class
	 * 
	 * @param flows The flows loaded from disk
	 */
	public SummaryTaintWrapper(IMethodSummaryProvider flows) {
		this.flows = flows;
		setContainerStrategyFactory(new DefaultConfigContainerStrategyFactory());
	}

	/**
	 * Sets the container strategy factory.
	 * 
	 * @param factory the new factory
	 * @return this object, use for builder pattern
	 */
	public SummaryTaintWrapper setContainerStrategyFactory(IContainerStrategyFactory factory) {
		this.containerStrategyFactory = factory;
		return this;
	}

	@Override
	public IContainerStrategy getContainerStrategy() {
		return containerStrategy;
	}

	@Override
	public void initialize(InfoflowManager manager) {
		this.manager = manager;
		if (containerStrategyFactory != null)
			this.containerStrategy = containerStrategyFactory.create(manager);

		// Load all classes for which we have summaries to signatures
		Set<String> loadableClasses = flows.getAllClassesWithSummaries();
		if (loadableClasses != null) {
			for (String className : loadableClasses)
				loadClass(className);
		}

		for (String className : flows.getSupportedClasses())
			loadClass(className);

		// Initialize the resolver that decides which summary is applicable to which
		// call site
		this.summaryResolver = new SummaryResolver(flows);

		// Get the hierarchy
		final Scene scene = Scene.v();
		this.hierarchy = scene.getActiveHierarchy();
		this.fastHierarchy = scene.getOrMakeFastHierarchy();

		// Register the taint propagation handler
		manager.getMainSolver().setFollowReturnsPastSeedsHandler(new SummaryFRPSHandler());

		// If we have a fallback wrapper, we need to initialize that one as well
		if (fallbackWrapper != null)
			fallbackWrapper.initialize(manager);
	}

	class HierarchyInjector implements PreAnalysisHandler {
		@Override
		public void onBeforeCallgraphConstruction() {
			// Inject the hierarchy
			for (String className : flows.getAllClassesWithSummaries()) {
				SootClass sc = Scene.v().forceResolve(className, SootClass.SIGNATURES);
				if (!sc.isPhantom())
					continue;

				ClassMethodSummaries summaries = flows.getClassFlows(className);
				if (summaries == null)
					continue;

				// Some phantom classes are actually interfaces
				if (summaries.hasInterfaceInfo()) {
					if (summaries.isInterface())
						sc.setModifiers(sc.getModifiers() | Modifier.INTERFACE);
					else
						sc.setModifiers(sc.getModifiers() & ~Modifier.INTERFACE);
				}

				// Set the correct superclass
				if (summaries.hasSuperclass()) {
					final String superclassName = summaries.getSuperClass();
					SootClass scSuperclass = Scene.v().forceResolve(superclassName, SootClass.SIGNATURES);
					sc.setSuperclass(scSuperclass);
				}

				// Register the interfaces
				if (summaries.hasInterfaces()) {
					for (String intfName : summaries.getInterfaces()) {
						SootClass scIntf = Scene.v().forceResolve(intfName, SootClass.SIGNATURES);
						scIntf.setModifiers(sc.getModifiers() | Modifier.INTERFACE);
						if (!sc.implementsInterface(intfName))
							sc.addInterface(scIntf);
					}
				}
			}
		}

		@Override
		public void onAfterCallgraphConstruction() {
			// NO-OP
		}
	}

	public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
		return Collections.singleton(new HierarchyInjector());
	}

	/**
	 * Loads the class with the given name into the scene. This makes sure that
	 * there is at least a phantom class with the given name
	 * 
	 * @param className The name of the class to load
	 */
	protected void loadClass(String className) {
		SootClass sc = Scene.v().getSootClassUnsafe(className);
		if (sc == null) {
			sc = Scene.v().makeSootClass(className);
			sc.setPhantomClass();
			Scene.v().addClass(sc);
		} else if (sc.resolvingLevel() < SootClass.HIERARCHY)
			Scene.v().forceResolve(className, SootClass.HIERARCHY);
	}

	/**
	 * Creates a taint that can be propagated through method summaries based on the
	 * given access path. This method assumes that the given statement is a method
	 * call and that the access path is flowing into this call.
	 * 
	 * @param ap                  The access path from which to create a taint
	 * @param stmt                The statement at which the access path came in
	 * @param matchReturnedValues True if the method shall also match on the left
	 *                            side of assign statements that capture that return
	 *                            value of a method call, otherwise false
	 * @return The set of taints derived from the given access path
	 */
	protected Set<Taint> createTaintFromAccessPathOnCall(AccessPath ap, Stmt stmt, boolean matchReturnedValues,
			ByReferenceBoolean killIncomingSource) {
		Value base = getMethodBase(stmt);
		Set<Taint> newTaints = null;
		// Check whether the base object or some field in it is tainted
		if ((ap.isLocal() || ap.isInstanceFieldRef()) && base != null && base == ap.getPlainValue()) {
			if (newTaints == null)
				newTaints = new HashSet<>();

			newTaints.add(new Taint(SourceSinkType.Field, -1, ap.getBaseType().toString(), ap.getBaseContext(),
					new AccessPathFragment(ap), ap.getTaintSubFields()));
		}

		// Check whether a parameter is tainted
		int paramIdx = getParameterIndex(stmt, ap);
		if (paramIdx >= 0) {
			if (newTaints == null)
				newTaints = new HashSet<>();

			newTaints.add(new Taint(SourceSinkType.Parameter, paramIdx, ap.getBaseType().toString(),
					ap.getBaseContext(), new AccessPathFragment(ap), ap.getTaintSubFields()));
		}

		// If we also match returned values, we must do this here
		if (matchReturnedValues && stmt instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			if (defStmt.getLeftOp() == ap.getPlainValue()) {
				// retVals are killed backwards
				if (killIncomingSource != null)
					killIncomingSource.value = true;
				if (newTaints == null)
					newTaints = new HashSet<>();

				newTaints.add(new Taint(SourceSinkType.Return, -1, ap.getBaseType().toString(), ap.getBaseContext(),
						new AccessPathFragment(ap), ap.getTaintSubFields()));
			}
		}

		return newTaints;
	}

	/**
	 * Creates a set of taints that can be propagated through method summaries based
	 * on the given access path. This method assumes that the given statement is a
	 * return site from a method.
	 * 
	 * @param ap   The access path from which to create a taint
	 * @param stmt The statement at which the access path came in
	 * @param gap  The gap in which the taint is valid
	 * @return The taint derived from the given access path
	 */
	protected Set<Taint> createTaintFromAccessPathOnReturn(AccessPath ap, Stmt stmt, GapDefinition gap) {
		SootMethod sm = manager.getICFG().getMethodOf(stmt);
		Set<Taint> res = null;

		// Check whether the base object or some field in it is tainted
		if (!sm.isStatic() && (ap.isLocal() || ap.isInstanceFieldRef())
				&& ap.getPlainValue() == sm.getActiveBody().getThisLocal()) {
			if (res == null)
				res = new HashSet<>();
			res.add(new Taint(SourceSinkType.Field, -1, ap.getBaseType().toString(), ap.getBaseContext(),
					new AccessPathFragment(ap), ap.getTaintSubFields(), gap));
		}

		// Check whether a parameter is tainted
		int paramIdx = getParameterIndex(sm, ap);
		if (paramIdx >= 0) {
			if (res == null)
				res = new HashSet<>();
			res.add(new Taint(SourceSinkType.Parameter, paramIdx, ap.getBaseType().toString(), ap.getBaseContext(),
					new AccessPathFragment(ap), ap.getTaintSubFields(), gap));
		}

		// Check whether the return value is tainted
		if (stmt instanceof ReturnStmt) {
			ReturnStmt retStmt = (ReturnStmt) stmt;
			if (retStmt.getOp() == ap.getPlainValue()) {
				if (res == null)
					res = new HashSet<>();
				res.add(new Taint(SourceSinkType.Return, -1, ap.getBaseType().toString(), ap.getBaseContext(),
						new AccessPathFragment(ap), ap.getTaintSubFields(), gap));
			}
		}
		return res;
	}

	/**
	 * Converts a taint back into an access path that is valid at the given
	 * statement
	 * 
	 * @param t    The taint to convert into an access path
	 * @param stmt The statement at which the access path shall be valid
	 * @return The access path derived from the given taint
	 */
	protected AccessPath createAccessPathFromTaint(Taint t, Stmt stmt, boolean reverseFlows) {
		// Convert the taints to Soot objects
		SootField[] fields = safeGetFields(t.getAccessPath());
		Type[] types = safeGetTypes(t.getAccessPath(), fields);
		ContainerContext[][] contexts = safeGetContexts(t.getAccessPath());
		Type baseType = TypeUtils.getTypeFromString(t.getBaseType());
		ContainerContext[] baseContext = t.getBaseContext();
		soot.jimple.infoflow.data.AccessPathFragment fragments[] = soot.jimple.infoflow.data.AccessPathFragment
				.createFragmentArray(fields, types, contexts);

		// If the taint is a return value, we taint the left side of the
		// assignment
		if (t.isReturn()) {
			// If the return value is not used, we can abort
			if (!(stmt instanceof DefinitionStmt))
				return null;

			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			return manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), baseType, baseContext,
					fragments, t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength, false);
		}

		// If the taint is a parameter value, we need to identify the
		// corresponding local
		if (t.isParameter() && stmt.containsInvokeExpr()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			int paramIndex = t.getParameterIndex();

			// If the caller is missing a corresponding argument
			// then just quit here.
			// Note that this can happen, e.g., because of the android lifecycle model
			// where sometimes arguments/params get removed in native code which is
			// not represented in the call-graph.
			if (paramIndex >= iexpr.getArgCount())
				return null;

			Value paramVal = iexpr.getArg(paramIndex);
			if (!AccessPath.canContainValue(paramVal))
				return null;

			// If the target local's type does not agree with the precise type we have
			// computed, we just take the target local's type
			if (manager.getTypeUtils().getMorePreciseType(baseType, paramVal.getType()) == null)
				baseType = null;

			return manager.getAccessPathFactory().createAccessPath(paramVal, baseType, fragments, t.taintSubFields(),
					false, true, ArrayTaintType.ContentsAndLength);
		}

		// If the taint is on the base value, we need to taint the base local
		if (t.isField() && stmt.containsInvokeExpr()) {
			final InvokeExpr iexpr = stmt.getInvokeExpr();
			if (iexpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
				return manager.getAccessPathFactory().createAccessPath(iiexpr.getBase(), baseType, baseContext,
						fragments, t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength, false);
			} else if (iexpr instanceof StaticInvokeExpr) {
				// For a static invocation, we apply field taints to the return value
				StaticInvokeExpr siexpr = (StaticInvokeExpr) iexpr;
				if (!(siexpr.getMethodRef().getReturnType() instanceof VoidType)) {
					if (stmt instanceof DefinitionStmt) {
						DefinitionStmt defStmt = (DefinitionStmt) stmt;
						return manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), baseType,
								baseContext, fragments, t.taintSubFields(), false, true,
								ArrayTaintType.ContentsAndLength, false);
					} else
						return null;
				}
			}
		}

		throw new RuntimeException("Could not convert taint to access path: " + t + " at " + stmt);
	}

	/**
	 * Converts a taint into an access path that is valid inside a given method.
	 * This models that a taint is propagated into the method and from there on in
	 * normal IFDS.
	 * 
	 * @param t  The taint to convert
	 * @param sm The method in which the access path shall be created
	 * @return The access path derived from the given taint and method
	 */
	protected Set<AccessPath> createAccessPathInMethod(Taint t, SootMethod sm) {
		// Convert the taints to Soot objects
		SootField[] fields = safeGetFields(t.getAccessPath());
		Type[] types = safeGetTypes(t.getAccessPath(), fields);
		Type baseType = TypeUtils.getTypeFromString(t.getBaseType());
		ContainerContext[] baseContext = t.getBaseContext();
		ContainerContext[][] contexts = safeGetContexts(t.getAccessPath());
		soot.jimple.infoflow.data.AccessPathFragment fragments[] = soot.jimple.infoflow.data.AccessPathFragment
				.createFragmentArray(fields, types, contexts);

		// A return value cannot be propagated into a method
		if (t.isReturn()) {
			if (manager.getConfig().getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Forwards)
				throw new RuntimeException("Unsupported taint type");
			Set<AccessPath> aps = new HashSet<>();
			for (Unit unit : sm.getActiveBody().getUnits()) {
				if (!(unit instanceof ReturnStmt))
					continue;

				AccessPath ap = manager.getAccessPathFactory().createAccessPath(((ReturnStmt) unit).getOp(), baseType,
						baseContext, fragments, t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength,
						false);
				aps.add(ap);
			}
			return aps;
		}

		if (t.isParameter()) {
			Local l = sm.getActiveBody().getParameterLocal(t.getParameterIndex());
			AccessPath ap = manager.getAccessPathFactory().createAccessPath(l, baseType, baseContext, fragments, true,
					false, true, ArrayTaintType.ContentsAndLength, false);
			return ap == null ? Collections.emptySet() : Collections.singleton(ap);
		}

		if (t.isField() || t.isGapBaseObject()) {
			Local l = sm.getActiveBody().getThisLocal();
			AccessPath ap = manager.getAccessPathFactory().createAccessPath(l, baseType, baseContext, fragments, true,
					false, true, ArrayTaintType.ContentsAndLength, false);
			return ap == null ? Collections.emptySet() : Collections.singleton(ap);
		}
		throw new RuntimeException("Failed to convert taint " + t);
	}

	@Override
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		Set<Abstraction> resAbs = null;
		ByReferenceBoolean killIncomingTaint = new ByReferenceBoolean(false);
		ByReferenceBoolean classSupported = new ByReferenceBoolean(false);

		// Compute the wrapper taints for the current method
		final InvokeExpr inv = stmt.getInvokeExpr();
		SootMethod callee = inv.getMethod();
		Set<AccessPath> res;
		if (inv instanceof DynamicInvokeExpr) {
			final DynamicInvokeExpr dyn = (DynamicInvokeExpr) inv;
			SootMethod m = dyn.getBootstrapMethodRef().tryResolve();
			if (m == null)
				return null;
			callee = m;
		}
		res = computeTaintsForMethod(stmt, d1, taintedAbs, callee, killIncomingTaint, classSupported);
		boolean wasEqualToIncoming = false;

		// Create abstractions from the access paths
		if (res != null && !res.isEmpty()) {
			if (resAbs == null)
				resAbs = new HashSet<>();
			for (AccessPath ap : res) {
				resAbs.add(taintedAbs.deriveNewAbstraction(ap, stmt));
				if (ap.equals(taintedAbs.getAccessPath()))
					wasEqualToIncoming = true;
			}
		}

		// If we have no data flows, we can abort early
		if (!killIncomingTaint.value && (resAbs == null || resAbs.isEmpty())) {
			// Is this method explicitly excluded?
			if (!this.flows.isMethodExcluded(callee.getDeclaringClass().getName(), callee.getSubSignature())) {
				// wrapperMisses.incrementAndGet();

				if (classSupported.value)
					return Collections.singleton(taintedAbs);
				else {
					reportMissingSummary(callee, stmt, taintedAbs);
					return fallbackWrapper == null ? null : fallbackWrapper.getTaintsForMethod(stmt, d1, taintedAbs);
				}
			}
		}

		// We always retain the incoming abstraction unless it is explicitly
		// cleared
		if (!killIncomingTaint.value) {
			if (resAbs == null)
				return Collections.singleton(taintedAbs);
			if (!wasEqualToIncoming)
				resAbs.add(taintedAbs);
		}
		return resAbs;
	}

	/**
	 * Method that is called when no summary exists for a given method
	 * 
	 * @param method   The method for which no summary exists
	 * @param stmt     The statement that calls the method for which no summary
	 *                 exists
	 * @param incoming The incoming taint abstraction
	 */
	protected void reportMissingSummary(SootMethod method, Stmt stmt, Abstraction incoming) {
		reportMissingMethod(method);
	}

	/**
	 * Method that is called when no summary exists for a given method
	 * 
	 * @param method Method to be reported as missing
	 */
	protected void reportMissingMethod(SootMethod method) {
		if (reportMissingSummaries && SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
			System.out.println("Missing summary for class " + method.getDeclaringClass());
	}

	/**
	 * Computes library taints for the given method and incoming abstraction
	 * 
	 * @param stmt              The statement to which to apply the library summary
	 * @param d1                The context of the incoming taint
	 * @param taintedAbs        The incoming taint
	 * @param method            The method for which to get library model taints
	 * @param killIncomingTaint Outgoing value that defines whether the original
	 *                          taint shall be killed instead of being propagated
	 *                          onwards
	 * @param classSupported    Outgoing parameter that informs the caller whether
	 *                          the callee class is supported, i.e., there is a
	 *                          summary configuration for that class
	 * @return The artificial taints coming from the libary model if any, otherwise
	 *         null
	 */
	private Set<AccessPath> computeTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs,
			final SootMethod method, ByReferenceBoolean killIncomingTaint, ByReferenceBoolean classSupported) {
		// wrapperHits.incrementAndGet();

		// Get the cached data flows
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, taintedAbs, classSupported);
		if (flowsInCallees == null || flowsInCallees.isEmpty())
			return null;

		// Create a level-0 propagator for the initially tainted access path
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, false, null);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return null;

		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null || classFlows.isEmpty())
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			// Check whether the incoming taint matches a clear
			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			boolean preventPropagation = false;
			for (Taint taint : taintsFromAP) {
				boolean killTaint = false;
				if (killIncomingTaint != null && flowsInCallee.hasClears()) {
					for (MethodClear clear : flowsInCallee.getAllClears()) {
						if (clearMatchesTaint(clear, taint, stmt)) {
							killTaint = true;
							preventPropagation |= clear.preventPropagation();
							break;
						}
					}
				}

				if (killTaint)
					killIncomingTaint.value = true;
				if (!preventPropagation)
					workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs));
			}

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList, false, stmt, taintedAbs,
					killIncomingTaint.value);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}
		return res;
	}

	/**
	 * Iteratively applies all of the given flow summaries until a fixed point is
	 * reached. if the flow enters user code, an analysis of the corresponding
	 * method will be spawned.
	 *
	 * @param flowsInCallee     The flow summaries for the given callee
	 * @param workList          The incoming propagators on which to apply the flow
	 *                          summaries
	 * @param reverseFlows      True if flows should be applied reverse. Useful for
	 *                          back- wards analysis
	 * @param stmt
	 * @param incoming
	 * @param killIncomingTaint
	 * @return The set of outgoing access paths
	 */
	private Set<AccessPath> applyFlowsIterative(MethodSummaries flowsInCallee, List<AccessPathPropagator> workList,
			boolean reverseFlows, Stmt stmt, Abstraction incoming, boolean killIncomingTaint) {
		Set<AccessPath> res = null;
		Set<AccessPathPropagator> doneSet = new HashSet<AccessPathPropagator>(workList);
		while (!workList.isEmpty()) {
			final AccessPathPropagator curPropagator = workList.remove(0);
			final GapDefinition curGap = curPropagator.getGap();
			// Make sure we don't have invalid data
			if (curGap != null && curPropagator.getParent() == null)
				throw new RuntimeException("Gap flow without parent detected");

			// Get the correct set of flows to apply
			MethodSummaries flowsInTarget;
			if (curGap == null) {
				// Not in a gap, obtain the flows for the callee
				flowsInTarget = flowsInCallee;
			} else {
				// We're in a gap. We first check whether we have summaries for the declared
				// target method of the gap
				flowsInTarget = getFlowSummariesForGap(curGap);

				// If we don't have flows for the declared gap method, we use the type
				// information from the call site to infer a target
				if (flowsInTarget == null || flowsInTarget.isEmpty()) {
					SootMethod gapMethod = Scene.v().grabMethod(curGap.getSignature());
					if (gapMethod != null) {
						SootMethod sm = manager.getICFG().getMethodOf(stmt);
						ClassSummaries targetSummaries = flowsInCallee.getInFlowsForGap(curGap).stream()
								.filter(f -> f.sink().isGapBaseObject()).map(f -> f.source().getBaseLocal(stmt))
								.flatMap(l -> rebaseSootMethod(gapMethod, l, sm)).filter(m -> m != null)
								.map(m -> getFlowSummariesForMethod(stmt, m, null))
								.collect(ClassSummaries::new, ClassSummaries::merge, ClassSummaries::merge);
						flowsInTarget = targetSummaries.getAllSummariesForMethod(curGap.getSignature());
					}
				}
			}

			// If we don't have summaries for the current gap, we look for
			// implementations in the application code
			if ((flowsInTarget == null || flowsInTarget.isEmpty()) && curGap != null) {
				SootMethod callee = Scene.v().grabMethod(curGap.getSignature());
				if (callee == null) {
					SootMethodAndClass smac = SootMethodRepresentationParser.v()
							.parseSootMethodString(curGap.getSignature());
					SootClass declClass = Scene.v().getSootClassUnsafe(smac.getClassName());
					if (declClass != null) {
						SootMethodRef ref = Scene.v().makeMethodRef(declClass, smac.getSubSignature(), false);
						callee = ref.tryResolve();
					}
				}
				if (callee != null) {
					for (SootMethod implementor : getImplementors(stmt, callee)) {
						Set<AccessPathPropagator> implementorPropagators = spawnAnalysisIntoClientCode(implementor,
								curPropagator, stmt, incoming);
						if (implementorPropagators != null)
							workList.addAll(implementorPropagators);
					}
				}
			}

			// Apply the flow summaries for other libraries
			if (flowsInTarget != null && !flowsInTarget.isEmpty()) {
				if (reverseFlows)
					flowsInTarget = flowsInTarget.reverse();
				for (MethodFlow flow : flowsInTarget) {
					if (flow.isExcludedOnClear() && killIncomingTaint)
						continue;

					// Apply the flow summary
					AccessPathPropagator newPropagator = applyFlow(flow, curPropagator);
					if (newPropagator == null) {
						// Can we reverse the flow and apply it in the other direction?
						flow = getReverseFlowForAlias(flow, curPropagator.getTaint());
						if (flow == null)
							continue;

						// Apply the reversed flow
						newPropagator = applyFlow(flow, curPropagator);
						if (newPropagator == null)
							continue;
					}

					// Propagate it
					if (newPropagator.getParent() == null && newPropagator.getTaint().getGap() == null) {
						AccessPath ap = createAccessPathFromTaint(newPropagator.getTaint(), newPropagator.getStmt(),
								reverseFlows);
						if (ap == null)
							continue;
						else {
							if (res == null)
								res = new HashSet<>();
							res.add(ap);
						}
					}

					// Final flows signal that the flow itself is complete and does not need
					// another iteration to be correct. This allows to model things like return
					// the previously held value etc.
					if (doneSet.add(newPropagator) && !flow.isFinal())
						workList.add(newPropagator);

					// If we have have tainted a heap field, we need to look for
					// aliases as well
					if (newPropagator.getTaint().hasAccessPath() && !flow.isFinal()) {
						AccessPathPropagator backwardsPropagator = newPropagator.deriveInversePropagator();
						if (doneSet.add(backwardsPropagator))
							workList.add(backwardsPropagator);
					}
				}
			}
		}
		return res;
	}

	/**
	 * Finds the equivalent {@link SootMethod} in a derived class
	 * 
	 * @param targetMethod The original method
	 * @param baseLocal    The base local on which the method is invoked
	 * @param context      The method in which the target method is invoked
	 * @return The target method that is called
	 */
	public Stream<SootMethod> rebaseSootMethod(SootMethod targetMethod, Local baseLocal, SootMethod context) {
		return Scene.v().getPointsToAnalysis().reachingObjects(baseLocal).possibleTypes().stream()
				.filter(t -> t instanceof RefType).map(t -> ((RefType) t).getSootClass())
				.map(sc -> Scene.v().getOrMakeFastHierarchy().resolveMethod(sc, targetMethod, false));
	}

	/**
	 * Checks whether the given flow denotes an aliasing relationship and can thus
	 * be applied in reverse
	 * 
	 * @param flow  The flow to check
	 * @param taint
	 * @return The reverse flow if the given flow works in both directions, null
	 *         otherwise
	 */
	protected MethodFlow getReverseFlowForAlias(MethodFlow flow, Taint taint) {
		// Reverse flows can only be applied if the flow is an
		// aliasing relationship
		if (!flow.isAlias(taint))
			return null;

		// Reverse flows can only be applied to heap objects
		if (!canTypeAlias(flow.source().getLastFieldType()))
			return null;
		if (!canTypeAlias(flow.sink().getLastFieldType()))
			return null;

		// There cannot be any flows to the return values of
		// gaps
		if (flow.source().getGap() != null && flow.source().getType() == SourceSinkType.Return)
			return null;
		if (flow.sink().getGap() != null && flow.sink().getType() == SourceSinkType.Return)
			return null;

		// If the flow manipulates a constraint, the outcome may be questionable
		if (flow.sink().getConstraintType().isAction())
			return null;

		// Reverse the flow if necessary
		return flow.reverse();
	}

	/**
	 * Checks whether objects of the given type can have aliases
	 * 
	 * @param type The type to check
	 * @return True if objects of the given type can have aliases, otherwise false
	 */
	protected boolean canTypeAlias(String type) {
		Type tp = TypeUtils.getTypeFromString(type);
		if (tp instanceof PrimType)
			return false;
		if (tp instanceof RefType)
			if (((RefType) tp).getClassName().equals("java.lang.String"))
				return false;
		return true;
	}

	/**
	 * Spawns the analysis into a gap implementation inside user code
	 *
	 * @param implementor The target method inside the user code into which the
	 *                    propagator shall be propagated
	 * @param propagator  The implementor that gets propagated into user code
	 * @return The taints at the end of the implementor method if a summary already
	 *         exists, otherwise false
	 */
	protected Set<AccessPathPropagator> spawnAnalysisIntoClientCode(SootMethod implementor,
			AccessPathPropagator propagator, Stmt stmt, Abstraction incoming) {
		// If the implementor has not yet been loaded, we must do this now
		if (!implementor.hasActiveBody())
			implementor.retrieveActiveBody(body -> manager.getICFG().notifyNewBody(body));

		Set<AccessPath> aps = createAccessPathInMethod(propagator.getTaint(), implementor);
		if (aps.isEmpty())
			return null;
		Set<Abstraction> absSet = aps.stream().map(ap -> incoming.deriveNewAbstraction(ap, stmt))
				.filter(Objects::nonNull).collect(Collectors.toSet());

		// We need to pop the last gap element off the stack
		AccessPathPropagator parent = safePopParent(propagator);
		GapDefinition gap = propagator.getParent() == null ? null : propagator.getParent().getGap();

		// We might already have a summary for the callee
		Set<AccessPathPropagator> outgoingTaints = null;
		for (Abstraction abs : absSet) {
			Set<EndSummary<Unit, Abstraction>> endSummary = manager.getMainSolver().endSummary(implementor, abs);
			if (endSummary != null && !endSummary.isEmpty()) {
				for (EndSummary<Unit, Abstraction> pair : endSummary) {
					if (outgoingTaints == null)
						outgoingTaints = new HashSet<>();

					// Create the taint that corresponds to the access path leaving
					// the user-code method
					Set<Taint> newTaints = createTaintFromAccessPathOnReturn(pair.d4.getAccessPath(), (Stmt) pair.eP,
							propagator.getGap());
					if (newTaints != null) {
						for (Taint newTaint : newTaints) {
							AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent,
									propagator.getParent() == null ? null : propagator.getParent().getStmt(),
									propagator.getParent() == null ? null : propagator.getParent().getD1(),
									propagator.getParent() == null ? null : propagator.getParent().getD2());
							outgoingTaints.add(newPropagator);
						}
					}
				}
				continue;
			}

			// Register the new context so that we can get the taints back
			this.userCodeTaints.put(new Pair<>(abs, implementor), propagator);

			// if we don't have a summary, we need to inject the taints into the solver
			for (Unit sP : manager.getICFG().getStartPointsOf(implementor)) {
				PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
				manager.getMainSolver().processEdge(edge);
			}
		}

		return outgoingTaints;
	}

	protected AccessPathPropagator safePopParent(AccessPathPropagator curPropagator) {
		if (curPropagator.getParent() == null)
			return null;
		return curPropagator.getParent().getParent();
	}

	/**
	 * Gets the flow summaries for the given gap definition, i.e., for the method in
	 * the gap
	 * 
	 * @param gap The gap definition
	 * @return The flow summaries for the method in the given gap if they exist,
	 *         otherwise null
	 */
	protected MethodSummaries getFlowSummariesForGap(GapDefinition gap) {
		// If we have the method in Soot, we can be more clever
		if (Scene.v().containsMethod(gap.getSignature())) {
			SootMethod gapMethod = Scene.v().getMethod(gap.getSignature());
			ClassSummaries flows = getFlowSummariesForMethod(null, gapMethod, null);
			if (flows != null && !flows.isEmpty()) {
				MethodSummaries summaries = new MethodSummaries();
				summaries.mergeSummaries(flows.getAllMethodSummaries());
				return summaries;
			}
		}

		// If we don't have the method, we can only directly look for the
		// signature
		SootMethodAndClass smac = SootMethodRepresentationParser.v().parseSootMethodString(gap.getSignature());
		ClassMethodSummaries cms = flows.getMethodFlows(smac.getClassName(), smac.getSubSignature());
		return cms == null ? null : cms.getMethodSummaries();
	}

	/**
	 * Gets the flow summaries for the given method
	 * 
	 * @param stmt           (Optional) The invocation statement at which the given
	 *                       method is called. If this parameter is not null, it is
	 *                       used to find further potential callees if there are no
	 *                       flow summaries for the given method.
	 * @param method         The method for which to get the flow summaries
	 * @param classSupported Outgoing parameter that informs the caller whether the
	 *                       callee class is supported, i.e., there is a summary
	 *                       configuration for that class
	 * @return The set of flow summaries for the given method if they exist,
	 *         otherwise null. Note that this is a set of sets, one set per possible
	 *         callee.
	 */
	private ClassSummaries getFlowSummariesForMethod(Stmt stmt, final SootMethod method,
			ByReferenceBoolean classSupported) {
		return getFlowSummariesForMethod(stmt, method, null, classSupported);
	}

	/**
	 * Gets the flow summaries for the given method
	 * 
	 * @param stmt           (Optional) The invocation statement at which the given
	 *                       method is called. If this parameter is not null, it is
	 *                       used to find further potential callees if there are no
	 *                       flow summaries for the given method.
	 * @param method         The method for which to get the flow summaries
	 * @param taintedAbs     The tainted incoming abstraction
	 * @param classSupported Outgoing parameter that informs the caller whether the
	 *                       callee class is supported, i.e., there is a summary
	 *                       configuration for that class
	 * @return The set of flow summaries for the given method if they exist,
	 *         otherwise null. Note that this is a set of sets, one set per possible
	 *         callee.
	 */
	protected ClassSummaries getFlowSummariesForMethod(Stmt stmt, final SootMethod method, Abstraction taintedAbs,
			ByReferenceBoolean classSupported) {
		final String subsig = method.getSubSignature();

		ClassSummaries classSummaries = null;
		SootClass morePreciseClass = getSummaryDeclaringClass(stmt,
				taintedAbs == null ? null : taintedAbs.getAccessPath());

		SummaryResponse response = summaryResolver
				.resolve(new SummaryQuery(morePreciseClass, method.getDeclaringClass(), subsig));
		if (response != null) {
			if (classSupported != null)
				classSupported.value = response.isClassSupported();
			classSummaries = new ClassSummaries();
			classSummaries.merge(response.getClassSummaries());
		}

		// Check the direct callee
		if (classSummaries == null || classSummaries.isEmpty()) {
			if (!method.isConstructor() && !method.isStaticInitializer() && !method.isStatic()) {
				// Check the callgraph
				if (stmt != null) {
					// Check the callees reported by the ICFG
					for (SootMethod callee : manager.getICFG().getCalleesOfCallAt(stmt)) {
						ClassMethodSummaries flows = this.flows.getMethodFlows(callee.getDeclaringClass(), subsig);
						if (flows != null && !flows.isEmpty()) {
							if (classSupported != null)
								classSupported.value = true;
							if (classSummaries == null)
								classSummaries = new ClassSummaries();
							classSummaries.merge("<dummy>", flows.getMethodSummaries());
						}
					}
				}
			}
		}

		return classSummaries;
	}

	/**
	 * Gets the class that likely declares the method that is being called by the
	 * given statement
	 * 
	 * @param stmt      The invocation statement
	 * @param taintedAP The tainted access path
	 * @return The class in which to look for a summary for the called method
	 */
	protected SootClass getSummaryDeclaringClass(Stmt stmt, AccessPath taintedAP) {
		Type declaredType = null;
		if (stmt != null) {
			if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				// If the base object of the call is tainted, we may have a more precise type in
				// the access path
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				if (taintedAP != null && iinv.getBase() == taintedAP.getPlainValue()) {
					declaredType = taintedAP.getBaseType();
				}

				// We may have a call such as
				// x = editable.toString();
				// In that case, the callee is Object.toString(), since in the stub Android
				// JAR, the class android.text.Editable does not override toString(). On a
				// real device, it does. Consequently, we have a summary in the "Editable"
				// class. To handle such weird cases, we walk the class hierarchy based on
				// the declared type of the base object.
				Type baseType = iinv.getBase().getType();
				declaredType = manager.getTypeUtils().getMorePreciseType(declaredType, baseType);
			} else if (stmt.getInvokeExpr() instanceof DynamicInvokeExpr) {
				return ((DynamicInvokeExpr) stmt.getInvokeExpr()).getBootstrapMethodRef().getDeclaringClass();

			}
		}
		return declaredType instanceof RefType ? ((RefType) declaredType).getSootClass() : null;
	}

	/**
	 * Gets suitable methods with an active body available being cast-compatible
	 * with method. First checks the call-graph for the given statement for suitable
	 * concrete callees. If no callee is matching, the hierarchy is used to find all
	 * possible callees.
	 *
	 * @param stmt   The current statement
	 * @param method The method for which to find implementations
	 * @return A set containing implementations of the given method
	 */
	protected Collection<SootMethod> getImplementors(Stmt stmt, SootMethod method) {
		Set<SootMethod> implementors = new HashSet<SootMethod>();
		if (stmt != null) {
			for (SootMethod callee : manager.getICFG().getCalleesOfCallAt(stmt)) {
				if (!callee.isConcrete())
					continue;

				SootClass gapClass = method.getDeclaringClass();
				SootClass implClass = callee.getDeclaringClass();
				if (fastHierarchy.canStoreClass(implClass, gapClass))
					implementors.add(callee);
			}
		}

		if (!implementors.isEmpty())
			return implementors;

		final String subSig = method.getSubSignature();
		List<SootClass> workList = new ArrayList<SootClass>();
		workList.add(method.getDeclaringClass());
		Set<SootClass> doneSet = new HashSet<SootClass>();

		while (!workList.isEmpty()) {
			SootClass curClass = workList.remove(0);
			if (!doneSet.add(curClass))
				continue;

			if (curClass.isInterface()) {
				workList.addAll(hierarchy.getImplementersOf(curClass));
				workList.addAll(hierarchy.getSubinterfacesOf(curClass));
			} else
				workList.addAll(hierarchy.getSubclassesOf(curClass));

			SootMethod ifm = curClass.getMethodUnsafe(subSig);
			if (ifm != null && ifm.isConcrete())
				implementors.add(ifm);
		}

		return implementors;
	}

	/**
	 * Applies a data flow summary to a given tainted access path
	 * 
	 * @param flow       The data flow summary to apply
	 * @param propagator The access path propagator on which to apply the given flow
	 * @return The access path propagator obtained by applying the given data flow
	 *         summary to the given access path propagator. if the summary is not
	 *         applicable, null is returned.
	 */
	protected AccessPathPropagator applyFlow(MethodFlow flow, AccessPathPropagator propagator) {
		final AbstractFlowSinkSource flowSource = flow.source();
		AbstractFlowSinkSource flowSink = flow.sink();
		final Taint taint = propagator.getTaint();

		// Make sure that the base type of the incoming taint and the one of
		// the summary are compatible
		boolean typesCompatible = flowSource.getBaseType() == null
				|| isCastCompatible(TypeUtils.getTypeFromString(taint.getBaseType()),
						TypeUtils.getTypeFromString(flowSource.getBaseType()));
		if (!typesCompatible)
			return null;

		// If this flow starts at a gap, our current taint must be at that gap
		if (taint.getGap() != flow.source().getGap())
			return null;

		// Maintain the stack of access path propagations
		final AccessPathPropagator parent;
		final GapDefinition gap, taintGap;
		final Stmt stmt;
		final Abstraction d1, d2;
		if (flowSink.getGap() != null) { // ends in gap, push on stack
			parent = propagator;
			gap = flowSink.getGap();
			stmt = null;
			d1 = null;
			d2 = null;
			taintGap = null;
		} else {
			parent = safePopParent(propagator);
			gap = propagator.getParent() == null ? null : propagator.getParent().getGap();
			stmt = propagator.getParent() == null ? propagator.getStmt() : propagator.getParent().getStmt();
			d1 = propagator.getParent() == null ? propagator.getD1() : propagator.getParent().getD1();
			d2 = propagator.getParent() == null ? propagator.getD2() : propagator.getParent().getD2();
			taintGap = propagator.getGap();
		}

		boolean addTaint = flowMatchesTaint(flow, taint, propagator.getStmt());

		// If we didn't find a match, there's little we can do
		if (!addTaint)
			return null;

		// Construct a new propagator
		Taint newTaint = null;
		if (flow.isCustom()) {
			newTaint = addCustomSinkTaint(flow, taint, taintGap);
		} else
			newTaint = addSinkTaint(flow, taint, taintGap, stmt, propagator.isInversePropagator());
		if (newTaint == null)
			return null;

		if (d2 != null && !d2.isAbstractionActive() && !d2.dependsOnCutAP() && flowSink.isReturn()) {
			// Special case: x = f(y), if y is inactive, we can only taint x if we have some
			// fields left in x
			// See ln. 224 of InfoflowProblem.
			if (!newTaint.hasAccessPath() || newTaint.getAccessPathLength() == 0)
				return null;
		}

		AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent, stmt, d1, d2,
				propagator.isInversePropagator());
		return newPropagator;
	}

	/**
	 * Checks whether the type tracked in the access path is compatible with the
	 * type of the base object expected by the flow summary
	 * 
	 * @param baseType  The base type tracked in the access path
	 * @param checkType The type in the summary
	 * @return True if the tracked base type is compatible with the type expected by
	 *         the flow summary, otherwise false
	 */
	protected boolean isCastCompatible(Type baseType, Type checkType) {
		if (baseType == null || checkType == null)
			return false;

		if (baseType == Scene.v().getObjectType())
			return checkType instanceof RefType;
		if (checkType == Scene.v().getObjectType())
			return baseType instanceof RefType;

		return baseType == checkType || fastHierarchy.canStoreType(baseType, checkType)
				|| fastHierarchy.canStoreType(checkType, baseType);
	}

	/**
	 * Gets the parameter index to which the given access path refers
	 * 
	 * @param stmt  The invocation statement
	 * @param curAP The access path
	 * @return The parameter index to which the given access path refers if it
	 *         exists. Otherwise, if the given access path does not refer to a
	 *         parameter, -1 is returned.
	 */
	protected int getParameterIndex(Stmt stmt, AccessPath curAP) {
		if (!stmt.containsInvokeExpr())
			return -1;
		if (curAP.isStaticFieldRef())
			return -1;

		final InvokeExpr iexpr = stmt.getInvokeExpr();
		for (int i = 0; i < iexpr.getArgCount(); i++)
			if (iexpr.getArg(i) == curAP.getPlainValue())
				return i;
		return -1;
	}

	/**
	 * Gets the parameter index to which the given access path refers
	 * 
	 * @param sm    The method in which to check the parameter locals
	 * @param curAP The access path
	 * @return The parameter index to which the given access path refers if it
	 *         exists. Otherwise, if the given access path does not refer to a
	 *         parameter, -1 is returned.
	 */
	protected int getParameterIndex(SootMethod sm, AccessPath curAP) {
		if (curAP.isStaticFieldRef())
			return -1;

		for (int i = 0; i < sm.getParameterCount(); i++)
			if (curAP.getPlainValue() == sm.getActiveBody().getParameterLocal(i))
				return i;
		return -1;
	}

	/**
	 * Checks whether the fields mentioned in the given taint correspond to those of
	 * the given flow source
	 * 
	 * @param taintedPath The tainted access path
	 * @param flowSource  The flow source with which to compare the taint
	 * @return True if the given taint references the same fields as the given flow
	 *         source, otherwise false
	 */
	protected boolean compareFields(Taint taintedPath, AbstractFlowSinkSource flowSource) {
		// if we have x.f....fn and the source is x.f'.f1'...f'n+1 and we don't
		// taint sub, we can't have a match
		if (taintedPath.getAccessPathLength() < flowSource.getAccessPathLength()) {
			if (!taintedPath.taintSubFields() || flowSource.isMatchStrict())
				return false;
		}

		// Compare the shared sub-path
		for (int i = 0; i < taintedPath.getAccessPathLength() && i < flowSource.getAccessPathLength(); i++) {
			String taintField = taintedPath.getAccessPath().getField(i);
			String sourceField = flowSource.getAccessPath().getField(i);
			if (sourceField.equals(taintField))
				continue;

			SootField taintSootField = safeGetField(taintField);
			SootField sourceSootField = safeGetField(sourceField);
			if (taintSootField.equals(sourceSootField))
				continue;

			Scene sc = Scene.v();
			SootClass sourceClass = sc.getSootClassUnsafe(Scene.signatureToClass(sourceField));
			SootClass taintClass = sc.getSootClassUnsafe(Scene.signatureToClass(taintField));
			if (sourceClass == null || taintClass == null)
				return false;
			if (sc.getOrMakeFastHierarchy().canStoreClass(taintClass, sourceClass)) {
				if (!Scene.signatureToSubsignature(sourceField).equals(Scene.signatureToSubsignature(taintField)))
					return false;
			} else
				return false;
		}

		return true;
	}

	/**
	 * Gets the field with the specified signature if it exists, otherwise returns
	 * null
	 * 
	 * @param fieldSig The signature of the field to retrieve
	 * @return The field with the given signature if it exists, otherwise null
	 */
	protected SootField safeGetField(String fieldSig) {
		if (fieldSig == null || fieldSig.isEmpty())
			return null;

		SootField sf = Scene.v().grabField(fieldSig);
		if (sf != null)
			return sf;

		// This field does not exist, so we need to create it
		int colonIdx = fieldSig.indexOf(":");
		String className = fieldSig.substring(1, colonIdx);
		SootClass sc = Scene.v().getSootClassUnsafe(className, true);
		if (sc.resolvingLevel() < SootClass.SIGNATURES && !sc.isPhantom()) {
			System.err.println("WARNING: Class not loaded: " + sc);
			return null;
		}

		String typeAndName = fieldSig.substring(colonIdx + 2, fieldSig.length() - 1);
		int spaceIdx = typeAndName.indexOf(" ");
		String type = typeAndName.substring(0, spaceIdx);
		String fieldName = typeAndName.substring(spaceIdx + 1);

		SootFieldRef ref = Scene.v().makeFieldRef(sc, fieldName, TypeUtils.getTypeFromString(type), false);
		return ref.resolve();
	}

	/**
	 * Gets an array of fields with the specified signatures
	 * 
	 * @param accessPath The access path from which to retrieve the list of the
	 *                   field signatures
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	protected SootField[] safeGetFields(AccessPathFragment accessPath) {
		if (accessPath == null || accessPath.isEmpty())
			return null;
		return safeGetFields(accessPath.getFields());
	}

	/**
	 * Gets an array of fields with the specified signatures
	 * 
	 * @param fieldSigs , list of the field signatures to retrieve
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	protected SootField[] safeGetFields(String[] fieldSigs) {
		if (fieldSigs == null || fieldSigs.length == 0)
			return null;
		SootField[] fields = new SootField[fieldSigs.length];
		for (int i = 0; i < fieldSigs.length; i++) {
			fields[i] = safeGetField(fieldSigs[i]);
			if (fields[i] == null)
				return null;
		}
		return fields;
	}

	/**
	 * Gets an array of types with the specified class names
	 * 
	 * @param accessPath The access path from which to retrieve the field types
	 * @param fields     The fields from which to get the types if we don't have any
	 *                   explicit ones
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	protected Type[] safeGetTypes(AccessPathFragment accessPath, SootField[] fields) {
		if (accessPath == null || accessPath.isEmpty())
			return null;
		return safeGetTypes(accessPath.getFieldTypes(), fields);
	}

	/**
	 * Gets an array of types with the specified class names
	 * 
	 * @param fieldTypes , list of the type names to retrieve
	 * @param fields     The fields from which to get the types if we don't have any
	 *                   explicit ones
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	protected Type[] safeGetTypes(String[] fieldTypes, SootField[] fields) {
		if (fieldTypes == null || fieldTypes.length == 0) {
			// If we don't have type information, but fields, we can use the declared field
			// types
			if (fields != null && fields.length > 0) {
				Type[] types = new Type[fields.length];
				for (int i = 0; i < fields.length; i++)
					types[i] = fields[i].getType();
				return types;
			}
			return null;
		}

		// Parse the explicit type information
		Type[] types = new Type[fieldTypes.length];
		for (int i = 0; i < fieldTypes.length; i++)
			types[i] = TypeUtils.getTypeFromString(fieldTypes[i]);
		return types;
	}

	protected ContainerContext[][] safeGetContexts(AccessPathFragment accessPath) {
		if (accessPath == null || accessPath.isEmpty())
			return null;
		return accessPath.getContexts();
	}

	/**
	 * Given the taint at the source and the flow, computes the taint at the sink.
	 * This method allows custom extensions to the taint wrapper. The default
	 * implementation always returns null.
	 * 
	 * @param flow  The flow between source and sink
	 * @param taint The taint at the source statement
	 * @param gap   The gap at which the new flow will hold
	 * @return The taint at the sink that is obtained when applying the given flow
	 *         to the given source taint
	 */
	protected Taint addCustomSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap) {
		return null;
	}

	/**
	 * Given the taint at the source and the flow, computes the taint at the sink
	 * 
	 * @param flow  The flow between source and sink
	 * @param taint The taint at the source statement
	 * @param gap   The gap at which the new flow will hold
	 * @return The taint at the sink that is obtained when applying the given flow
	 *         to the given source taint
	 */
	protected Taint addSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap, Stmt stmt,
			boolean inversePropagator) {
		final AbstractFlowSinkSource flowSource = flow.source();
		final AbstractFlowSinkSource flowSink = flow.sink();
		final boolean taintSubFields = flow.sink().taintSubFields();
		final Boolean checkTypes = flow.getTypeChecking();

		AccessPathFragment remainingFields = cutSubFields(flow, getRemainingFields(flowSource, taint));
		AccessPathFragment appendedFields = AccessPathFragment.append(flowSink.getAccessPath(), remainingFields);

		int lastCommonAPIdx = Math.min(flowSource.getAccessPathLength(), taint.getAccessPathLength());

		Type sinkType = TypeUtils.getTypeFromString(getAssignmentType(flowSink, flow.methodSig()));
		Type taintType = TypeUtils.getTypeFromString(getAssignmentType(taint, lastCommonAPIdx - 1));

		// For type checking, we need types
		if ((checkTypes == null || checkTypes.booleanValue()) && sinkType != null && taintType != null) {
			// If we taint something in the base object, its type must match. We
			// might have a taint for "a" in o.add(a) and need to check whether
			// "o" matches the expected type in our summary.
			if (!(sinkType instanceof PrimType) && !isCastCompatible(taintType, sinkType)
					&& flowSink.getType() == SourceSinkType.Field) {
				// If the target is an array, the value might also flow into an
				// element
				boolean found = false;
				while (sinkType instanceof ArrayType) {
					sinkType = ((ArrayType) sinkType).getElementType();
					if (isCastCompatible(taintType, sinkType)) {
						found = true;
						break;
					}
				}
				while (taintType instanceof ArrayType) {
					taintType = ((ArrayType) taintType).getElementType();
					if (isCastCompatible(taintType, sinkType)) {
						found = true;
						break;
					}
				}
				if (!found)
					return null;
			}
		}

		// If we enter a gap with a type "GapBaseObject", we need to convert
		// it to a regular field
		SourceSinkType sourceSinkType = flowSink.getType();
		if (flowSink.getType() == SourceSinkType.GapBaseObject && remainingFields != null && !remainingFields.isEmpty())
			sourceSinkType = SourceSinkType.Field;

		String sBaseType = sinkType == null ? null : "" + sinkType;
		if (!flow.getIgnoreTypes()) {
			// Compute the new base type
			Type newBaseType = manager.getTypeUtils().getMorePreciseType(taintType, sinkType);
			if (newBaseType == null)
				newBaseType = sinkType;

			sBaseType = String.valueOf(newBaseType);
			// Set the correct type. In case x -> b.x, the new type is not the type
			// of b, but of the field x.
			if (flowSink.hasAccessPath()) {
				if (appendedFields != null)
					appendedFields = appendedFields.updateFieldType(flowSink.getAccessPathLength() - 1, sBaseType);
				sBaseType = flowSink.getBaseType();
			}
		}

		ContainerContext[] baseCtxt = null;
		if (containerStrategy != null) {
			if (flow.sink().isConstrained()) {
				ContainerContext[] ctxt = concretizeFlowConstraints(flow.getConstraints(), stmt,
						taint.hasAccessPath() ? taint.getAccessPath().getFirstFieldContext() : null);
				if (appendedFields != null && ctxt != null && !containerStrategy.shouldSmash(ctxt))
					appendedFields = appendedFields.addContext(ctxt);
			} else if (flow.sink().append() && !appendInfiniteAscendingChain(flow, stmt)) {
				ContainerContext[] stmtCtxt = concretizeFlowConstraints(flow.getConstraints(), stmt, null);
				ContainerContext[] taintCtxt = taint.getAccessPath().getFirstFieldContext();
				ContainerContext[] ctxt = containerStrategy.append(stmtCtxt, taintCtxt);
				if (ctxt != null && !containerStrategy.shouldSmash(ctxt) && appendedFields != null)
					appendedFields = appendedFields.addContext(ctxt);
			} else if (flow.sink().shiftLeft() && !inversePropagator) {
				ContainerContext[] taintCtxt = taint.getAccessPath().getFirstFieldContext();
				if (taintCtxt != null) {
					Tristate lte = flowShiftLeft(flowSource, flow, taint, stmt);
					if (!lte.isFalse()) {
						ContainerContext newCtxt = containerStrategy.shift(taintCtxt[0], -1, lte.isTrue());
						if (newCtxt != null && appendedFields != null) {
							appendedFields = appendedFields.addContext(
									newCtxt.containsInformation() ? new ContainerContext[] { newCtxt } : null);
						}
					}
				}
			} else if (flow.sink().shiftRight() && !inversePropagator) {
				ContainerContext[] taintCtxt = taint.getAccessPath().getFirstFieldContext();
				if (taintCtxt != null) {
					Tristate lte = flowShiftRight(flowSource, flow, taint, stmt);
					if (!lte.isFalse()) {
						ContainerContext newCtxt = containerStrategy.shift(taintCtxt[0], 1, lte.isTrue());
						if (newCtxt != null && appendedFields != null) {
							appendedFields = appendedFields.addContext(
									newCtxt.containsInformation() ? new ContainerContext[] { newCtxt } : null);
						}
					}
				}
			} else if (flow.sink().keepConstraint() || (flow.sink().keepOnRO() && containerStrategy.isReadOnly(stmt))) {
				ContainerContext[] ctxt;
				if (lastCommonAPIdx == 0)
					ctxt = taint.getBaseContext();
				else
					ctxt = taint.getAccessPath().getContext(lastCommonAPIdx - 1);

				if (ctxt != null) {
					// We may only address one constraint in the source and have another constraint
					// that is kept in the sink. Here we filter the used constraints out.
					ctxt = filterContexts(ctxt, flow.getConstraints());
					if (appendedFields == null || appendedFields.isEmpty())
						baseCtxt = ctxt;
					else
						appendedFields = appendedFields.addContext(ctxt);
				}
			}
		}

		// Taint the correct fields
		return new Taint(sourceSinkType, flowSink.getParameterIndex(), sBaseType, baseCtxt, appendedFields,
				taintSubFields || taint.taintSubFields(), gap);
	}

	/**
	 * Adding a list with addAll to itself might create an infinite ascending
	 * chain...
	 *
	 * @param flow method flow
	 * @param stmt current statement
	 * @return true if there might be an infinite ascending chain
	 */
	private boolean appendInfiniteAscendingChain(MethodFlow flow, Stmt stmt) {
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		AccessPath sourceAp = createAccessPathFromTaint(new Taint(flow.source().getType(),
				flow.source().getParameterIndex(), flow.source().getBaseType(), true), stmt, false);
		AccessPath sinkAp = createAccessPathFromTaint(
				new Taint(flow.sink().getType(), flow.sink().getParameterIndex(), flow.sink().getBaseType(), true),
				stmt, false);
		PointsToSet sourcePts = pta.reachingObjects(sourceAp.getPlainValue());
		PointsToSet sinkPts = pta.reachingObjects(sinkAp.getPlainValue());
		return sourcePts.hasNonEmptyIntersection(sinkPts);
	}

	private ContainerContext[] filterContexts(ContainerContext[] ctxt, FlowConstraint[] constraints) {
		// If the current method does not use constraints,
		// we keep the full context.
		if (constraints.length == 0)
			return ctxt;

		ContainerContext[] newCtxt = new ContainerContext[ctxt.length];
		int i = 0;
		for (int k = 0; k < constraints.length; k++) {
			FlowConstraint constraint = constraints[k];
			if (constraint.getType() == SourceSinkType.Any) {
				newCtxt[i++] = ctxt[k];
			}
		}
		if (i == 0)
			return null;
		else if (i == ctxt.length)
			return newCtxt;
		return Arrays.copyOf(newCtxt, i);
	}

	/**
	 * Cuts the given access path if required for the given method flow. This method
	 * can cut the incoming access path to shorter lengths or abandon the original
	 * access path altogether.
	 * 
	 * @param flow       The method flow specification
	 * @param accessPath The remaining fields from the original access path
	 * @return The fields that shall be appended to the new access path. This is
	 *         usually a prefix of the original remaining fields
	 */
	protected AccessPathFragment cutSubFields(MethodFlow flow, AccessPathFragment accessPath) {
		if (isCutSubFields(flow))
			return null;
		else
			return accessPath;
	}

	/**
	 * Checks whether the following fields shall be deleted when applying the given
	 * flow specification
	 * 
	 * @param flow The flow specification to check
	 * @return true if the following fields shall be deleted, false otherwise
	 */
	protected boolean isCutSubFields(MethodFlow flow) {
		Boolean cut = flow.getCutSubFields();
		return cut != null && cut.booleanValue();
	}

	/**
	 * Gets the type at the given position from a taint.
	 * 
	 * @param taint The taint from which to get the propagation type
	 * @param idx   The index inside the access path from which to get the type. -1
	 *              refers to the base type
	 * @return The type at the given index inside the access path
	 */
	protected String getAssignmentType(Taint taint, int idx) {
		if (idx < 0)
			return taint.getBaseType();

		final AccessPathFragment accessPath = taint.getAccessPath();
		if (accessPath == null)
			return null;
		final String[] fieldTypes = accessPath.getFieldTypes();

		return fieldTypes == null ? null : fieldTypes[idx];
	}

	/**
	 * Gets the type that is finally assigned when propagating this source or sink.
	 * For an access path a.b.c, this would be the type of "c".
	 * 
	 * @param srcSink   The source or sink from which to get the propagation type
	 * @param methodSig The method subsignature for retrieving missing types from
	 *                  the signature
	 * @return The type of the value which the access path of the given source or
	 *         sink finally references
	 */
	protected String getAssignmentType(AbstractFlowSinkSource srcSink, String methodSig) {
		if (!srcSink.hasAccessPath()) {
			String baseType = srcSink.getBaseType();
			if (baseType != null)
				return baseType;

			// If we have no base type in the summary, we parse it from the method signature
			if (srcSink.hasGap()) {
				// We take the signature of the gap
				String gapSig = srcSink.getGap().getSignature();
				SootMethodAndClass smac = SootMethodRepresentationParser.v().parseSootMethodString(gapSig);
				if (srcSink.isReturn())
					return smac.getReturnType();
				else if (srcSink.isParameter())
					return smac.getParameters().get(srcSink.getParameterIndex());
				else if (srcSink.isGapBaseObject())
					return smac.getClassName();
			} else {
				// We take the signature of the method for which we have the summary
				MethodSubSignature subsig = new MethodSubSignature(Scene.v().getSubSigNumberer().findOrAdd(methodSig));
				if (srcSink.isReturn())
					return subsig.returnType.toString();
				else if (srcSink.isParameter())
					return subsig.parameterTypes.get(srcSink.getParameterIndex()).toString();
			}
			return null;
		}

		// If we don't have explicit access path types, we use the declared
		// types instead
		final AccessPathFragment accessPath = srcSink.getAccessPath();
		if (accessPath.getFieldTypes() == null && accessPath.getFields() != null) {
			String[] ap = accessPath.getFields();
			String apElement = ap[srcSink.getAccessPathLength() - 1];

			Pattern pattern = Pattern.compile("^\\s*<(.*?):\\s*(.*?)>\\s*$");
			Matcher matcher = pattern.matcher(apElement);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return accessPath.getFieldTypes() == null ? null
				: accessPath.getFieldTypes()[srcSink.getAccessPathLength() - 1];
	}

	/**
	 * Gets the remaining fields which are tainted, but not covered by the given
	 * flow summary source
	 * 
	 * @param flowSource  The flow summary source
	 * @param taintedPath The tainted access path
	 * @return The remaining fields which are tainted in the given access path, but
	 *         which are not covered by the given flow summary source
	 */
	protected AccessPathFragment getRemainingFields(AbstractFlowSinkSource flowSource, Taint taintedPath) {
		if (!flowSource.hasAccessPath())
			return taintedPath.getAccessPath();

		int fieldCnt = taintedPath.getAccessPathLength() - flowSource.getAccessPathLength();
		if (fieldCnt <= 0)
			return null;

		final AccessPathFragment taintedAP = taintedPath.getAccessPath();
		String[] oldFields = taintedAP.getFields();
		String[] oldFieldTypes = taintedAP.getFieldTypes();

		String[] fields = new String[fieldCnt];
		String[] fieldTypes = new String[fieldCnt];
		System.arraycopy(oldFields, flowSource.getAccessPathLength(), fields, 0, fieldCnt);
		System.arraycopy(oldFieldTypes, flowSource.getAccessPathLength(), fieldTypes, 0, fieldCnt);

		ContainerContext[][] contexts = new ContainerContext[fieldCnt][];
		System.arraycopy(taintedAP.getContexts(), flowSource.getAccessPathLength(), contexts, 0, fieldCnt);
		return new AccessPathFragment(fields, fieldTypes, contexts);
	}

	/**
	 * Gets the base object on which the method is invoked
	 * 
	 * @param stmt The statement for which to get the base of the method call
	 * @return The base object of the method call if it exists, otherwise null
	 */
	private Value getMethodBase(Stmt stmt) {
		if (!stmt.containsInvokeExpr())
			throw new RuntimeException("Statement is not a method call: " + stmt);
		InvokeExpr invExpr = stmt.getInvokeExpr();
		if (invExpr instanceof InstanceInvokeExpr)
			return ((InstanceInvokeExpr) invExpr).getBase();
		return null;
	}

	@Override
	public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
		// If we directly support the callee, we are exclusive in any case
		if (supportsCallee(stmt)) {
			wrapperHits.getAndIncrement();
			return true;
		}

		// If the fallback wrapper supports the method and is exclusive for it,
		// we are as well
		if (fallbackWrapper != null && fallbackWrapper.isExclusive(stmt, taintedPath)) {
			wrapperHits.getAndIncrement();
			return true;
		}

		// We may also be exclusive for a complete class
		if (stmt.containsInvokeExpr()) {
			final InvokeExpr invExpr = stmt.getInvokeExpr();
			SootClass targetClass = invExpr.getMethod().getDeclaringClass();
			// The target class should never be null, but it happened
			if (targetClass != null) {
				// Are the class flows configured to be exclusive?
				final String targetClassName = targetClass.getName();
				ClassMethodSummaries cms = flows.getClassFlows(targetClassName);
				if (cms != null && cms.isExclusiveForClass()) {
					wrapperHits.getAndIncrement();
					return true;
				}

				// Check for classes excluded by meta data
				ClassSummaries summaries = flows.getSummaries();
				SummaryMetaData metaData = summaries.getMetaData();
				if (metaData != null) {
					if (metaData.isClassExclusive(targetClassName)) {
						wrapperHits.getAndIncrement();
						return true;
					}
				}

				SummaryResponse resp = summaryResolver.resolve(SummaryQuery.fromStmt(stmt));
				if (resp.isClassSupported())
					return true;
			}
		}
		wrapperMisses.getAndIncrement();
		return false;
	}

	@Override
	public boolean supportsCallee(SootMethod method) {
		// Check whether we directly support that class
		SootClass declClass = method.getDeclaringClass();
		if (declClass == null)
			return false;

		ClassMethodSummaries cms = flows.getClassFlows(declClass.getName());
		if (cms == null)
			return false;

		// We assume for exclusive classes that we have all summaries for that class.
		if (cms.isExclusiveForClass())
			return true;

		// Otherwise, we check whether we have a summary for the method.
		MethodSummaries summaries = cms.getMethodSummaries().filterForMethod(method.getSubSignature());
		return summaries != null && !summaries.isEmpty();
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		if (!callSite.containsInvokeExpr())
			return false;

		if (manager == null) {
			// If we support the method, we are exclusive for it
			SootMethod method = callSite.getInvokeExpr().getMethod();
			if (supportsCallee(method))
				return true;
		} else {
			// Check if we are exclusive for any potential callee
			for (SootMethod callee : manager.getICFG().getCalleesOfCallAt(callSite))
				if (!callee.isStaticInitializer())
					if (supportsCallee(callee))
						return true;
		}

		return false;
	}

	/**
	 * Gets the propagators that have been registered as being passed into user code
	 * with the given context and for the given callee
	 * 
	 * @param abs    The context abstraction with which the taint was passed into
	 *               the callee
	 * @param callee The callee into which the taint was passed
	 * @return The of taint propagators passed into the given callee with the given
	 *         context. If no such propagators exist, null is returned.
	 */
	Set<AccessPathPropagator> getUserCodeTaints(Abstraction abs, SootMethod callee) {
		return this.userCodeTaints.get(new Pair<>(abs, callee));
	}

	@Override
	public int getWrapperHits() {
		return wrapperHits.get();
	}

	@Override
	public int getWrapperMisses() {
		return wrapperMisses.get();
	}

	@Override
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		// Get the cached data flows
		final SootMethod method = stmt.getInvokeExpr().getMethod();
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, taintedAbs, null);

		// If we have no data flows, we can abort early
		if (flowsInCallees == null || flowsInCallees.isEmpty()) {
			if (fallbackWrapper == null)
				return null;
			else
				return fallbackWrapper.getAliasesForMethod(stmt, d1, taintedAbs);
		}

		// Create a level-0 propagator for the initially tainted access path
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, true, null);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return Collections.emptySet();

		ByReferenceBoolean killIncomingTaint = new ByReferenceBoolean();
		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			boolean reverseFlows = manager.getConfig()
					.getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Backwards;

			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null)
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;
			flowsInCallee = flowsInCallee.filterForAliases();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			boolean killTaint = false;
			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			for (Taint taint : taintsFromAP) {
				boolean preventPropagation = false;
				if (flowsInCallee.hasClears()) {
					for (MethodClear clear : flowsInCallee.getAllClears()) {
						if (clear.isAlias(taint) && clearMatchesTaint(clear, taint, stmt)) {
							killTaint = true;
							preventPropagation = clear.preventPropagation();
							break;
						}
					}
				}

				if (killTaint)
					killIncomingTaint.value = true;
				if (!preventPropagation)
					workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs, !reverseFlows));
			}

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList, false, stmt, taintedAbs, false);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}

		// We always retain the incoming taint
		if (res == null || res.isEmpty())
			return killIncomingTaint.value ? null : Collections.singleton(taintedAbs);

		// Create abstractions from the access paths
		Set<Abstraction> resAbs = new HashSet<>(res.size() + 1);
		if (!killIncomingTaint.value)
			resAbs.add(taintedAbs);
		for (AccessPath ap : res) {
			Abstraction newAbs = taintedAbs.deriveNewAbstraction(ap, stmt);
			newAbs.setCorrespondingCallSite(stmt);
			resAbs.add(newAbs);
		}
		return resAbs;
	}

	/**
	 * Sets whether missing summaries for classes shall be reported on the
	 * command-line
	 * 
	 * @param report True if missing summaries shall be reported on the command
	 *               line, otherwise false
	 */
	public void setReportMissingDummaries(boolean report) {
		this.reportMissingSummaries = report;
	}

	/**
	 * Sets the fallback taint wrapper to be used if there is no StubDroid summary
	 * for a certain class
	 * 
	 * @param fallbackWrapper The fallback taint wrapper to be used if there is no
	 *                        StubDroid summary for a certain class
	 */
	public void setFallbackTaintWrapper(ITaintPropagationWrapper fallbackWrapper) {
		this.fallbackWrapper = fallbackWrapper;
	}

	/**
	 * Gets the provider from which this taint wrapper loads it flows
	 * 
	 * @return The provider from which this taint wrapper loads it flows
	 */
	public IMethodSummaryProvider getProvider() {
		return this.flows;
	}

	@Override
	public Set<Abstraction> getInverseTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		ByReferenceBoolean classSupported = new ByReferenceBoolean(false);
		// Get the cached data flows
		final SootMethod method = stmt.getInvokeExpr().getMethod();
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, taintedAbs, classSupported);

		// If we have no data flows, we can abort early
		if (flowsInCallees.isEmpty()) {
			if (classSupported.value)
				return Collections.singleton(taintedAbs);

			if (fallbackWrapper != null && fallbackWrapper instanceof IReversibleTaintWrapper)
				return ((IReversibleTaintWrapper) fallbackWrapper).getInverseTaintsForMethod(stmt, d1, taintedAbs);
			else
				return null;
		}

		// Create a level-0 propagator for the initially tainted access path
		ByReferenceBoolean killIncomingTaint = new ByReferenceBoolean();
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, true,
				killIncomingTaint);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return Collections.emptySet();

		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null)
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			for (Taint taint : taintsFromAP) {
				if (!killIncomingTaint.value && flowsInCallee.hasClears()) {
					for (MethodClear clear : flowsInCallee.getAllClears()) {
						if (clearMatchesTaint(clear, taint, stmt)) {
							killIncomingTaint.value = true;
							break;
						}
					}
				}

				workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs, true));
			}

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList, true, stmt, taintedAbs, false);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}

		// We always retain the incoming taint
		if (res == null || res.isEmpty()) {
			// except it is overwritten
			if (killIncomingTaint.value)
				return null;
			return Collections.singleton(taintedAbs);
		}

		// Create abstractions from the access paths
		Set<Abstraction> resAbs = new HashSet<>(res.size() + 1);
		if (!killIncomingTaint.value)
			resAbs.add(taintedAbs);
		for (AccessPath ap : res) {
			Abstraction newAbs = taintedAbs.deriveNewAbstraction(ap, stmt);
			newAbs.setCorrespondingCallSite(stmt);
			resAbs.add(newAbs);
		}
		return resAbs;
	}

	@Override
	public Set<Abstraction> getTaintsForMethodApprox(Stmt stmt, Abstraction d1, Abstraction source) {
		if (containerStrategy == null)
			return null;

		if (source.getAccessPath().getFragmentCount() == 0
				|| Arrays.stream(source.getAccessPath().getFragments()).noneMatch(f -> f.hasContext()))
			return null;

		if (!(stmt.getInvokeExpr() instanceof InstanceInvokeExpr))
			return null;
		InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
		Local base = (Local) iiExpr.getBase();

		// We also need to shift if the base and the taint MAY alias. But we need the
		// answer to that now, which clashes with the on-demand alias resolving of
		// FlowDroid. Especially because we are not really able to correlate that a flow
		// originated from an alias query here. So we use some coarser approximations to
		// find out whether we need to shift or not.

		// First, we check whether they must alias, which allows us to strong update
		// here
		Tristate found = Tristate.FALSE();
		boolean mustAlias = !source.getAccessPath().isStaticFieldRef()
				&& manager.getAliasing().mustAlias(source.getAccessPath().getPlainValue(), base, stmt);
		if (mustAlias) {
			// Must alias means we definitely know how to precisely update here
			found = Tristate.TRUE();
		} else {
			// Otherwise use the points-to information of SPARK to approximate here
			PointsToSet basePts = Scene.v().getPointsToAnalysis().reachingObjects(base);
			PointsToSet incomingPts = Scene.v().getPointsToAnalysis()
					.reachingObjects(source.getAccessPath().getPlainValue());

			if (basePts.hasNonEmptyIntersection(incomingPts)) {
				found = Tristate.MAYBE();
			} else if (source.getAccessPath().getFragmentCount() > 0) {
				for (soot.jimple.infoflow.data.AccessPathFragment f : source.getAccessPath().getFragments()) {
					incomingPts = Scene.v().getPointsToAnalysis().reachingObjects(incomingPts, f.getField());
					if (basePts.hasNonEmptyIntersection(incomingPts)) {
						// Both might alias
						found = Tristate.MAYBE();
						break;
					}
				}
			}
		}

		if (found.isFalse())
			return null;

		final SootMethod method = stmt.getInvokeExpr().getMethod();
		final ByReferenceBoolean classSupported = new ByReferenceBoolean();
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, source, classSupported);
		if (flowsInCallees == null || flowsInCallees.isEmpty())
			return null;

		Set<Abstraction> res = new NonNullHashSet<>();
		for (String className : flowsInCallees.getClasses()) {
			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null || classFlows.isEmpty())
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries().getApproximateFlows();
			if (flowsInCallee == null || !flowsInCallee.hasFlows())
				continue;

			boolean shiftR = false;
			boolean shiftL = false;
			for (MethodFlow flow : flowsInCallee.getAllFlows()) {
				if (flow.sink().getConstraintType() == ConstraintType.SHIFT_RIGHT) {
					shiftR = true;
					break;
				} else if (flow.sink().getConstraintType() == ConstraintType.SHIFT_LEFT) {
					shiftL = true;
					break;
				}
			}

			if (!shiftL && !shiftR) {
				res.add(source);
			} else if (shiftR) {
				soot.jimple.infoflow.data.AccessPathFragment[] fragments = new soot.jimple.infoflow.data.AccessPathFragment[source
						.getAccessPath().getFragmentCount()];
				for (int k = 0; k < fragments.length; k++) {
					soot.jimple.infoflow.data.AccessPathFragment f = source.getAccessPath().getFragments()[k];
					if (f.getContext() == null) {
						fragments[k] = f;
						continue;
					}

					ContainerContext[] ctxt = new ContainerContext[f.getContext().length];
					for (int i = 0; i < ctxt.length; i++) {
						ContainerContext c = f.getContext()[i];
						if (c == null || !c.containsInformation())
							continue;
						ctxt[i] = containerStrategy.shift(c, 1, found.isTrue());
						fragments[k] = f.copyWithNewContext(containerStrategy.shouldSmash(ctxt) ? null : ctxt);
					}
				}
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(source.getAccessPath().getPlainValue(),
						source.getAccessPath().getBaseType(), fragments, source.getAccessPath().getTaintSubFields(),
						false, true, source.getAccessPath().getArrayTaintType());
				res.add(source.deriveNewAbstraction(ap, stmt));
			} else if (shiftL) {
				soot.jimple.infoflow.data.AccessPathFragment[] fragments = new soot.jimple.infoflow.data.AccessPathFragment[source
						.getAccessPath().getFragmentCount()];
				for (int k = 0; k < fragments.length; k++) {
					soot.jimple.infoflow.data.AccessPathFragment f = source.getAccessPath().getFragments()[k];
					if (f.getContext() == null) {
						fragments[k] = f;
						continue;
					}

					ContainerContext[] ctxt = new ContainerContext[f.getContext().length];
					for (int i = 0; i < ctxt.length; i++) {
						ContainerContext c = f.getContext()[i];
						if (c == null || !c.containsInformation())
							continue;
						ctxt[i] = containerStrategy.shift(c, -1, found.isTrue());
					}
					fragments[k] = f.copyWithNewContext(containerStrategy.shouldSmash(ctxt) ? null : ctxt);
				}
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(source.getAccessPath().getPlainValue(),
						source.getAccessPath().getBaseType(), fragments, source.getAccessPath().getTaintSubFields(),
						false, true, source.getAccessPath().getArrayTaintType());
				res.add(source.deriveNewAbstraction(ap, stmt));
			}
		}

		return res.isEmpty() ? Collections.singleton(source) : res;
	}

	protected ContainerContext[] concretizeFlowConstraints(FlowConstraint[] constraints, Stmt stmt,
			ContainerContext[] taintCtxt) {
		if (containerStrategy == null)
			return taintCtxt;
		if (stmt == null)
			System.out.println("x");
		assert stmt.containsInvokeExpr();
		InvokeExpr ie = stmt.getInvokeExpr();
		ContainerContext[] ctxt = new ContainerContext[constraints.length];
		if (constraints.length == 0)
			// e.g. if you call map.toString(), we do not have a constraint
			return taintCtxt;
		for (int i = 0; i < constraints.length; i++) {
			FlowConstraint c = constraints[i];
			switch (c.getType()) {
			case Parameter:
				if (c.isIndexBased())
					ctxt[i] = containerStrategy.getIndexContext(ie.getArg(c.getParamIdx()), stmt);
				else
					ctxt[i] = containerStrategy.getKeyContext(ie.getArg(c.getParamIdx()), stmt);
				break;
			case Implicit:
				assert c.isIndexBased();
				assert ie instanceof InstanceInvokeExpr;
				switch (c.getImplicitLocation()) {
				case First:
					ctxt[i] = containerStrategy.getFirstPosition(((InstanceInvokeExpr) ie).getBase(), stmt);
					break;
				case Last:
					ctxt[i] = containerStrategy.getLastPosition(((InstanceInvokeExpr) ie).getBase(), stmt);
					break;
				case Next:
					ctxt[i] = containerStrategy.getNextPosition(((InstanceInvokeExpr) ie).getBase(), stmt);
					break;
				default:
					throw new RuntimeException("Missing case!");
				}
				break;
			case Any:
				ctxt[i] = UnknownContext.v();
				break;
			default:
				throw new RuntimeException("Unknown context!");
			}
		}

		return ctxt;
	}

	protected Tristate matchesConstraints(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
			final Taint taint, final Stmt stmt) {
		// If no constrains apply to the flow source, we can unconditionally use it
		if (!flowSource.isConstrained())
			return Tristate.TRUE();
		ContainerContext[] taintContext = taint.getAccessPath().getFirstFieldContext();
		if (taintContext == null)
			return Tristate.TRUE();

		ContainerContext[] stmtCtxt = concretizeFlowConstraints(flow.getConstraints(), stmt, taintContext);
		assert stmtCtxt.length == taintContext.length;
		Tristate state = Tristate.TRUE();
		for (int i = 0; i < stmtCtxt.length; i++) {
			state = state.and(containerStrategy.intersect(taintContext[i], stmtCtxt[i]));
		}

		return flowSource.getConstraintType() == ConstraintType.NO_MATCH ? Tristate.fromBoolean(state.isFalse())
				: state;
	}

	protected Tristate matchesConstraintsOnClear(final AbstractFlowSinkSource flowSource,
			final AbstractMethodSummary flow, final Taint taint, final Stmt stmt) {
		// On clears, we need to under-approximate. If the clear is only valid when a
		// constraint matches and we don't have support on constraints, we refrain from
		// clearing the taint.
		if (flowSource.isConstrained() && containerStrategy == null)
			return Tristate.FALSE();
		return matchesConstraints(flowSource, flow, taint, stmt);
	}

	protected Tristate matchShiftLeft(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
			final Taint taint, final Stmt stmt) {
		ContainerContext[] taintContext = taint.getAccessPath().getFirstFieldContext();
		if (taintContext == null)
			return Tristate.FALSE();

		ContainerContext[] stmtCtxt = concretizeFlowConstraints(flow.getConstraints(), stmt, taintContext);
		assert stmtCtxt.length == taintContext.length;
		Tristate state = Tristate.TRUE();
		for (int i = 0; i < stmtCtxt.length; i++) {
			state = state.and(containerStrategy.lessThanEqual(taintContext[i], stmtCtxt[i]).negate());
		}

		return state;
	}

	protected Tristate matchShiftRight(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
			final Taint taint, final Stmt stmt) {
		// If no constrains apply to the flow source, we can unconditionally use it
		ContainerContext[] taintContext = taint.getAccessPath().getFirstFieldContext();
		if (taintContext == null)
			return Tristate.FALSE();

		ContainerContext[] stmtCtxt = concretizeFlowConstraints(flow.getConstraints(), stmt, taintContext);
		assert stmtCtxt.length == taintContext.length;
		Tristate state = Tristate.TRUE();
		for (int i = 0; i < stmtCtxt.length; i++) {
			state = state.and(containerStrategy.lessThanEqual(stmtCtxt[i], taintContext[i]));
		}

		return state;
	}

	/**
	 * Checks whether the given flow summary the given taint
	 *
	 * @param flow  The flow summary to match
	 * @param taint The taint to match
	 * @param stmt  The statement at which to perform the mapping
	 * @return True if the given flow summary matches the given taint, otherwise
	 *         false
	 */
	protected boolean flowMatchesTaint(final MethodFlow flow, final Taint taint, final Stmt stmt) {
		return !flowMatchesTaintInternal(flow.source(), flow, taint, stmt, this::matchesConstraints).isFalse();
	}

	/**
	 * Checks whether the given clear summary the given taint
	 *
	 * @param clear The clear summary to match
	 * @param taint The taint to match
	 * @param stmt  The statement at which to perform the mapping
	 * @return True if the given clear summary matches the given taint, otherwise
	 *         false
	 */
	protected boolean clearMatchesTaint(final MethodClear clear, final Taint taint, final Stmt stmt) {
		return flowMatchesTaintInternal(clear.getClearDefinition(), clear, taint, stmt, this::matchesConstraintsOnClear)
				.isTrue();
	}

	@FunctionalInterface
	protected interface MatchFunction {
		Tristate match(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow, final Taint taint,
				final Stmt stmt);
	}

	protected Tristate flowShiftLeft(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
			final Taint taint, final Stmt stmt) {
		return flowMatchesTaintInternal(flowSource, flow, taint, stmt, this::matchShiftLeft);
	}

	protected Tristate flowShiftRight(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
			final Taint taint, final Stmt stmt) {
		return flowMatchesTaintInternal(flowSource, flow, taint, stmt, this::matchShiftRight);
	}

	protected Tristate flowMatchesTaintInternal(final AbstractFlowSinkSource flowSource,
			final AbstractMethodSummary flow, final Taint taint, final Stmt stmt, MatchFunction f) {
		// Matches parameter
		boolean match = flowSource.isParameter() && taint.isParameter()
				&& taint.getParameterIndex() == flowSource.getParameterIndex();
		// Flows from a field can either be applied to the same field or the base object
		// in total
		match = match || flowSource.isField() && (taint.isGapBaseObject() || taint.isField());
		// We can have a flow from a local or a field
		match = match || flowSource.isThis() && taint.isField();
		// A value can also flow from the return value of a gap to somewhere
		match = match
				|| flowSource.isReturn() && taint.isReturn() && flowSource.getGap() != null && taint.getGap() != null;
		// For aliases, we over-approximate flows from the return edge to all possible
		// exit nodes
		match = match
				|| flowSource.isReturn() && flowSource.getGap() == null && taint.getGap() == null && taint.isReturn();
		if (!match || !compareFields(taint, flowSource))
			return Tristate.FALSE();

		return f.match(flowSource, flow, taint, stmt);
	}

}
