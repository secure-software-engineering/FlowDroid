package soot.jimple.infoflow.solver;

import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.infoflow.solver.fastSolver.ISchedulingStrategy;

/**
 * Common interface for IFDS solvers that schedule their IFDS edge processing
 * tasks based on interchangeable strategies
 * 
 * @author Steven Arzt
 *
 */
public interface IStrategyBasedParallelSolver<N, D extends FastSolverLinkedNode<D, N>> {

	/**
	 * Sets the strategy for scheduling edges
	 * 
	 * @param strategy The strategy for scheduling edges
	 */
	public void setSchedulingStrategy(ISchedulingStrategy<N, D> strategy);

}
