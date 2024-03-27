package soot.jimple.infoflow.collections.taintWrappers;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.NonNullHashSet;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.ContainerContext;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.sourceSink.ConstraintType;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.summary.*;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathPropagator;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.*;
import java.util.function.Function;

public class CollectionSummaryTaintWrapper extends SummaryTaintWrapper implements ICollectionsSupport {
    protected IContainerStrategy containerStrategy;
    protected Function<InfoflowManager, IContainerStrategy> gen;

    /**
     * Creates a new instance of the {@link SummaryTaintWrapper} class
     *
     * @param flows The flows loaded from disk
     */
    public CollectionSummaryTaintWrapper(IMethodSummaryProvider flows, Function<InfoflowManager, IContainerStrategy> gen) {
        super(flows);
        this.gen = gen;
    }

    @Override
    public void initialize(InfoflowManager manager) {
        super.initialize(manager);

        this.containerStrategy = gen.apply(manager);
    }

    private SootField getConstrainedField(AbstractFlowSinkSource afss) {
        ConstraintType ct = afss.getConstraintType();
        if (ct == null || ct == ConstraintType.FALSE)
            return null;
        else
            return safeGetField(afss.getAccessPath().getFirstFieldName());
    }

    private boolean flowDependsOnContext(AbstractFlowSinkSource afss) {
        switch (afss.getConstraintType()) {
            case TRUE:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case READONLY:
            case APPEND:
                return true;
            default:
                return false;
        }
    }

    private SootMethod methodSigToMethod(String clazz, String subsig) {
        SootClass sc = Scene.v().getSootClassUnsafe(clazz);
        SootMethod sm = null;
        while (sm == null && sc != null) {
            sm = sc.getMethodUnsafe(subsig);
            sc = sc.getSuperclassUnsafe();
        }
        return sm;
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
        final SootMethod callee = stmt.getInvokeExpr().getMethod();
        Set<AccessPath> res = computeTaintsForMethod(stmt, d1, taintedAbs, callee, killIncomingTaint, classSupported);

        // Create abstractions from the access paths
        if (res != null && !res.isEmpty()) {
            if (resAbs == null)
                resAbs = new HashSet<>();
            for (AccessPath ap : res)
                resAbs.add(taintedAbs.deriveNewAbstraction(ap, stmt));
        }

        // If we have no data flows, we can abort early
        if (!killIncomingTaint.value && (resAbs == null || resAbs.isEmpty())) {
            // Is this method explicitly excluded?
            if (!this.flows.isMethodExcluded(callee.getDeclaringClass().getName(), callee.getSubSignature())) {
//				wrapperMisses.incrementAndGet();

                if (classSupported.value)
                    return Collections.singleton(taintedAbs);
                else {
                    reportMissingSummary(callee, stmt, taintedAbs);
                    return fallbackWrapper != null ? fallbackWrapper.getTaintsForMethod(stmt, d1, taintedAbs) : null;
                }
            }
        }

        // We always retain the incoming abstraction unless it is explicitly
        // cleared
        if (!killIncomingTaint.value) {
            if (resAbs == null)
                return Collections.singleton(taintedAbs);
            resAbs.add(taintedAbs);
        }
        return resAbs;
    }

    @Override
    public Set<Abstraction> getTaintsForMethodApprox(Stmt stmt, Abstraction d1, Abstraction source) {
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
            PointsToSet incomingPts = Scene.v().getPointsToAnalysis().reachingObjects(source.getAccessPath().getPlainValue());

            if (basePts.hasNonEmptyIntersection(incomingPts)) {
                found = Tristate.MAYBE();
            } else if (source.getAccessPath().getFragmentCount() > 0) {
                for (soot.jimple.infoflow.data.AccessPathFragment f : source.getAccessPath().getFragments()) {
                    incomingPts = Scene.v().getPointsToAnalysis().reachingObjects(incomingPts,
                            f.getField());
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
                soot.jimple.infoflow.data.AccessPathFragment[] fragments = new soot.jimple.infoflow.data.AccessPathFragment[source.getAccessPath().getFragmentCount()];
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
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(source.getAccessPath().getPlainValue(), source.getAccessPath().getBaseType(), fragments,
                        source.getAccessPath().getTaintSubFields(), false, true, source.getAccessPath().getArrayTaintType());
                res.add(source.deriveNewAbstraction(ap, stmt));
            } else if (shiftL) {
                soot.jimple.infoflow.data.AccessPathFragment[] fragments = new soot.jimple.infoflow.data.AccessPathFragment[source.getAccessPath().getFragmentCount()];
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
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(source.getAccessPath().getPlainValue(), source.getAccessPath().getBaseType(), fragments,
                        source.getAccessPath().getTaintSubFields(), false, true, source.getAccessPath().getArrayTaintType());
                res.add(source.deriveNewAbstraction(ap, stmt));
            }
        }

        return res.isEmpty() ? Collections.singleton(source) : res;
    }

