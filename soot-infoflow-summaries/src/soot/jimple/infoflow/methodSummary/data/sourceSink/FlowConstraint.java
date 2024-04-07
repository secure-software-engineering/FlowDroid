package soot.jimple.infoflow.methodSummary.data.sourceSink;

import soot.jimple.infoflow.methodSummary.data.summary.ImplicitLocation;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

import java.util.Objects;

/**
 * Describes another source that provides a constraint for a given flow
 */
public abstract class FlowConstraint {
    private final SourceSinkType type;
    private final int paramIdx;
    private final String baseType;
    private final AccessPathFragment accessPathFragment;

    public FlowConstraint(SourceSinkType type, int paramIdx, String baseType, AccessPathFragment accessPathFragment) {
        this.type = type;
        this.paramIdx = paramIdx;
        this.baseType =  baseType;
        this.accessPathFragment = accessPathFragment;
    }

    public SourceSinkType getType() {
        return type;
    }

    public int getParamIdx() {
        return paramIdx;
    }

    public String getBaseType() {
        return baseType;
    }

    public AccessPathFragment getAccessPathFragment() {
        return accessPathFragment;
    }

    public abstract boolean isIndexBased();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowConstraint that = (FlowConstraint) o;
        return paramIdx == that.paramIdx && type == that.type && Objects.equals(baseType, that.baseType) && Objects.equals(accessPathFragment, that.accessPathFragment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, paramIdx, baseType, accessPathFragment);
    }

    public abstract ImplicitLocation getImplicitLocation();
}
