package soot.jimple.infoflow.util.preanalyses;

import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

/**
 * Performs a live variables analysis on just one local. Also supports a kill unit.
 *
 * @author Tim Lange
 */
public class SingleLiveVariableAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Local>> {
    Local queryLocal;
    // The turn unit acts like a kill because reads below are
    // not relevant for the alias analysis
    Unit turnUnit;
    int runtime;

    public SingleLiveVariableAnalysis(DirectedGraph<Unit> graph, Local queryLocal, Unit turnUnit) {
        super(graph);
        this.queryLocal = queryLocal;
        this.turnUnit = turnUnit;
        long timeBefore = System.nanoTime();
        doAnalysis();
        this.runtime = (int) Math.round((System.nanoTime() - timeBefore) / 1E3);
    }

    @Override
    protected void flowThrough(FlowSet<Local> in, Unit unit, FlowSet<Local> out) {
        if (unit == turnUnit) {
            out.clear();
            return;
        }
        in.copy(out);

        for (ValueBox box : unit.getDefBoxes()) {
            if (box.getValue() == queryLocal)
                out.remove(queryLocal);
        }

        for (ValueBox box : unit.getUseBoxes()) {
            if (box.getValue() == queryLocal)
                out.add(queryLocal);
        }
    }

    @Override
    protected FlowSet<Local> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Local> in1, FlowSet<Local> in2, FlowSet<Local> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Local> in, FlowSet<Local> out) {
        in.copy(out);
    }

    public boolean canOmitAlias(Unit unit) {
        return getFlowBefore(unit).size() == 0;
    }

    /**
     * Reports the runtime in microseconds
     * @return runtime in microseconds
     */
    public int getRuntimeInMicroseconds() {
        return runtime;
    }
}
