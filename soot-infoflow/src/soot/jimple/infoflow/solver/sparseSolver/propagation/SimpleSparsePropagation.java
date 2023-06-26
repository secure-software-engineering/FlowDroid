package soot.jimple.infoflow.solver.sparseSolver.propagation;

import java.util.Collection;
import java.util.stream.Collectors;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;

/**
 * Sparse propagation based on (Unit, (Local|Field), FlowSensitivityUnit)-triples
 *
 * @author Tim Lange
 */
public class SimpleSparsePropagation extends AbstractSparsePropagation {
    /**
     * Key for lookup of a local in the sparse control flow graph
     */
    private class LocalNode extends SCFGNode {
        LocalNode(Unit unit, Abstraction abs) {
            super(unit, abs.getAccessPath().getPlainValue(), abs.getAccessPath().getFirstField(),
                    abs.getActivationUnit(), abs.getTurnUnit());
        }

        @Override
        protected boolean isAffectedByInternal(Unit unit) {
            Stmt stmt = (Stmt) unit;
            Aliasing aliasing = problem.getManager().getAliasing();
            if (aliasing == null)
                return stmt.getUseAndDefBoxes().stream().anyMatch(vb -> vb.getValue() == local);
            else
                return stmt.getUseBoxes().stream().anyMatch(vb -> vb.getValue() == local)
                        || stmt.getDefBoxes().stream().anyMatch(vb -> vb.getValue() instanceof Local
                        && aliasing.mustAlias(local, (Local) vb.getValue(), (Stmt) unit));
        }
    }

    /**
     * Key for lookup of a static vars in the sparse control flow graph
     */
    private class StaticNode extends SCFGNode {
        StaticNode(Unit unit, Abstraction abs) {
            super(unit, null, abs.getAccessPath().getFirstField(), abs.getActivationUnit(), abs.getTurnUnit());
            assert abs.getAccessPath().isStaticFieldRef();
        }

        @Override
        protected boolean isAffectedByInternal(Unit unit) {
            Stmt stmt = (Stmt) unit;
            // Special case: backward clinit handling depends on the clinit call edge from NewExpr
            if (turnUnit != null && stmt instanceof DefinitionStmt
                    && ((DefinitionStmt) stmt).getRightOp() instanceof NewExpr)
                return true;

            // Every call site has to be visited for static fields
            return stmt.containsInvokeExpr() || (stmt.containsFieldRef() && stmt.getFieldRef().getField() == field);
        }
    }

    public SimpleSparsePropagation(AbstractInfoflowProblem problem) {
        super(problem);
    }

    @Override
    public Collection<Unit> getSuccsOf(Unit unit, Abstraction abstraction) {
        assert !abstraction.getAccessPath().isEmpty();

        if (abstraction.getExceptionThrown())
            return getSuccessors(new SCFGExceptionNode(unit, abstraction));

        if (abstraction.getAccessPath().isStaticFieldRef())
            return getSuccessors(new StaticNode(unit, abstraction));

        return getSuccessors(new LocalNode(unit, abstraction));
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sm, Abstraction abstraction) {
        assert !abstraction.getAccessPath().isEmpty();

        if (abstraction.getExceptionThrown())
            return iCfg.getStartPointsOf(sm).stream()
                    .flatMap(sP -> getSuccessors(new SCFGExceptionNode(sP, abstraction)).stream())
                    .collect(Collectors.toSet());

        if (abstraction.getAccessPath().isStaticFieldRef())
            return iCfg.getStartPointsOf(sm).stream()
                    .flatMap(sP -> getSuccessors(new StaticNode(sP, abstraction)).stream())
                    .collect(Collectors.toSet());

        return iCfg.getStartPointsOf(sm).stream()
                .flatMap(sP -> getSuccessors(new LocalNode(sP, abstraction)).stream())
                .collect(Collectors.toSet());
    }
}
