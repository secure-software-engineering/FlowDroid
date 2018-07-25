package soot.jimple.infoflow.solver.ngsolver;

import heros.solver.PathEdge;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;

public class SolverState<N, D> {

	protected final N target;
	protected final D sourceVal;
	protected final D targetVal;
	protected final MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctions;

	public SolverState(D sourceVal, N target, D targetVal) {
		this(sourceVal, target, targetVal, new MyConcurrentHashMap<PathEdge<N, D>, D>());
	}

	private SolverState(D sourceVal, N target, D targetVal, MyConcurrentHashMap<PathEdge<N, D>, D> jumpFunctions) {
		this.sourceVal = sourceVal;
		this.target = target;
		this.targetVal = targetVal;
		this.jumpFunctions = jumpFunctions;
	}

	public SolverState<N, D> derive(N target, D targetVal) {
		return new SolverState<>(this.sourceVal, target, targetVal, this.jumpFunctions);
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
		result = prime * result + ((jumpFunctions == null) ? 0 : jumpFunctions.hashCode());
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
		if (jumpFunctions == null) {
			if (other.jumpFunctions != null)
				return false;
		} else if (!jumpFunctions.equals(other.jumpFunctions))
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
