package soot.jimple.infoflow.collections.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.problems.rules.forward.ArrayWithIndexPropagationRule;
import soot.jimple.infoflow.collections.problems.rules.forward.CollectionWrapperPropagationRule;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.problems.rules.forward.ArrayPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.ExceptionPropagationRule;
import soot.jimple.infoflow.problems.rules.ITaintPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.ImplicitPropagtionRule;
import soot.jimple.infoflow.problems.rules.forward.SinkPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.SkipSystemClassRule;
import soot.jimple.infoflow.problems.rules.forward.SourcePropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StaticPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StopAfterFirstKFlowsPropagationRule;
import soot.jimple.infoflow.problems.rules.forward.StrongUpdatePropagationRule;
import soot.jimple.infoflow.problems.rules.forward.TypingPropagationRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Use with collections to correctly approximate lists
 *
 * @author Tim Lange
 */
public class CollectionRulePropagationManagerFactory implements IPropagationRuleManagerFactory {
    @Override
    public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
                                                    TaintPropagationResults results) {
        List<ITaintPropagationRule> ruleList = new ArrayList<>();

        ruleList.add(new SourcePropagationRule(manager, zeroValue, results));
        ruleList.add(new SinkPropagationRule(manager, zeroValue, results));
        ruleList.add(new StaticPropagationRule(manager, zeroValue, results));

        if (manager.getConfig().getEnableArrayTracking())
            ruleList.add(new ArrayWithIndexPropagationRule(manager, zeroValue, results));
        if (manager.getConfig().getEnableExceptionTracking())
            ruleList.add(new ExceptionPropagationRule(manager, zeroValue, results));
        if (manager.getTaintWrapper() != null)
            ruleList.add(new CollectionWrapperPropagationRule(manager, zeroValue, results));
        if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
            ruleList.add(new ImplicitPropagtionRule(manager, zeroValue, results));
        ruleList.add(new StrongUpdatePropagationRule(manager, zeroValue, results));
        if (manager.getConfig().getEnableTypeChecking())
            ruleList.add(new TypingPropagationRule(manager, zeroValue, results));
        ruleList.add(new SkipSystemClassRule(manager, zeroValue, results));
        if (manager.getConfig().getStopAfterFirstKFlows() > 0)
            ruleList.add(new StopAfterFirstKFlowsPropagationRule(manager, zeroValue, results));

        return new PropagationRuleManager(manager, zeroValue, results,
                ruleList.toArray(new ITaintPropagationRule[0]));
    }
}
