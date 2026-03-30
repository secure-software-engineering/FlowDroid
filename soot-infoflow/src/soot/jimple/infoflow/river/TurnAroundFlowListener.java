package soot.jimple.infoflow.river;

import java.util.Set;

import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.ITaintPropagationRule;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;

/**
 * TaintPropagationHandler to record which statements secondary flows reach.
 * Attach to the backward analysis.
 */
public class TurnAroundFlowListener implements TaintPropagationHandler {
	private IAdditionalFlowSinkPropagationRule sinkRule = null;

	/**
	 * Ensures that the field sourceRule is always set.
	 * 
	 * @param manager
	 */
	private void ensureSourcePropagationRule(InfoflowManager manager) {
		if (sinkRule != null)
			return;

		if (!manager.getConfig().getAdditionalFlowsEnabled())
			throw new IllegalStateException("Additional flows are not enabled!");

		PropagationRuleManager ruleManager = manager.getMainSolver().getTabulationProblem().getPropagationRules();
		for (ITaintPropagationRule rule : ruleManager.getRules()) {
			if (rule instanceof IAdditionalFlowSinkPropagationRule) {
				sinkRule = (IAdditionalFlowSinkPropagationRule) rule;
				return;
			}
		}

		throw new IllegalStateException(
				"Enabled additional flows but no IConditionalFlowSinkPropagationRule in place!");
	}

	@Override
	public void notifyFlowIn(Unit unit, Abstraction incoming, InfoflowManager manager, FlowFunctionType type) {
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return;

		ensureSourcePropagationRule(manager);
		if (!(manager.getSourceSinkManager() instanceof IConditionalFlowManager))
			return;
		if (!Utils.isReadAt(unit, incoming.getAccessPath()))
			return;

		final IConditionalFlowManager ssm = (IConditionalFlowManager) manager.getSourceSinkManager();

		Stmt stmt = (Stmt) unit;
		InvokeExpr inv = stmt.getInvokeExprUnsafe();
		if (inv != null && ssm.isTurnAroundPoint(inv.getMethod())) {
			// Record the statement
			sinkRule.processTurnAroundSink(null, incoming, stmt);
		}

	}

	@Override
	public boolean notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, TaintPropagationResults results, FlowFunctionType type) {
		// NO-OP
		return false;
	}

}
