package soot.jimple.infoflow.problems.rules;

import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.problems.rules.forward.ArrayWithIndexPropagationRule;
import soot.jimple.infoflow.collections.problems.rules.forward.CollectionWrapperPropagationRule;
import soot.jimple.infoflow.config.PreciseCollectionStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.forward.ArrayPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.ExceptionPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.ImplicitPropagtionRule;
import soot.jimple.infoflow.problems.rules.forward.SinkPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.SkipSystemClassRule;
import soot.jimple.infoflow.problems.rules.forward.SourcePropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StaticPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StopAfterFirstKFlowsPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StrongUpdatePropagationRule;
import soot.jimple.infoflow.problems.rules.forward.TypingPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.WrapperPropagationRule;

/**
 * Default implementation of the {@link IPropagationRuleManagerFactory} class
 * 
 * @author Steven Arzt
 *
 */
public class DefaultPropagationRuleManagerFactory implements IPropagationRuleManagerFactory {

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		List<Class<? extends ITaintPropagationRule>> ruleList = new ArrayList<>();

		ruleList.add(SourcePropagationRule.class);
		ruleList.add(SinkPropagationRule.class);
		ruleList.add(StaticPropagationRule.class);

		boolean preciseCollectionTrackingEnabled = manager.getConfig()
				.getPreciseCollectionStrategy() != PreciseCollectionStrategy.NONE;
		if (manager.getConfig().getEnableArrayTracking()) {
			if (preciseCollectionTrackingEnabled)
				ruleList.add(ArrayWithIndexPropagationRule.class);
			else
				ruleList.add(ArrayPropagationRule.class);
		}
		if (manager.getConfig().getEnableExceptionTracking())
			ruleList.add(ExceptionPropagationRule.class);
		if (manager.getTaintWrapper() != null) {
			if (preciseCollectionTrackingEnabled)
				ruleList.add(CollectionWrapperPropagationRule.class);
			else
				ruleList.add(WrapperPropagationRule.class);
		}
		if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
			ruleList.add(ImplicitPropagtionRule.class);
		ruleList.add(StrongUpdatePropagationRule.class);
		if (manager.getConfig().getEnableTypeChecking())
			ruleList.add(TypingPropagationRule.class);
		ruleList.add(SkipSystemClassRule.class);
		if (manager.getConfig().getStopAfterFirstKFlows() > 0)
			ruleList.add(StopAfterFirstKFlowsPropagationRule.class);

		return new PropagationRuleManager(manager, zeroValue, results, ruleList);
	}

	public PropagationRuleManager createAliasRuleManager(InfoflowManager manager, Abstraction zeroValue) {
		return null;
	}
}
