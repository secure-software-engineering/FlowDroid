package soot.jimple.infoflow.solver.sparseSolver.propagation;

import java.util.Collection;
import java.util.stream.Collectors;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;

/**
 * Precise sparse propagation based on (Unit, Local, Field, FlowSensitivityUnit)-quadruple
 *
 * @author Tim Lange
 */
public class PreciseSparsePropagation extends AbstractSparsePropagation {
    /**
     * Key for lookup in the sparse control flow graph
     */
    private class NormalSCFGNode extends SCFGNode {
        NormalSCFGNode(Unit unit, Abstraction abs) {
            // Special case: If the access path depends on a cut access path, we might not be able to match
            //               the field correctly and thus refrain from trying to anticipate the field here.
            super(unit, abs.getAccessPath().getPlainValue(),
                    abs.getAccessPath().getFirstField(),
                    abs.getActivationUnit(), abs.getTurnUnit());
        }

        protected boolean isAffectedByInternal(Unit unit) {
            // Identity statement usually don't affect the flow unless they are exits. Though, if parameters
            // used as sources or to throw exceptions, the backward direction might need them.
            if (turnUnit == null && unit instanceof IdentityStmt)
                return false;

            // Check for normal flows
            if (unit instanceof DefinitionStmt) {
                DefinitionStmt def = (DefinitionStmt) unit;
                return matchesLhs(def.getLeftOp(), def, def instanceof AssignStmt)
                        || matchesRhs(def.getRightOp());
            }

            // Check for calls/call-to-returns
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr())
                return isPartOfCall(local, stmt.getInvokeExpr());

            if (stmt instanceof IfStmt)
                return matchesRhs(((IfStmt) stmt).getCondition());

            return false;
        }

        private boolean matchesLhs(Value lhs, DefinitionStmt stmt, boolean mustAlias) {
            // Another special case: The StrongUpdateRule performs strong updates based on the must alias
            // analysis of Soot. We have to also query the analysis to find out whether the statement is a
            // successor or not. Though, this only is the case for LHS of assignments.
            Aliasing aliasing = mustAlias ? problem.getManager().getAliasing() : null;

            if (lhs instanceof StaticFieldRef) {
                // Even though static field refs could also have base expansions, they are
                // not relevant here because jimple does only allow one field ref per statement
                return local == null && ((StaticFieldRef) lhs).getField() == field;
            } else if (lhs instanceof InstanceFieldRef) {
                InstanceFieldRef ref = ((InstanceFieldRef) lhs);
                // Early return if the base doesn't match
                if (aliasing == null ? local != ref.getBase()
                        : local == null || !aliasing.mustAlias(local, (Local) ref.getBase(), stmt))
                    return false;

                // Either we have no field to match, or the field matches perfectly
                if (field == null || field == ref.getField())
                    return true;

                // If the access path is a cut-off approximation, we have to check whether there might be
                // registered a base that matches.
                return matchesRegisteredBase(ref.getField());
            } else if (lhs instanceof ArrayRef) {
                ArrayRef arrayRef = (ArrayRef) lhs;
                return arrayRef.getBase() == local || arrayRef.getIndex() == local;
            } else if (lhs instanceof Local) {
                // Special case: backward clinit handling depends on the clinit call edge from NewExpr
                if (turnUnit != null && local == null && stmt.getRightOp() instanceof NewExpr)
                    return true;
                return aliasing == null ? local == lhs : local != null && aliasing.mustAlias(local, (Local) lhs, stmt);
            } else {
                throw new RuntimeException("Unexpected lhs: !" + lhs.getClass().getName());
            }
        }

        private boolean matchesRhs(Value rhs) {
            if (rhs instanceof StaticFieldRef) {
                // Even though static field refs could also have base expansions, they are
                // not relevant here because jimple does only allow one field ref per statement
                return local == null && ((StaticFieldRef) rhs).getField() == field;
            } else if (rhs instanceof InstanceFieldRef) {
                InstanceFieldRef ref = ((InstanceFieldRef) rhs);
                // Early return if the base doesn't match
                if (local != ref.getBase())
                    return false;

                // Either we have no field to match, or the field matches perfectly
                if (field == null || field == ref.getField())
                    return true;

                // If the access path is a cut-off approximation, we have to check whether there might be
                // registered a base that matches.
                return matchesRegisteredBase(ref.getField());
            } else if (rhs instanceof InvokeExpr) {
                return isPartOfCall(local, (InvokeExpr) rhs);
            } else if (rhs instanceof CastExpr) {
                return ((CastExpr) rhs).getOp() == local;
            } else if (rhs instanceof UnopExpr) {
                return ((UnopExpr) rhs).getOp() == local;
            } else if (rhs instanceof BinopExpr) {
                BinopExpr binop = (BinopExpr) rhs;
                return binop.getOp1() == local || binop.getOp2() == local;
            } else if (rhs instanceof ArrayRef) {
                ArrayRef arrayRef = (ArrayRef) rhs;
                return arrayRef.getBase() == local || arrayRef.getIndex() == local;
            } else if (rhs instanceof Local) {
                return local == rhs;
            } else if (rhs instanceof NewArrayExpr) {
                return ((NewArrayExpr) rhs).getSize() == local;
            } else if (rhs instanceof NewMultiArrayExpr) {
                for (Value size : ((NewMultiArrayExpr) rhs).getSizes())
                    if (size == local)
                        return true;
                return false;
            } else if (rhs instanceof InstanceOfExpr) {
                return ((InstanceOfExpr) rhs).getOp() == local;
            } else if (rhs instanceof Constant || rhs instanceof NewExpr
                        || rhs instanceof ParameterRef || rhs instanceof ThisRef
                        || rhs instanceof CaughtExceptionRef) {
                return false;
            } else {
                throw new RuntimeException("Unexpected rhs: !" + rhs.getClass().getName());
            }
        }

        private boolean isPartOfCall(Local local, InvokeExpr ie) {
            // Static variables are always part of the call
            if (local == null)
                return true;

            for (int i = 0; i < ie.getArgCount(); i++)
                if (ie.getArg(i) == local)
                    return true;

            return ie instanceof InstanceInvokeExpr && ((InstanceInvokeExpr) ie).getBase() == local;
        }

        private boolean matchesRegisteredBase(SootField field) {
            Collection<AccessPathFragment[]> bases = problem.getManager().getAccessPathFactory().getBaseForType(local.getType());
            if (bases != null) {
                synchronized (bases) {
                    for (AccessPathFragment[] base : bases) {
                        if (base[0].getField() == field)
                            return true;
                    }
                }
            }
            return false;
        }
    }

    public PreciseSparsePropagation(AbstractInfoflowProblem problem) {
        super(problem);
    }

    @Override
    public Collection<Unit> getSuccsOf(Unit unit, Abstraction abstraction) {
        assert !abstraction.getAccessPath().isEmpty();

        if (abstraction.getExceptionThrown())
            return getSuccessors(new SCFGExceptionNode(unit, abstraction));

        return getSuccessors(new NormalSCFGNode(unit, abstraction));
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sm, Abstraction abstraction) {
        assert !abstraction.getAccessPath().isEmpty();

        if (abstraction.getExceptionThrown())
            return iCfg.getStartPointsOf(sm).stream()
                    .flatMap(sP -> getSuccessors(new SCFGExceptionNode(sP, abstraction)).stream())
                    .collect(Collectors.toSet());

        return iCfg.getStartPointsOf(sm).stream()
                .flatMap(sP -> getSuccessors(new NormalSCFGNode(sP, abstraction)).stream())
                .collect(Collectors.toSet());
    }
}
