package soot.jimple.infoflow.solver.fastSolver;

import soot.SootMethod;
import soot.jimple.infoflow.solver.fastSolver.IFDSSolver.ScheduleTarget;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Default implementations for scheduling strategies
 * 
 * @author Steven Arzt
 *
 */
public class DefaultSchedulingStrategy<N, D extends FastSolverLinkedNode<D, N>, I extends BiDiInterproceduralCFG<N, SootMethod>> {

	protected final IFDSSolver<N, D, I> solver;

	/**
	 * Strategy that schedules each edge individually, potentially in a new thread
	 */
	public final ISchedulingStrategy<N, D> EACH_EDGE_INDIVIDUALLY = new ISchedulingStrategy<N, D>() {

		@Override
		public void propagateInitialSeeds(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateNormalFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateCallFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateCallToReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		};

	};

	/**
	 * Strategy that schedules each edge that crosses a method boundary, but
	 * processes all edges inside the same method locally in the same task
	 */
	public final ISchedulingStrategy<N, D> EACH_METHOD_INDIVIDUALLY = new ISchedulingStrategy<N, D>() {

		@Override
		public void propagateInitialSeeds(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateNormalFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateCallFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		}

		@Override
		public void propagateCallToReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR);
		};

	};

	/**
	 * Strategy that never schedules a new task, i.e., processes all edges locally
	 * in same task
	 */
	public final ISchedulingStrategy<N, D> ALL_EDGES_LOCALLY = new ISchedulingStrategy<N, D>() {

		@Override
		public void propagateInitialSeeds(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateNormalFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateCallFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateCallToReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		}

		@Override
		public void propagateReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, ScheduleTarget.LOCAL);
		};

	};

	/**
	 * Creates a new instance of the {@link DefaultSchedulingStrategy} class
	 * 
	 * @param solver The solver on which to schedule the edges
	 */
	public DefaultSchedulingStrategy(IFDSSolver<N, D, I> solver) {
		this.solver = solver;
	}

}
