package soot.jimple.infoflow.solver;

import heros.DontSynchronize;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Common superclass of all IFDS solvers.
 *
 * @param <N> node
 * @param <D> data flow fact
 */
public class AbstractIFDSSolver<N, D extends FastSolverLinkedNode<D, N>> {
    @DontSynchronize("readOnly")
    protected PredecessorShorteningMode shorteningMode = PredecessorShorteningMode.NeverShorten;

    /**
     * Sets whether abstractions on method returns shall be connected to the
     * respective call abstractions to shortcut paths.
     *
     * @param mode The strategy to use for shortening predecessor paths
     */
    public void setPredecessorShorteningMode(PredecessorShorteningMode mode) {
        this.shorteningMode = mode;
    }

    /**
     * Shortens the predecessors of the first argument if configured.
     *
     * @param returnD data flow fact leaving the method, which predecessor chain should be shortened
     * @param incomingD incoming data flow fact at the call site
     * @param calleeD first data flow fact in the callee, i.e. with current statement == call site
     * @return data flow fact with a shortened predecessor chain
     */
    protected D shortenPredecessors(D returnD, D incomingD, D calleeD, N currentUnit, N callSite) {
        switch (shorteningMode) {
            case AlwaysShorten:
                // If we don't build a path later, we do not have to keep flows through callees.
                // But we have to keep the call site so that we do not lose any neighbors, but
                // skip any abstractions inside the callee. This sets the predecessor of the
                // returned abstraction to the first abstraction in the callee.
                if (returnD != calleeD) {
                    D res = returnD.clone(currentUnit, callSite);
                    res.setPredecessor(calleeD);
                    return res;
                }
                break;
            case ShortenIfEqual:
                if (returnD.equals(incomingD))
                    return incomingD;
                break;
        }

        return returnD;
    }
}
