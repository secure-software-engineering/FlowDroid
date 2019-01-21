package soot.jimple.infoflow.solver.ngsolver;

import heros.solver.PathEdge;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;

public class SolverState<N, D> {

	protected final N target;
	protected final D sourceVal;
	protected final D targetVal;
	protected final MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctionsForward;
	protected final MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctionsBackward;

	public SolverState(D sourceVal, N target, D targetVal) {
		this(sourceVal, target, targetVal, new MyConcurrentHashMap<PathEdge<N, D>, D>(),
				new MyConcurrentHashMap<PathEdge<N, D>, D>());
	}

	private SolverState(D sourceVal, N target, D targetVal, MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctionsForward,
			MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctionsBackward) {
		this.sourceVal = sourceVal;
		this.target = target;
		this.targetVal = targetVal;
		this.jumpFunctionsForward = jumpFunctionsForward;
		this.jumpFunctionsBackward = jumpFunctionsBackward;
	}

	/**
	 * Creates a new solver state with the same context, but a new current statement
	 * and a new target abstraction
	 * 
	 * @param target    The new statement to which to apply the state
	 * @param targetVal The new target abstraction
	 * @return The new solver state
	 */
	public SolverState<N, D> derive(N target, D targetVal) {
		if (target == this.target && targetVal == this.targetVal)
			return this;
		return new SolverState<>(this.sourceVal, target, targetVal, this.jumpFunctionsForward,
				this.jumpFunctionsBackward);
	}

	/**
	 * Creates a new solver state with the same context and current statement, but a
	 * new target abstraction
	 * 
	 * @param abs The new target abstraction
	 * @return The new solver state
	 */
	public SolverState<N, D> derive(D abs) {
		if (abs == this.targetVal)
			return this;
		return new SolverState<>(this.sourceVal, this.target, abs, this.jumpFunctionsForward,
				this.jumpFunctionsBackward);
	}

	public N getTarget() {
		return target;
	}

	public D getSourceVal() {
		return sourceVal;
	}

	public D getTargetVal() {
		return targetVal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jumpFunctionsForward == null) ? 0 : jumpFunctionsForward.hashCode());
		result = prime * result + ((jumpFunctionsBackward == null) ? 0 : jumpFunctionsBackward.hashCode());
		result = prime * result + ((sourceVal == null) ? 0 : sourceVal.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((targetVal == null) ? 0 : targetVal.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SolverState<?, ?> other = (SolverState<?, ?>) obj;
		if (jumpFunctionsForward == null) {
			if (other.jumpFunctionsForward != null)
				return false;
		} else if (!jumpFunctionsForward.equals(other.jumpFunctionsForward))
			return false;
		if (jumpFunctionsBackward == null) {
			if (other.jumpFunctionsBackward != null)
				return false;
		} else if (!jumpFunctionsBackward.equals(other.jumpFunctionsBackward))
			return false;
		if (sourceVal == null) {
			if (other.sourceVal != null)
				return false;
		} else if (!sourceVal.equals(other.sourceVal))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (targetVal == null) {
			if (other.targetVal != null)
				return false;
		} else if (!targetVal.equals(other.targetVal))
			return false;
		return true;
	}

}
