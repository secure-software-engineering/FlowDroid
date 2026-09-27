package soot.jimple.infoflow.river;

import java.util.Collections;
import java.util.Set;

import heros.solver.PathEdge;
import soot.RefType;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

/**
 * TaintPropagationHandler querying the forward analysis when reaching a
 * turnaround point. Attach to the backward analysis.
 * 
 */

public class TurnAroundFlowGenerator implements TaintPropagationHandler {
	// SourceSinkManager that also keeps track of conditions
	private IConditionalFlowManager condFlowManager = null;
	private IInfoflowSolver forwardSolver;

	public TurnAroundFlowGenerator(IInfoflowSolver forwardSolver) {
		this.forwardSolver = forwardSolver;
	}

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
	public boolean notifyFlowOut(Unit unit, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, TaintPropagationResults results, FlowFunctionType type) {
		// We only need to handle CallToReturn edges
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return false;

		// Check whether any use matches the incoming taint
		if (!Utils.isReadAt(unit, incoming.getAccessPath()))
			return false;

		ensureCondFlowManager(manager);

		Stmt stmt = (Stmt) unit;

		// Check for sink contexts
		if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			Abstraction baseTaint = Utils.getTaintFromLocal(outgoing,
					((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase());

			// Is the base tainted in the outgoing set?
			if (baseTaint != null && baseTaint.getAccessPath().getBaseType() instanceof RefType) {
				if (manager.getSourceSinkManager().isTurnAroundPoint(stmt.getInvokeExpr().getMethod())) {
					Abstraction newAbs = createAdditionalFlowAbstraction(baseTaint, stmt);
					// Query the forward analysis
					forwardSolver.processEdge(new PathEdge<>(d1, unit, newAbs));

				}
			}
		}

		return false;
	}

	/**
	 * Creates a new abstraction that is injected into the forward direction.
	 *
	 * @param baseTaint Taint of the base local
	 * @param stmt      Current statement
	 * @return New abstraction
	 */
	protected Abstraction createAdditionalFlowAbstraction(Abstraction baseTaint, Stmt stmt) {
		Abstraction newAbs = new Abstraction(Collections.singleton(TurnAroundSecondarySinkDefinition.INSTANCE),
				baseTaint.getAccessPath(), stmt, null, false, false);
		newAbs.setCorrespondingCallSite(stmt);
		newAbs.setSourceContext(new AdditionalFlowInfoSourceContext(TurnAroundSecondarySinkDefinition.INSTANCE,
				baseTaint.getAccessPath(), stmt));
		return newAbs.deriveNewAbstractionWithTurnUnit(stmt);
	}
}
