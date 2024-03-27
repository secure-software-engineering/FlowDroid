package soot.jimple.infoflow.collections.strategies.containers;

import soot.*;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.analyses.ReadOnlyListViewAnalysis;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

import java.util.*;

/**
 * Strategy that only reasons about maps with constant keys
 *
 * @author Tim Lange
 */
public class ConstantMapStrategy implements IContainerStrategy {
    // Benign race on the counters because they are on the critical path within the data flow analysis
    private volatile long resolvedKeys;
    private volatile long unresolvedKeys;

    protected final InfoflowManager manager;

    protected final ReadOnlyListViewAnalysis itAnalysis;

    protected final List<RefType> indexTypes;

    public ConstantMapStrategy(InfoflowManager manager) {
        this.manager = manager;
        itAnalysis = new ReadOnlyListViewAnalysis(manager.getICFG());

        indexTypes = new ArrayList<>(2);
        indexTypes.add(RefType.v("java.util.List"));
        indexTypes.add(RefType.v("java.util.Queue"));
    }

    public long getResolvedKeys() {
        return resolvedKeys;
    }

    public long getUnresolvedKeys() {
        return unresolvedKeys;
    }

    @Override
    public Tristate intersect(ContainerContext apKey, ContainerContext stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof KeySetContext)
            return ((KeySetContext<?>) apKey).intersect((KeySetContext<?>) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContainerContext[] append(ContainerContext[] ctxt1, ContainerContext[] ctxt2) {
        // putAll in maps can be easily modelled as a copy operation
        // append is only needed for lists and derivatives
        return null;
    }

    @Override
    public ContainerContext getKeyContext(Value value, Stmt stmt) {
        if (value instanceof Constant) {
            resolvedKeys++;
            return new KeySetContext<>((Constant) value);
        }

        unresolvedKeys++;
        return UnknownContext.v();
    }

    /**
     * Checks whether an index should be resolved. This is a special case, where Lists and Sets share the same
     * interface (Collection), yet we do not track Sets and thus, don't need to query the index for Sets.
     *
     * @param base base local
     * @return true if index should be resovled
     */
    protected boolean shouldResolveIndex(Value base) {
        if (!(base instanceof Local))
            return false;

        // Assume only the collection class needs special checks
        if (base.getType() instanceof RefType
                && !((RefType) base.getType()).getClassName().equals("java.util.Collection"))
            return true;

        // Ask SPARK whether this could be a list or a queue
        Set<Type> types = Scene.v().getPointsToAnalysis().reachingObjects((Local) base).possibleTypes();
        FastHierarchy fh = Scene.v().getFastHierarchy();
        for (Type t : types) {
            for (RefType idxType : indexTypes)
                if (fh.canStoreType(t, idxType))
                    return true;
        }

        return false;
    }

    @Override
    public ContainerContext getIndexContext(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContainerContext getNextPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContainerContext getFirstPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContainerContext getLastPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public Tristate lessThanEqual(ContainerContext ctxt1, ContainerContext ctxt2) {
        return Tristate.MAYBE();
    }

    @Override
    public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public boolean shouldSmash(ContainerContext[] ctxts) {
        for (ContainerContext ctxt : ctxts) {
            if (ctxt.containsInformation())
                return false;
        }

        return true;
    }

    @Override
    public boolean isReadOnly(Unit unit) {
        return itAnalysis.isReadOnlyIterator(unit);
    }
}
