package soot.jimple.infoflow.collections.strategies.widening;

import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Widens at each shift operation
 *
 * @author Tim Lange
 */
public class WideningOnShiftOperationStrategy extends AbstractWidening {

    // Contains all subsignatures that may result in an infinite domain
    private final Set<String> shiftSigs;

    protected WideningOnShiftOperationStrategy(InfoflowManager manager, Set<String> shiftSigs) {
        super(manager);
        this.shiftSigs = shiftSigs;
    }

    @Override
    public Abstraction widen(Abstraction d2, Abstraction d3, Unit unit) {
        Stmt stmt = (Stmt) unit;
        // Only shifting can produce infinite ascending chains
        if (!stmt.containsInvokeExpr() || !isShift(d2, d3))
            return d3;

        return forceWiden(d3, unit);
    }
}
