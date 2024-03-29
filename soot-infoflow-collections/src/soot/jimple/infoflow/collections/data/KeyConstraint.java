package soot.jimple.infoflow.collections.data;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.summary.ImplicitLocation;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

public class KeyConstraint extends FlowConstraint {
    public KeyConstraint(SourceSinkType type, int paramIdx, String baseType, AccessPathFragment accessPathFragment) {
        super(type, paramIdx, baseType, accessPathFragment);
    }

    @Override
    public boolean isIndexBased() {
        return false;
    }

    @Override
    public ImplicitLocation getImplicitLocation() {
        throw new IllegalStateException("Check isIndex first!");
    }
}
