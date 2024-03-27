package soot.jimple.infoflow.problems.rules;

import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.backward.*;
import soot.jimple.infoflow.problems.rules.forward.SkipSystemClassRule;
import soot.jimple.infoflow.problems.rules.forward.StopAfterFirstKFlowsPropagationRule;

/**
 * Backward implementation of the {@link IPropagationRuleManagerFactory} class
 * 
 * @author Tim Lange
 *
 */
public class BackwardPropagationRuleManagerFactory implements IPropagationRuleManagerFactory {

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		List<ITaintPropagationRule> ruleList = new ArrayList<>();

		// backwards only
		ruleList.add(new BackwardsSinkPropagationRule(manager, zeroValue, results));
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
