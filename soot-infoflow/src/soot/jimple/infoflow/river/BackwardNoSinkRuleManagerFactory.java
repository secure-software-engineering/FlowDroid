package soot.jimple.infoflow.river;

import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.problems.rules.backward.BackwardsArrayPropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsClinitRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsExceptionPropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsImplicitFlowRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsSourcePropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsStrongUpdatePropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsWrapperRule;
import soot.jimple.infoflow.problems.rules.ITaintPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.SkipSystemClassRule;
import soot.jimple.infoflow.problems.rules.forward.StopAfterFirstKFlowsPropagationRule;

/**
 * PropagationRuleManagerFactory used for the backward direction in River.
 * Expects an empty source sink manager.
 * 
 * @author Tim Lange
 */
public class BackwardNoSinkRuleManagerFactory implements IPropagationRuleManagerFactory {

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		List<ITaintPropagationRule> ruleList = new ArrayList<>();

		// backwards only
		ruleList.add(new BackwardsSourcePropagationRule(manager, zeroValue, results));
		ruleList.add(new BackwardsClinitRule(manager, zeroValue, results));
		ruleList.add(new BackwardsStrongUpdatePropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getEnableExceptionTracking())
			ruleList.add(new BackwardsExceptionPropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getEnableArrayTracking())
			ruleList.add(new BackwardsArrayPropagationRule(manager, zeroValue, results));
		if (manager.getTaintWrapper() != null)
			ruleList.add(new BackwardsWrapperRule(manager, zeroValue, results));

		// shared
		ruleList.add(new SkipSystemClassRule(manager, zeroValue, results));
		if (manager.getConfig().getStopAfterFirstKFlows() > 0)
			ruleList.add(new StopAfterFirstKFlowsPropagationRule(manager, zeroValue, results));

		if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
			ruleList.add(new BackwardsImplicitFlowRule(manager, zeroValue, results));

		return new PropagationRuleManager(manager, zeroValue, results, ruleList.toArray(new ITaintPropagationRule[0]));
	}

}
