package soot.jimple.infoflow.methodSummary.generator;

import java.util.*;

import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.methodSummary.data.sourceSink.ConstraintType;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGeneratorConfiguration.TaintCondition;
import soot.jimple.infoflow.methodSummary.generator.gaps.IGapManager;
import soot.jimple.infoflow.methodSummary.util.AliasUtils;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * Taint wrapper to be used during summary construction. If we find a call for
 * which we have no callee, we create a gap in our summary. This means that this
 * taint wrapper needs to produce fake sources for the possible outcomes of the
 * code inside the gap.
 * 
 * @author Steven Arzt
 */
public class SummaryGenerationTaintWrapper implements ITaintPropagationWrapper {

	protected InfoflowManager manager;

	protected final MethodSummaries summaries;
	protected final IGapManager gapManager;

	public SummaryGenerationTaintWrapper(MethodSummaries summaries, IGapManager gapManager) {
		this.summaries = summaries;
		this.gapManager = gapManager;
	}

	@Override
	public void initialize(InfoflowManager manager) {
		this.manager = manager;
	}

	@Override
	public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
		return Collections.emptyList();
	}

	@Override
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		// This must be a method invocation
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedPath);

		// Check whether we need to create a gap
		if (!gapManager.needsGapConstruction(stmt, taintedPath, manager.getICFG()))
			return Collections.singleton(taintedPath);

		// If we have already seen this statement, we just pass on
		if (!taintedPath.isAbstractionActive())
			return Collections.singleton(taintedPath);

		// If this is a call to a system method, we just over-approximate the
		// taint
		if (manager.getICFG().getMethodOf(stmt).getDeclaringClass().getName().equals("java.lang.System"))
			return Collections.singleton(taintedPath);

		// We do not model hashCode() and equals() for performance reasons
		Set<Abstraction> res = getTaintsForHashCodeEquals(stmt, taintedPath);
		if (res != null)
			return res;

		// Do create the gap
		GapDefinition gap = gapManager.getOrCreateGapForCall(summaries, stmt);
		if (gap == null)
			return Collections.singleton(taintedPath);

		// Produce a continuation
		res = new HashSet<Abstraction>();
		InvokeExpr iexpr = stmt.getInvokeExpr();
		if (iexpr instanceof InstanceInvokeExpr) {
			AccessPath ap = manager.getAccessPathFactory()
					.createAccessPath(((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase(), true);
			res.add(getContinuation(taintedPath, ap, gap, stmt));
		}
		for (Value paramVal : stmt.getInvokeExpr().getArgs())
			if (AccessPath.canContainValue(paramVal)) {
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(paramVal, true);
				if (AliasUtils.canAccessPathHaveAliases(ap))
					res.add(getContinuation(taintedPath, ap, gap, stmt));
			}
		if (stmt instanceof DefinitionStmt) {
			AccessPath ap = manager.getAccessPathFactory().createAccessPath(((DefinitionStmt) stmt).getLeftOp(), true);
			res.add(getContinuation(taintedPath, ap, gap, stmt));
		}

		return res;
	}

	/**
	 * Gets the taints for hashCode() and equals() and other methods with default
	 * taints if the summary generator is configured to not summarizes these methods
	 * 
	 * @param stmt        The statement that might call a method with default taint
	 * @param taintedPath The incoming taint abstraction
	 * @return The taint abstractions with which to continue the data flow analysis,
	 *         or <code>null</code> if this method does not model the call. If the
	 *         callee is handled, but no taint abstractions shall be propagated,
	 *         this method returns an empty set.
	 */
	protected Set<Abstraction> getTaintsForHashCodeEquals(Stmt stmt, Abstraction taintedPath) {
		final InvokeExpr iexpr = stmt.getInvokeExpr();
		final SummaryGeneratorConfiguration config = (SummaryGeneratorConfiguration) manager.getConfig();

		// The calls to methods with default taints are always virtual calls
		if (iexpr instanceof InstanceInvokeExpr && !config.getSummarizeHashCodeEquals()) {
			SootMethodRef ref = iexpr.getMethodRef();
			InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
			AccessPath ap = taintedPath.getAccessPath();

			TaintCondition taintCondition = config.getDefaultTaints().get(ref.getSubSignature().toString());
			if (taintCondition != null) {
				final Set<Abstraction> taints = new HashSet<Abstraction>();

				// We always keep the incoming taint
				taints.add(taintedPath);

				if (stmt instanceof DefinitionStmt) {
					DefinitionStmt defStmt = (DefinitionStmt) stmt;

					// Taint the return value only if the incoming tainted path is part of the
					// invoke expression
					if (ap.getPlainValue() == iiexpr.getBase() || iiexpr.getArgs().contains(ap.getPlainValue())) {

						// If the taint condition is fulfilled, the return value is tainted
						if (taintCondition.equals(TaintCondition.TaintAlways)
								|| (taintCondition.equals(TaintCondition.TaintOnImplicit)
										&& config.getImplicitFlowMode().trackControlFlowDependencies()))
							taints.add(taintedPath.deriveNewAbstraction(
									manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), false), stmt));
					}
				}

				return taints;
			}
		}

		// This is not a special method that we handle
		return null;
	}

	/**
	 * Creates a continuation at a gap. A continuation is a new abstraction without
	 * a predecessor that has the gap definition as its source.
	 * 
	 * @param source     The source abstraction that flowed into the gap
	 * @param accessPath The new acces path that shall be tainted after the gap
	 * @param gap
	 * @param stmt
	 * @return
	 */
	private Abstraction getContinuation(Abstraction source, AccessPath accessPath, GapDefinition gap, Stmt stmt) {
		// Make sure that we don't break anything
		Abstraction newOutAbs = source.clone().deriveNewAbstraction(accessPath, stmt);

		// Create the source information pointing to the gap. This may not be unique
		newOutAbs.setPredecessor(null);

		// If no longer have a predecessor, we must fake a source context
		newOutAbs.setSourceContext(new SourceContext((Collection<ISourceSinkDefinition>) null, accessPath, stmt, getFlowSource(accessPath, stmt, gap)));

		return newOutAbs;
	}

	/**
	 * Creates a flow source based on an access path and a gap invocation statement.
	 * The flow source need not necessarily be unique. For a call z=b.foo(a,a), the
	 * flow source for access path "a" can either be parameter 0 or parameter 1.
	 * 
	 * @param accessPath The access path for which to create the flow source
	 * @param stmt       The statement that calls the sink with the given access
	 *                   path
	 * @param gap        definition of the gap from which the data flow originates
	 * @return The set of generated flow sources
	 */
	private Set<FlowSource> getFlowSource(AccessPath accessPath, Stmt stmt, GapDefinition gap) {
		Set<FlowSource> res = new HashSet<FlowSource>();

		// This can be a base object
		if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr)
			if (((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase() == accessPath.getPlainValue())
				res.add(new FlowSource(SourceSinkType.Field, accessPath.getBaseType().toString(), gap, ConstraintType.FALSE));

		// This can be a parameter
		for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++)
			if (stmt.getInvokeExpr().getArg(i) == accessPath.getPlainValue())
				res.add(new FlowSource(SourceSinkType.Parameter, i, accessPath.getBaseType().toString(), gap, ConstraintType.FALSE));

		// This can be a return value
		if (stmt instanceof DefinitionStmt)
			if (((DefinitionStmt) stmt).getLeftOp() == accessPath.getPlainValue())
				res.add(new FlowSource(SourceSinkType.Return, accessPath.getBaseType().toString(), gap, ConstraintType.FALSE));

		return res;
	}

	@Override
	public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
		return gapManager.needsGapConstruction(stmt, taintedPath, manager.getICFG());
	}

	@Override
	public boolean supportsCallee(SootMethod method) {
		// Callees are always theoretically supported
		return true;
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		// We only wrap calls that have no callees
		Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(callSite);
		return !edgeIt.hasNext();
	}

	@Override
	public int getWrapperHits() {
		// Statics reporting is not supported by this taint wrapper
		return -1;
	}

	@Override
	public int getWrapperMisses() {
		// Statics reporting is not supported by this taint wrapper
		return -1;
	}

	@Override
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		// We don't need to process aliases
		return null;
	}

}
