package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.ContainerContext;

public class PreciseShift implements IShiftOperation {
    @Override
    public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
        if (ctxt instanceof IntervalContext) {
            IntervalContext shifted = ((IntervalContext) ctxt).exactShift(new IntervalContext(n));
            return exact ? shifted : ((IntervalContext) ctxt).union(shifted);
        }

        throw new RuntimeException("Expect interval context but got: " + ctxt.getClass().getName());
    }
}
