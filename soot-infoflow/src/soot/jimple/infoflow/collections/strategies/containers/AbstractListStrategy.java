package soot.jimple.infoflow.collections.strategies.containers;

import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.strategies.containers.shift.IShiftOperation;
import soot.jimple.infoflow.collections.strategies.containers.shift.MinMaxShift;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * Strategy that is the base for container strategies with list support
 *
 * @author Tim Lange
 */
public abstract class AbstractListStrategy extends ConstantMapStrategy {
    // Benign race on the counters because they are on the critical path within the data flow analysis
    private long resolvedIndices;
    private long unresolvedIndices;

    private final IShiftOperation shiftOp;

    public AbstractListStrategy(InfoflowManager manager) {
        super(manager);
        this.shiftOp = new MinMaxShift();
    }

    public AbstractListStrategy(InfoflowManager manager, IShiftOperation shiftOp) {
        super(manager);
        this.shiftOp = shiftOp;
    }

    public long getResolvedIndices() {
        return resolvedIndices;
    }

    public long getUnresolvedIndices() {
        return unresolvedIndices;
    }

    @Override
    public Tristate intersect(ContainerContext apKey, ContainerContext stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof IntervalContext)
            return ((IntervalContext) apKey).intersects((IntervalContext) stmtKey);
        if (apKey instanceof KeySetContext)
            return ((KeySetContext<?>) apKey).intersect((KeySetContext<?>) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContainerContext getIndexContext(Value value, Stmt stmt) {
        if (value instanceof IntConstant) {
            resolvedIndices++;
            return new IntervalContext(((IntConstant) value).value);
        }

        unresolvedIndices++;
        return UnknownContext.v();
    }

    @Override
    public abstract ContainerContext getNextPosition(Value value, Stmt stmt);

    @Override
    public ContainerContext getFirstPosition(Value value, Stmt stmt) {
        resolvedIndices++;
        return new IntervalContext(0);
    }

    @Override
    public abstract ContainerContext getLastPosition(Value value, Stmt stmt);

    @Override
    public Tristate lessThanEqual(ContainerContext ctxt1, ContainerContext ctxt2) {
        if (ctxt1 == UnknownContext.v() || ctxt2 == UnknownContext.v())
            return Tristate.MAYBE();

        if (ctxt1 instanceof IntervalContext && ctxt2 instanceof IntervalContext)
            return ((IntervalContext) ctxt1).lessThanEqual(((IntervalContext) ctxt2));

        throw new RuntimeException("Unknown combination of " + ctxt1.toString() + " and " + ctxt2.toString());
    }

    @Override
    public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
        return shiftOp.shift(ctxt, n, exact);
    }

    @Override
    public ContainerContext[] append(ContainerContext[] ctxt1, ContainerContext[] ctxt2) {
        // Shifting only occurs on lists
        if (ctxt1 == null || ctxt2 == null
                || ctxt1.length != ctxt2.length || ctxt1.length != 1 || !(ctxt1[0] instanceof IntervalContext))
            return null;

        ContainerContext[] newCtxt = new ContainerContext[1];
        newCtxt[0] = ((IntervalContext) ctxt1[0]).exactShift((IntervalContext) ctxt2[0]);
        return newCtxt;
    }
}
