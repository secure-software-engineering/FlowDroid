package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.data.ContainerContext;

public class InvalidateShift implements IShiftOperation {
    @Override
    public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
        return UnknownContext.v();
    }
}
