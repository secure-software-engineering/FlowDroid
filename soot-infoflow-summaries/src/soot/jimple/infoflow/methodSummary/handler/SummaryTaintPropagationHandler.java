package soot.jimple.infoflow.methodSummary.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.methodSummary.generator.GapManager;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * The SummaryTaintPropagationHandler collects all abstraction that reach the
 * return statement of a specified method.
 * 
 */
public class SummaryTaintPropagationHandler implements TaintPropagationHandler {

	private final String methodSig;
	private final String parentClass;
	private final Set<SootMethod> excludedMethods = new HashSet<>();
	private final GapManager gapManager;
	private SootMethod method = null;
	private boolean followReturnsPastSeeds = false;

	private ConcurrentHashMultiMap<Abstraction, Stmt> result = new ConcurrentHashMultiMap<>();

	/**
	 * Creates a new instance of the SummaryTaintPropagationHandler class
	 * 
	 * @param m
	 *            The signature of the method for which summaries are being computed
	 * @param parentClass
	 *            The parent class to which the method belongs
	 * @param gapManager
	 *            The gap manager for creating and referencing gaps
	 */
	public SummaryTaintPropagationHandler(String m, String parentClass, GapManager gapManager) {
		this.methodSig = m;
		this.parentClass = parentClass;
		this.gapManager = gapManager;
	}

	/**
	 * Creates a new instance of the SummaryTaintPropagationHandler class
	 * 
	 * @param m
	 *            The method for which summaries are being computed
	 * @param gapManager
	 *            The gap manager for creating and referencing gaps
	 */
	public SummaryTaintPropagationHandler(SootMethod m, GapManager gapManager) {
		this.method = m;
		this.methodSig = null;
		this.parentClass = null;
		this.gapManager = gapManager;
	}

	private boolean isMethodToSummarize(SootMethod currentMethod) {
		// This must either be the method defined by signature or the
		// corresponding one in the parent class
		if (currentMethod == method)
			return true;

		return parentClass != null && currentMethod.getDeclaringClass().getName().equals(parentClass)
				&& currentMethod.getSubSignature().equals(method.getSubSignature());
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction result, InfoflowManager manager, FlowFunctionType type) {
		// Initialize the method we are interested in
		if (method == null)
			method = Scene.v().getMethod(methodSig);

		// Handle the flow function
		final IInfoflowCFG cfg = manager.getICFG();
		if (type.equals(TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction)) {
			// We only record leaving flows for those methods that we actually
			// want to generate a summary for
			SootMethod m = cfg.getMethodOf(stmt);
			if (!isMethodToSummarize(m))
				return;

			// Record the flow which leaves the method
			handleReturnFlow((Stmt) stmt, result, cfg);
		} else if (type.equals(TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction))
			handleCallToReturnFlow((Stmt) stmt, result, cfg);
	}

	/**
	 * Handles a taint that leaves a method at an exit node
	 * 
	 * @param stmt
	 *            The statement at which the taint leaves the method
	 * @param abs
	 *            The taint abstraction that leaves the method
	 * @param cfg
	 *            The control flow graph
	 */
	protected void handleReturnFlow(Stmt stmt, Abstraction abs, IInfoflowCFG cfg) {
		// Check whether we must register the abstraction for post-processing
		// We ignore inactive abstractions
		if (!abs.isAbstractionActive())
			return;

		// We record all results during the taint propagation. At this point in
		// time, we cannot yet decide whether a gap that references the respective
		// base local will later be created.
		addResult(abs, stmt);
	}

	/**
	 * Removes all collected abstractions that are neither returned from the method
	 * to be summarized, nor referenced in gaps
	 */
	private void purgeResults() {
		for (Iterator<Abstraction> absIt = result.keySet().iterator(); absIt.hasNext();) {
			Abstraction abs = absIt.next();

			// If this a taint on a field of a gap object, we need to report it as
			// well. Code can obtain references to library objects are store data in
			// there.
			boolean isGapField = gapManager.isLocalReferencedInGap(abs.getAccessPath().getPlainValue());

			// If this abstraction is neither referenced in a gap, nor returned,
			// we remove it
			if (!isGapField) {
				boolean isReturned = false;
				for (Stmt stmt : result.get(abs))
					if (isValueReturnedFromCall(stmt, abs)) {
						isReturned = true;
						break;
					}
				if (!isReturned)
					absIt.remove();
			}
		}
	}

	/**
	 * Checks whether the given value is returned from inside the callee at the
	 * given call site
	 * 
	 * @param stmt
	 *            The statement to check
	 * @param abs
	 *            The value to check
	 * @return True if the given value is returned from inside the given callee at
	 *         the given call site, otherwise false
	 */
	private boolean isValueReturnedFromCall(Unit stmt, Abstraction abs) {
		// If the value is returned, we save it
		if (stmt instanceof ReturnStmt) {
			ReturnStmt retStmt = (ReturnStmt) stmt;
			if (retStmt.getOp() == abs.getAccessPath().getPlainValue())
				return true;
		}

		// If the value is thrown, we save it
		if (stmt instanceof ThrowStmt) {
			ThrowStmt throwStmt = (ThrowStmt) stmt;
			if (throwStmt.getOp() == abs.getAccessPath().getPlainValue())
				return true;
		}

		// If the value corresponds to a parameter, we save it
		for (Value param : method.getActiveBody().getParameterLocals())
			if (abs.getAccessPath().getPlainValue() == param)
				return true;

		// If the value is a field, we save it
		return (!method.isStatic() && abs.getAccessPath().getPlainValue() == method.getActiveBody().getThisLocal());
	}

	protected void handleCallToReturnFlow(Stmt stmt, Abstraction abs, IInfoflowCFG cfg) {
		// Check whether we must construct a gap
		if (gapManager.needsGapConstruction(stmt, abs, cfg))
			addResult(abs, stmt);
	}

	/**
	 * Adds the given abstraction and statement to the result map
	 * 
	 * @param abs
	 *            The abstraction to be collected
	 * @param stmt
	 *            The statement at which the abstraction was collected
	 */
	protected void addResult(Abstraction abs, Stmt stmt) {
		// Add the abstraction to the map. If we already have an equal
		// abstraction, we must add the current one as a neighbor.
		if (!this.result.put(abs, stmt)) {
			for (Abstraction abs2 : result.keySet()) {
				if (abs.equals(abs2)) {
					abs2.addNeighbor(abs);
					break;
				}
			}
		}
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit u, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// Do not propagate through excluded methods
		SootMethod sm = manager.getICFG().getMethodOf(u);
		if (excludedMethods.contains(sm))
			return Collections.emptySet();
		if (type == FlowFunctionType.ReturnFlowFunction && !followReturnsPastSeeds && sm == method)
			return Collections.emptySet();

		return outgoing;
	}

	public MultiMap<Abstraction, Stmt> getResult() {
		purgeResults();
		return result;
	}

	public GapManager getGapManager() {
		return this.gapManager;
	}

	/**
	 * Adds the given method to the set of excluded methods over which taints are
	 * never propagated
	 * 
	 * @param excluded
	 *            The method to exclude
	 */
	public void addExcludedMethod(SootMethod excluded) {
		this.excludedMethods.add(excluded);
	}

	/**
	 * Sets whether propagations out of the method to be summaries shall be allowed.
	 * If not the analysis will not march up the callgraph any further from that
	 * method.
	 * 
	 * @param follow
	 *            True if propagations upwards from the target method shall be
	 *            allowed, otherwise false
	 */
	public void setFollowReturnsPastSeeds(boolean follow) {
		this.followReturnsPastSeeds = follow;
	}

}
