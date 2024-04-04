package soot.jimple.infoflow.problems.rules.backward;

import heros.solver.PathEdge;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

public class BackwardsClinitRule extends AbstractTaintPropagationRule {
    public BackwardsClinitRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    private void propagateToClinit(Abstraction d1, Abstraction abs, SootMethod callee) {
        Collection<Unit> startPoints = manager.getICFG().getStartPointsOf(callee);
        // Most likely |startPoints|=1 but just to be safe
        for (Unit startPoint : startPoints)
            manager.getMainSolver().processEdge(new PathEdge<>(d1, startPoint, abs));
    }

    private boolean containsStaticField(SootMethod callee, Abstraction abs) {
        return manager.getICFG().isStaticFieldUsed(callee, abs.getAccessPath().getFirstField())
                || manager.getICFG().isStaticFieldRead(callee, abs.getAccessPath().getFirstField());
    }

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        if (!(stmt instanceof AssignStmt))
            return null;
        AssignStmt assignStmt = (AssignStmt) stmt;

        final AccessPath ap = source.getAccessPath();
        final Aliasing aliasing = getAliasing();
        if (aliasing == null)
            return null;

        Collection<SootMethod> callees = manager.getICFG().getCalleesOfCallAt(stmt);
        // Look through all callees and find the clinit call
        SootMethod callee = callees.stream().filter(c -> c.hasActiveBody()
                && c.getSubSignature().equals("void <clinit>()")).findAny().orElse(null);

        // no clinit edge -> nothing to do
        if (callee == null)
            return null;

        Value leftOp = assignStmt.getLeftOp();
        boolean leftSideMatches = Aliasing.baseMatches(BaseSelector.selectBase(leftOp, false), source);
        Value rightOp = assignStmt.getRightOp();
        Value rightVal = BaseSelector.selectBase(assignStmt.getRightOp(), false);
        SootClass declaringClassMethod = manager.getICFG().getMethodOf(assignStmt).getDeclaringClass();

        Abstraction newAbs = null;
        if (leftSideMatches && rightOp instanceof StaticFieldRef) {
            SootClass declaringClassOp = ((StaticFieldRef) rightOp).getField().getDeclaringClass();
            // If the static reference is from the same class
            // we will at least find the class NewExpr above.
            // So we wait, maybe we'll find an overwrite
            if (declaringClassMethod == declaringClassOp)
                return null;

            // This might be the last occurence of the declaring class of the static reference
            // so we need the visit the clinit method too. The clinit handling is an overapproximation
            // inherited from the default callgraph algorithm SPARK.
            AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(ap, rightVal,
                    rightVal.getType(), false);
            newAbs = source.deriveNewAbstraction(newAp, stmt);
        }
        else if (ap.isStaticFieldRef() && rightOp instanceof NewExpr) {
            SootClass declaringClassOp = ((NewExpr) rightOp).getBaseType().getSootClass();
            // The NewExpr is in its own class, so we find at least another NewExpr of this kind.
            if (declaringClassMethod == declaringClassOp)
                return null;

            // In static blocks any statement can be inside also static field of other classes
            // so we also have to look into it for a possible use.
            newAbs = source.deriveNewAbstraction(source.getAccessPath(), stmt);
        }

        if (newAbs != null && containsStaticField(callee, newAbs)) {
            newAbs.setCorrespondingCallSite(assignStmt);
            propagateToClinit(d1, newAbs, callee);
        }

        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        // This kills all taints returning from the manual injection of the edge to clinit.
        Stmt callStmt = source.getCorrespondingCallSite();
        if (manager.getICFG().getMethodOf(stmt).getSubSignature().equals("void <clinit>()")
                && callStmt instanceof AssignStmt && ((AssignStmt) callStmt).getRightOp() instanceof StaticFieldRef)
            killAll.value = true;

        return null;
    }
}
