package soot.jimple.infoflow.collections.strategies.widening;

import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Provides the infoflow solver with information about when to widen.
 * Precondition: needs to be thread-safe
 *
 * @author Tim Lange
 */
public interface WideningStrategy<N, D extends FastSolverLinkedNode<D, N>> {
    /**
     * Widens the abstraction
     *
     * @param d2   incoming fact
     * @param d3   outgoing fact
     * @param n    program point
     * @return possibly widened fact
     */
    D widen(D d2, D d3, N n);

    /**
     * Widen the fact according to the widening strategy
     *
     * @param fact current fact
     * @param n    program point
     * @return widened fact
     */
    D forceWiden(D fact, N n);
}
