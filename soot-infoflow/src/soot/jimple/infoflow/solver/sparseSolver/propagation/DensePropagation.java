package soot.jimple.infoflow.solver.sparseSolver.propagation;

import java.util.Collection;

import soot.SootMethod;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Propagation strategy delegating the work to the ICFG, i.e. propagating dense
 *
 * @param <N> Statements
 * @param <D> Facts
 * @param <I> Interprocedural control-flow graph
 *
 * @author Tim Lange
 */
public class DensePropagation<N, D extends FastSolverLinkedNode<D, N>, I extends BiDiInterproceduralCFG<N, SootMethod>>
        implements IPropagationStrategy<N, D, I> {
    private final I iCfg;

    public DensePropagation(I iCfg) {
        this.iCfg = iCfg;
    }

    @Override
    public Collection<N> getSuccsOf(N n, D d) {
        return iCfg.getSuccsOf(n);
    }

    @Override
    public Collection<N> getStartPointsOf(SootMethod sm, D d) {
        return iCfg.getStartPointsOf(sm);
    }
}
