package soot.jimple.infoflow.collections.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.problems.rules.forward.ArrayWithIndexPropagationRule;
import soot.jimple.infoflow.collections.problems.rules.forward.CollectionWrapperPropagationRule;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.DefaultPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.problems.rules.forward.*;
import soot.jimple.infoflow.problems.rules.ITaintPropagationRule;

/**
 * Use with collections to correctly approximate lists
 *
 * @author Tim Lange
 */
public class CollectionRulePropagationManagerFactory extends DefaultPropagationRuleManagerFactory {
    @Override
    public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
                                                    TaintPropagationResults results) {
        PropagationRuleManager rm = super.createRuleManager(manager, zeroValue, results);
        ITaintPropagationRule[] rules = rm.getRules();
        for (int i = 0; i < rules.length; i++) {
            if (rules[i] instanceof ArrayPropagationRule)
                rules[i] = new ArrayWithIndexPropagationRule(manager, zeroValue, results);
            if (rules[i] instanceof WrapperPropagationRule)
                rules[i] = new CollectionWrapperPropagationRule(manager, zeroValue, results);
        }

        return rm;
    }
}
