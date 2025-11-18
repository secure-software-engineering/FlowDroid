package soot.jimple.infoflow.problems.rules;

import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.backward.BackwardsArrayPropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsClinitRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsExceptionPropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsImplicitFlowRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsSinkPropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsSourcePropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsStrongUpdatePropagationRule;
import soot.jimple.infoflow.problems.rules.backward.BackwardsWrapperRule;
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
		List<Class<? extends ITaintPropagationRule>> ruleList = new ArrayList<>();

		// backwards only
		ruleList.add(BackwardsSinkPropagationRule.class);
		ruleList.add(BackwardsSourcePropagationRule.class);
		ruleList.add(BackwardsClinitRule.class);
		ruleList.add(BackwardsStrongUpdatePropagationRule.class);
		if (manager.getConfig().getEnableExceptionTracking())
			ruleList.add(BackwardsExceptionPropagationRule.class);
		if (manager.getConfig().getEnableArrayTracking())
			ruleList.add(BackwardsArrayPropagationRule.class);
		if (manager.getTaintWrapper() != null)
			ruleList.add(BackwardsWrapperRule.class);

		// shared
		ruleList.add(SkipSystemClassRule.class);
		if (manager.getConfig().getStopAfterFirstKFlows() > 0)
			ruleList.add(StopAfterFirstKFlowsPropagationRule.class);

		if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
			ruleList.add(BackwardsImplicitFlowRule.class);

		return new PropagationRuleManager(manager, zeroValue, results, ruleList);
	}

}
