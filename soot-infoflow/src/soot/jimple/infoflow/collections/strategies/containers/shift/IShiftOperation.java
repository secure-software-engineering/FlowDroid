package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.data.ContainerContext;

public interface IShiftOperation {
    ContainerContext shift(ContainerContext ctxt, int n, boolean exact);
}
