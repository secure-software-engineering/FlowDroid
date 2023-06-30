package soot.jimple.infoflow.solver.sparseSolver.propagation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.*;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Abstract class for sparse propagations
 *
 * @author Tim Lange
 */
public abstract class AbstractSparsePropagation implements IPropagationStrategy<Unit, Abstraction, BiDiInterproceduralCFG<Unit, SootMethod>> {
    /**
     * Key for lookup in the sparse control flow graph
     */
    protected abstract class SCFGNode {
        protected final Unit unit;
        protected final Local local;
        protected final SootField field;
        protected final Unit activationUnit;
        protected final Unit turnUnit;

        protected SCFGNode(Unit unit, Local local, SootField field, Unit activationUnit, Unit turnUnit) {
            this.unit = unit;
            this.local = local;
            this.field = field;
            this.activationUnit = activationUnit;
            this.turnUnit = turnUnit;
        }

        /**
         * Checks whether the unit affects the value
         *
         * @param unit current unit
         * @return true if this node is used at unit
         */
        public boolean isAffectedBy(Unit unit) {
            // To keep flow sensitivity, each fact must visit its activation unit/turn unit
            if (requiredForFlowSensitivity(unit))
                return true;

            // We always need the return flow function to decide whether and how to map the
            // fact back into the caller
            if (iCfg.isExitStmt(unit))
                return true;

            return isAffectedByInternal(unit);
        }

        /**
         * Checks whether the unit affects the value. Does not need to handle flow sensitivity and exits.
         *
         * @param unit current unit
         * @return true if this node is used at unit
         */
        protected abstract boolean isAffectedByInternal(Unit unit);

        /**
         * Checks whether the current unit is needed to keep flow sensitivity
         *
         * @param unit current unit
         * @return true if unit should be visited regardless of the values
         */
        protected boolean requiredForFlowSensitivity(Unit unit) {
            boolean forwardFlowSensitivity = activationUnit != null
                    && (activationUnit == unit || problem.isCallSiteActivatingTaint(unit, activationUnit));
            if (forwardFlowSensitivity)
                return true;

            SootMethod turnM;
            boolean backwardFlowSensitivity = turnUnit != null && (turnM = iCfg.getMethodOf(turnUnit)) != null
                    && (turnUnit == unit || iCfg.getCalleesOfCallAt(unit).stream().anyMatch(sm -> sm == turnM));
            return backwardFlowSensitivity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SCFGNode SCFGNode = (SCFGNode) o;
            return Objects.equals(unit, SCFGNode.unit)
                    && Objects.equals(local, SCFGNode.local)
                    && Objects.equals(field, SCFGNode.field)
                    && Objects.equals(activationUnit, SCFGNode.activationUnit)
                    && Objects.equals(turnUnit, SCFGNode.turnUnit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(unit, local, field, activationUnit, turnUnit);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (local != null)
                sb.append(local);
            if (local != null && field != null)
                sb.append(".");
            if (field != null)
                sb.append(field);
            sb.append(" @ ").append(unit);
            return sb.toString();
        }
    }

    protected class SCFGExceptionNode extends SCFGNode {
        protected SCFGExceptionNode(Unit unit, Abstraction abs) {
            super(unit, null, null, abs.getActivationUnit(), abs.getTurnUnit());
        }

        @Override
        protected boolean isAffectedByInternal(Unit unit) {
            if (turnUnit == null)
                return unit instanceof IdentityStmt
                        && ((IdentityStmt) unit).getRightOp() instanceof CaughtExceptionRef;
            else
                return unit instanceof InvokeStmt || unit instanceof ThrowStmt;
        }
    }

    protected final BiDiInterproceduralCFG<Unit, SootMethod> iCfg;
    protected final AbstractInfoflowProblem problem;

    // Maps nodes to successors
    private final ConcurrentHashMap<SCFGNode, Collection<Unit>> sparseCfg = new ConcurrentHashMap<>();

    protected AbstractSparsePropagation(AbstractInfoflowProblem problem) {
        this.problem = problem;
        this.iCfg = problem.interproceduralCFG();
    }

    /**
     * DFS from node towards the exit of methods. Aborts each path at the first usage and adds it as a successor
     *
     * @param node current node
     * @return collection of successor statements for node
     */
    private Collection<Unit> computeSuccessors(SCFGNode node) {
        Set<Unit> succs = new HashSet<>();

        Deque<Unit> stack = new ArrayDeque<>();
        HashSet<Unit> visited = new HashSet<>();
        for (Unit succ : iCfg.getSuccsOf(node.unit))
            stack.push(succ);
        while (!stack.isEmpty()) {
            Unit current = stack.pop();
            if (visited.add(current)) {
                Stmt stmt = (Stmt) current;
                if (node.isAffectedBy(current)) {
                    succs.add(stmt);
                } else {
                    for (Unit succ : iCfg.getSuccsOf(current)) {
                        stack.push(succ);
                    }
                }
            }
        }

        return succs;
    }

    /**
     * Returns the successors of node and caches the result for subsequent calls
     *
     * @param node current node
     * @return collection of successors
     */
    protected Collection<Unit> getSuccessors(SCFGNode node) {
        return sparseCfg.computeIfAbsent(node, this::computeSuccessors);
    }
}
