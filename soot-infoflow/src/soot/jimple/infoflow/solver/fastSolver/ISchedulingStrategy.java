package soot.jimple.infoflow.solver.fastSolver;

/**
 * Common interface for all edge scheduling strategies in the solver
 * 
 * @author Steven Arzt
 *
 */
public interface ISchedulingStrategy<N, D extends FastSolverLinkedNode<D, N>> {

	public void propagateInitialSeeds(D sourceVal, N target, D targetVal, N relatedCallSite,
			boolean isUnbalancedReturn);

	public void propagateNormalFlow(D sourceVal, N target, D targetVal, N relatedCallSite, boolean isUnbalancedReturn);

	public void propagateCallFlow(D sourceVal, N target, D targetVal, N relatedCallSite, boolean isUnbalancedReturn);

	public void propagateCallToReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
			boolean isUnbalancedReturn);

	public void propagateReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite, boolean isUnbalancedReturn);

}
