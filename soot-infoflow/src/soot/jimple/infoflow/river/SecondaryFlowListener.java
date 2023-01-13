package soot.jimple.infoflow.river;

import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.problems.rules.forward.ITaintPropagationRule;

/**
 * TaintPropagationHandler to record which statements secondary flows reach
 */
public class SecondaryFlowListener implements TaintPropagationHandler {
	private IComplexFlowSourcePropagationRule sourceRule = null;

	/**
	 * Ensures that the field sinkRule is always set
	 * 
	 * @param manager
	 */
	private void ensureSinkPropagationRule(InfoflowManager manager) {
		if (sourceRule != null)
			return;

		PropagationRuleManager ruleManager = manager.getMainSolver().getTabulationProblem().getPropagationRules();
		for (ITaintPropagationRule rule : ruleManager.getRules()) {
			if (rule instanceof IComplexFlowSourcePropagationRule) {
				sourceRule = (IComplexFlowSourcePropagationRule) rule;
				return;
			}
		}

		throw new IllegalStateException("Enabled complex flows but no IComplexFlowSourcePropagationRule in place!");
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction incoming, InfoflowManager manager, FlowFunctionType type) {
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return;

		Local taintedLocal = incoming.getAccessPath().getPlainValue();
		// Check whether the taint flows into the sink
		// TODO: duplicate check?
//		if (stmt.getUseBoxes().stream().noneMatch(paramBox -> paramBox.getValue() == taintedLocal))
//			return;

		ensureSinkPropagationRule(manager);
		// Record the statement
		sourceRule.processComplexFlowSource(null, incoming, (Stmt) stmt);
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// NO-OP
		return outgoing;
	}

}
