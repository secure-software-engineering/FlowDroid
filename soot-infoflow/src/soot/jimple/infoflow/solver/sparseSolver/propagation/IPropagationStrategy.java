package soot.jimple.infoflow.solver.sparseSolver.propagation;

import java.util.Collection;

import soot.SootMethod;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public interface IPropagationStrategy<N, D extends FastSolverLinkedNode<D, N>, I extends BiDiInterproceduralCFG<N, SootMethod>> {
    /**
     * Get the successors of n given d
     *
     * @param n current unit
     * @param d outgoing abstraction at n
     * @return collection of successors
     */
    Collection<N> getSuccsOf(N n, D d);

    /**
     * Get the start points of sm given d
     *
     * @param sm method to be called
     * @param d  callee context abstraction
     * @return collection of successors
     */
    Collection<N> getStartPointsOf(SootMethod sm, D d);
}
