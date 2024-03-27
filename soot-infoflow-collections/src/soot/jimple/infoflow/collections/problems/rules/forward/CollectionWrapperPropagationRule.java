package soot.jimple.infoflow.collections.problems.rules.forward;

import java.util.HashSet;
import java.util.Set;

import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.cfg.FlowDroidSourceStatement;
import soot.jimple.infoflow.collections.taintWrappers.ICollectionsSupport;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.forward.WrapperPropagationRule;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule that is specifically suited for Taint Wrappers with ICollectionsSupport
 *
 * @author Tim Lange
 */
public class CollectionWrapperPropagationRule extends WrapperPropagationRule {

    public CollectionWrapperPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    /**
     * Computes the taints produced by a taint wrapper object
     *
     * @param d1     The context (abstraction at the method's start node)
     * @param iStmt  The call statement the taint wrapper shall check for well-
     *               known methods that introduce black-box taint propagation
     * @param source The taint source
     * @return The taints computed by the wrapper
     */
    protected Set<Abstraction> computeWrapperTaints(Abstraction d1, final Stmt iStmt, Abstraction source,
                                                  ByReferenceBoolean killSource) {
        // Do not process zero abstractions
        if (source == getZeroValue())
            return null;

        // If we don't have a taint wrapper, there's nothing we can do here
        if (getManager().getTaintWrapper() == null)
            return null;

        // Do not check taints that are not mentioned anywhere in the call
        final Aliasing aliasing = getAliasing();
        if (aliasing != null && !source.getAccessPath().isStaticFieldRef() && !source.getAccessPath().isEmpty()) {
            boolean found = false;
            // The base object must be tainted
            Local base = null;
            if (iStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) iStmt.getInvokeExpr();
                base = (Local) iiExpr.getBase();
                found = aliasing.mayAlias(base, source.getAccessPath().getPlainValue());
            }

            // or one of the parameters must be tainted
            if (!found)
                for (int paramIdx = 0; paramIdx < iStmt.getInvokeExpr().getArgCount(); paramIdx++)
                    if (aliasing.mayAlias(source.getAccessPath().getPlainValue(),
                            iStmt.getInvokeExpr().getArg(paramIdx))) {
                        found = true;
                        break;
                    }

            if (!found && base != null && manager.getTaintWrapper() instanceof ICollectionsSupport) {
                Set<Abstraction> approx = ((ICollectionsSupport) manager.getTaintWrapper()).getTaintsForMethodApprox(iStmt, d1, source);
                // null means there's nothing to update, so leave it to the flow function to apply the identity
                if (approx == null)
                    return null;

                // Assume our approximation did produce some taint and use that as a result
                killSource.value = true;
                return approx;
            }

            // If nothing is tainted, we don't have any taints to propagate
            if (!found)
                return null;
        }

        // Do not apply the taint wrapper to statements that are sources on their own
        if (!getManager().getConfig().getInspectSources()) {
            // Check whether this can be a source at all
            if (iStmt.hasTag(FlowDroidSourceStatement.TAG_NAME))
                return null;
        }

        Set<Abstraction> res = getManager().getTaintWrapper().getTaintsForMethod(iStmt, d1, source);

        if (res != null) {
            Set<Abstraction> resWithAliases = new HashSet<>(res);
            for (Abstraction abs : res) {
                // The new abstraction gets activated where it was generated
                if (!abs.equals(source))
                    checkAndPropagateAlias(d1, iStmt, resWithAliases, abs);
            }
            res = resWithAliases;
        }

        // We assume that a taint wrapper returns the complete set of taints for exclusive methods. Thus, if the
        // incoming taint should be kept alive, the taint wrapper needs to add it to the outgoing set.
        killSource.value = manager.getTaintWrapper() != null && manager.getTaintWrapper().isExclusive(iStmt, source);

        return res;
    }

    /**
     * Starts alias tracking for the results of a taint wrapper if necessary
     *
     * @param d1             The context in which the wrapped method was called
     * @param iStmt          The call site
     * @param resWithAliases The resulting taint abstractions generated by the
     *                       wrapper
     * @param abs            The original incoming taint abstraction
     */
    protected void checkAndPropagateAlias(Abstraction d1, final Stmt iStmt, Set<Abstraction> resWithAliases,
                                          Abstraction abs) {
        // If the taint wrapper creates a new taint, this must be propagated
        // backwards as there might be aliases for the base object
        // Note that we don't only need to check for heap writes such as a.x = y,
        // but also for base object taints ("a" in this case).
        final AccessPath val = abs.getAccessPath();
        boolean isBasicString = TypeUtils.isStringType(val.getBaseType()) && !val.getCanHaveImmutableAliases()
                && !getAliasing().isStringConstructorCall(iStmt);
        boolean taintsObjectValue = val.getBaseType() instanceof RefType
                && abs.getAccessPath().getBaseType() instanceof RefType && !isBasicString;
        boolean taintsStaticField = getManager().getConfig()
                .getStaticFieldTrackingMode() != StaticFieldTrackingMode.None && abs.getAccessPath().isStaticFieldRef();

        // If the tainted value gets overwritten, it cannot have aliases afterwards
        boolean taintedValueOverwritten = (iStmt instanceof DefinitionStmt)
                ? Aliasing.baseMatches(((DefinitionStmt) iStmt).getLeftOp(), abs)
                : false;

        if (!taintedValueOverwritten) {
            if (taintsStaticField || (taintsObjectValue && abs.getAccessPath().getTaintSubFields())
                    || manager.getAliasing().canHaveAliases(iStmt, val.getCompleteValue(), abs)) {
                // Filter out context changes that are no new aliases
                HashSet<Abstraction> aliases = new HashSet<>();
                for (Abstraction a : resWithAliases) {
                    // Sometimes we have to invalidate keys or shift an index. In that case, there is no need
                    // to search for a new alias. We prevent this here.
                    boolean contextChange = abs.equalsWithoutContext(a);
                    if (!contextChange)
                        aliases.add(a);
                }

                if (!aliases.isEmpty())
                    getAliasing().computeAliases(d1, iStmt, val.getPlainValue(), aliases,
                        getManager().getICFG().getMethodOf(iStmt), abs);
            }
        }
    }
}