    @Override
    public IContainerStrategy getContainerStrategy() {
        return containerStrategy;
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
//		wrapperHits.incrementAndGet();

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
                        if (flowMatchesTaint(clear, taint, stmt)) {
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
                                                            killIncomingTaint);
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
     * @param flowsInCallee The flow summaries for the given callee
     * @param workList      The incoming propagators on which to apply the flow
     *                      summaries
     * @param reverseFlows  True if flows should be applied reverse. Useful for
     *                      back- wards analysis
     * @param stmt
     * @param incoming
     * @return The set of outgoing access paths
     */
    private Set<AccessPath> applyFlowsIterative(MethodSummaries flowsInCallee, List<AccessPathPropagator> workList,
                                                boolean reverseFlows, Stmt stmt, Abstraction incoming,
                                                ByReferenceBoolean killIncomingTaint) {
        Set<AccessPath> res = null;
        Set<AccessPathPropagator> doneSet = new HashSet<>(workList);
        while (!workList.isEmpty()) {
            final AccessPathPropagator curPropagator = workList.remove(0);
            final GapDefinition curGap = curPropagator.getGap();
            // Make sure we don't have invalid data
            if (curGap != null && curPropagator.getParent() == null)
                throw new RuntimeException("Gap flow without parent detected");

            // Get the correct set of flows to apply
            MethodSummaries flowsInTarget = curGap == null ? flowsInCallee : getFlowSummariesForGap(curGap);

            // If we don't have summaries for the current gap, we look for
            // implementations in the application code
            if ((flowsInTarget == null || flowsInTarget.isEmpty()) && curGap != null) {
                SootMethod callee = Scene.v().grabMethod(curGap.getSignature());
                if (callee != null) {
                    Collection<SootMethod> implementors = getImplementors(curPropagator.getStmt(), callee);
                    for (SootMethod implementor : implementors) {
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
                    if (flow.isExcludedOnClear() && killIncomingTaint.value)
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

    protected MethodFlow getReverseFlowForAlias(MethodFlow flow, Taint t) {
        MethodFlow reversed = flow.reverse();
        // Reverse flows can only be applied if the flow is an
        // aliasing relationship
        if (!reversed.isAlias(t))
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

        return reversed;
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
            // Special case: x = f(y), if y is inactive, we can only taint x if we have some fields left in x
            // See ln. 224 of InfoflowProblem.
            if (!newTaint.hasAccessPath() || newTaint.getAccessPathLength() == 0)
                return null;
        }

        AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent, stmt, d1, d2,
                propagator.isInversePropagator());
        return newPropagator;
    }

    protected boolean flowMatchesTaint(final AbstractFlowSinkSource flowSource, final Taint taint) {
        throw new RuntimeException("Do not use");
    }

    protected ContainerContext[] concretizeFlowConstraints(FlowConstraint[] constraints, Stmt stmt, ContainerContext[] taintCtxt) {
        assert stmt.containsInvokeExpr();
        InvokeExpr ie = stmt.getInvokeExpr();
        ContainerContext[] ctxt = new ContainerContext[constraints.length];
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

        return flowSource.getConstraintType() == ConstraintType.NO_MATCH ? Tristate.fromBoolean(state.isFalse()) : state;
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
     * Checks whether the given source matches the given taint
     *
     * @param flow      The flow to match
     * @param taint      The taint to match
     * @return True if the given source matches the given taint, otherwise false
     */
    protected boolean flowMatchesTaint(final MethodFlow flow, final Taint taint, final Stmt stmt) {
        return !flowMatchesTaintInternal(flow.source(), flow, taint, stmt, this::matchesConstraints).isFalse();
    }

    protected boolean flowMatchesTaint(final MethodClear flow, final Taint taint, final Stmt stmt) {
        return flowMatchesTaintInternal(flow.getClearDefinition(), flow, taint, stmt, this::matchesConstraints).isTrue();
    }

    @FunctionalInterface
    protected interface MatchFunction {
        Tristate match(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
                       final Taint taint, final Stmt stmt);
    }

    protected Tristate flowShiftLeft(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
                                     final Taint taint, final Stmt stmt) {
        return flowMatchesTaintInternal(flowSource, flow, taint, stmt, this::matchShiftLeft);
    }

    protected Tristate flowShiftRight(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
                                     final Taint taint, final Stmt stmt) {
        return flowMatchesTaintInternal(flowSource, flow, taint, stmt, this::matchShiftRight);
    }

    protected Tristate flowMatchesTaintInternal(final AbstractFlowSinkSource flowSource, final AbstractMethodSummary flow,
                                               final Taint taint, final Stmt stmt, MatchFunction f) {
        // Matches parameter
        boolean match = flowSource.isParameter() && taint.isParameter()
                        && taint.getParameterIndex() == flowSource.getParameterIndex();
        // Flows from a field can either be applied to the same field or the base object in total
        match = match || flowSource.isField() && (taint.isGapBaseObject() || taint.isField());
        // We can have a flow from a local or a field
        match = match || flowSource.isThis() && taint.isField();
        // A value can also flow from the return value of a gap to somewhere
        match = match || flowSource.isReturn() && taint.isReturn() && flowSource.getGap() != null && taint.getGap() != null;
        // For aliases, we over-approximate flows from the return edge to all possible exit nodes
        match = match || flowSource.isReturn() && flowSource.getGap() == null && taint.getGap() == null && taint.isReturn();
        if (!match || !compareFields(taint, flowSource))
            return Tristate.FALSE();

        return f.match(flowSource, flow, taint, stmt);
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

    protected Taint addSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap) {
        throw new RuntimeException("Do not use");
    }

    /**
     * Adding a list with addAll to itself might create an infinite ascending chain...
     *
     * @param flow method flow
     * @param stmt current statement
     * @return true if there might be an infinite ascending chain
     */
    private boolean appendInfiniteAscendingChain(MethodFlow flow, Stmt stmt) {
        PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        AccessPath sourceAp = createAccessPathFromTaint(new Taint(flow.source().getType(),
                                                                  flow.source().getParameterIndex(),
                                                                  flow.source().getBaseType(),
                                                                  true),
                                                        stmt, false);
        AccessPath sinkAp = createAccessPathFromTaint(new Taint(flow.sink().getType(),
                                                                flow.sink().getParameterIndex(),
                                                                flow.sink().getBaseType(),
                                                                true),
                                                      stmt, false);
        PointsToSet sourcePts = pta.reachingObjects(sourceAp.getPlainValue());
        PointsToSet sinkPts = pta.reachingObjects(sinkAp.getPlainValue());
        return sourcePts.hasNonEmptyIntersection(sinkPts);
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
    protected Taint addSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap, Stmt stmt, boolean inversePropagator) {
        final AbstractFlowSinkSource flowSource = flow.source();
        final AbstractFlowSinkSource flowSink = flow.sink();
        final boolean taintSubFields = flow.sink().taintSubFields();
        final Boolean checkTypes = flow.getTypeChecking();

        AccessPathFragment remainingFields = cutSubFields(flow, getRemainingFields(flowSource, taint));
        AccessPathFragment appendedFields = AccessPathFragment.append(flowSink.getAccessPath(), remainingFields);

        int lastCommonAPIdx = Math.min(flowSource.getAccessPathLength(), taint.getAccessPathLength());

        Type sinkType = TypeUtils.getTypeFromString(getAssignmentType(flowSink));
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

            // Set the correct type. In case x -> b.x, the new type is not the type
            // of b, but of the field x.
            if (flowSink.hasAccessPath()) {
                if (appendedFields != null)
                    appendedFields = appendedFields.updateFieldType(flowSink.getAccessPathLength() - 1,
                            String.valueOf(newBaseType));
                sBaseType = flowSink.getBaseType();
            }
        }

        ContainerContext[] baseCtxt = null;

        if (flow.sink().isConstrained()) {
            ContainerContext[] ctxt = concretizeFlowConstraints(flow.getConstraints(), stmt, taint.hasAccessPath() ? taint.getAccessPath().getFirstFieldContext() : null);
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
                        appendedFields = appendedFields.addContext(newCtxt.containsInformation() ? new ContainerContext[]{newCtxt} : null);
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
                        appendedFields = appendedFields.addContext(newCtxt.containsInformation() ? new ContainerContext[]{newCtxt} : null);
                    }
                }
            }
        } else if (flow.sink().keepConstraint()
                    || (flow.sink().keepOnRO() && containerStrategy.isReadOnly(stmt))) {
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

        // Taint the correct fields
        return new Taint(sourceSinkType, flowSink.getParameterIndex(), sBaseType, baseCtxt, appendedFields,
                taintSubFields || taint.taintSubFields(), gap);
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
                        if (clear.isAlias(taint) && flowMatchesTaint(clear, taint, stmt)) {
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
            Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList, false, stmt, taintedAbs,
                                                            killIncomingTaint);
            if (resCallee != null && !resCallee.isEmpty()) {
                if (res == null)
                    res = new HashSet<>();
                res.addAll(resCallee);
            }
        }

        // We always retain the incoming taint
        if (res == null || res.isEmpty())
            return killIncomingTaint.value ? Collections.emptySet() : Collections.singleton(taintedAbs);

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
}
