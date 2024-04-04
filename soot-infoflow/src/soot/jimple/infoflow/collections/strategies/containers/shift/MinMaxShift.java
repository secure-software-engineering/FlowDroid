package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.ContainerContext;

public class MinMaxShift implements IShiftOperation {
    @Override
    public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
        if (ctxt instanceof IntervalContext) {
            int min = n < 0 ? 0 : ((IntervalContext) ctxt).getMin();
            int max = n > 0 ? Integer.MAX_VALUE : ((IntervalContext) ctxt).getMax();
            return new IntervalContext(min, max);
        }

        throw new RuntimeException("Expect interval context but got: " + ctxt.getClass().getName());
    }
}
