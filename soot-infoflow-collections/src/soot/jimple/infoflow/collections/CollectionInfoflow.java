package soot.jimple.infoflow.collections;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.problems.rules.CollectionRulePropagationManagerFactory;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;

public class CollectionInfoflow extends Infoflow {
    public CollectionInfoflow(String s, boolean b, DefaultBiDiICFGFactory defaultBiDiICFGFactory) {
        super(s, b, defaultBiDiICFGFactory);
    }

    @Override
    protected IPropagationRuleManagerFactory initializeRuleManagerFactory() {
        return new CollectionRulePropagationManagerFactory();
    }
}
