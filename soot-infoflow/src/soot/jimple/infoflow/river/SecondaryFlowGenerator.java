package soot.jimple.infoflow.river;

import java.util.Set;

import heros.solver.PathEdge;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.sourcesSinks.manager.IConditionalFlowManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

/**
 * TaintPropagationHandler querying the backward analysis when reaching a
 * conditional sink. Attach to the forward analysis.
 * 
 * @author Tim Lange
 */
public class SecondaryFlowGenerator implements TaintPropagationHandler {
	// SourceSinkManager that also keeps track of conditions
	private IConditionalFlowManager condFlowManager = null;

	/**
	 * Ensures the condFlowManager field is always set.
	 *
	 * @param manager Infoflow Manager
	 */
	private void ensureCondFlowManager(InfoflowManager manager) {
		if (condFlowManager != null)
			return;

		if (!manager.getConfig().getAdditionalFlowsEnabled())
			throw new IllegalStateException("Additional flows are not enabled!");

		ISourceSinkManager ssm = manager.getSourceSinkManager();
		if (ssm instanceof IConditionalFlowManager) {
			condFlowManager = (IConditionalFlowManager) ssm;
			return;
		}

		throw new IllegalStateException("Additional Flows enabled but no ConditionalFlowManager in place!");
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
		// NO-OP
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit unit, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// We only need to handle CallToReturn edges
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return outgoing;

		// Check whether any use matches the incoming taint
		if (!isReadAt(unit, incoming.getAccessPath()))
			return outgoing;

		ensureCondFlowManager(manager);
		// Check whether the statement is an instance call suitable for complex flows.
		// isConditionalSink implicitly
		Stmt stmt = (Stmt) unit;
		if (!condFlowManager.isConditionalSink(stmt))
			return outgoing;

		// Is the base tainted in the outgoing set?
		Abstraction baseTaint = getTaintFromLocal(outgoing, ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase());
		if (baseTaint == null)
			return outgoing;

		Abstraction newAbs = createAdditionalFlowAbstraction(baseTaint, stmt);

		// Query the backward analysis
		for (Unit pred : manager.getICFG().getPredsOf(unit))
			manager.additionalManager.getMainSolver().processEdge(new PathEdge<>(d1, pred, newAbs));

		return outgoing;
	}

	/**
	 * Creates a new abstraction that is injected into the backward direction.
	 *
	 * @param baseTaint taint of the base local
	 * @param stmt current statement
	 * @return new abstraction
	 */
	protected Abstraction createAdditionalFlowAbstraction(Abstraction baseTaint, Stmt stmt) {
		Abstraction newAbs = new Abstraction(null, baseTaint.getAccessPath(), stmt, null, false, false);
		newAbs.setCorrespondingCallSite(stmt);
		return newAbs;
	}

	/**
	 * Check whether baseLocal is tainted in the outgoing set.
	 * Assumes baseLocal is an object and the check happens at a call site.
	 *
	 * @param outgoing outgoing taint set
	 * @param baseLocal base local
	 * @return corresponding abstraction if baseLocal is tainted else null
	 */
	protected Abstraction getTaintFromLocal(Set<Abstraction> outgoing, Value baseLocal) {
		for (Abstraction abs : outgoing)
			if (abs.getAccessPath().getPlainValue() == baseLocal)
				return abs;

		return null;
	}

	/**
	 * Check whether the access path is read at unit.
	 *
	 * @param unit unit
	 * @param ap access path
	 * @return true if ap is read at unit
	 */
	protected boolean isReadAt(Unit unit, AccessPath ap) {
		for (ValueBox box : unit.getUseBoxes())
			if (box.getValue() == ap.getPlainValue())
				return true;

		return false;
	}
}
