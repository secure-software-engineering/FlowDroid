package soot.jimple.infoflow.river;

import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.problems.rules.forward.ITaintPropagationRule;

/**
 * TaintPropagationHandler to record which statements secondary flows reach.
 * Attach to the backward analysis.
 */
public class SecondaryFlowListener implements TaintPropagationHandler {
	private IConditionalFlowSinkPropagationRule sourceRule = null;

	/**
	 * Ensures that the field sourceRule is always set.
	 * 
	 * @param manager
	 */
	private void ensureSourcePropagationRule(InfoflowManager manager) {
		if (sourceRule != null)
			return;

		if (!manager.getConfig().getAdditionalFlowsEnabled())
			throw new IllegalStateException("Additional flows are not enabled!");

		PropagationRuleManager ruleManager = manager.getMainSolver().getTabulationProblem().getPropagationRules();
		for (ITaintPropagationRule rule : ruleManager.getRules()) {
			if (rule instanceof IConditionalFlowSinkPropagationRule) {
				sourceRule = (IConditionalFlowSinkPropagationRule) rule;
				return;
			}
		}

		throw new IllegalStateException("Enabled additional flows but no IConditionalFlowSinkPropagationRule in place!");
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction incoming, InfoflowManager manager, FlowFunctionType type) {
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return;

		ensureSourcePropagationRule(manager);
		// Record the statement
		sourceRule.processSecondaryFlowSink(null, incoming, (Stmt) stmt);
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// NO-OP
		return outgoing;
	}

}
