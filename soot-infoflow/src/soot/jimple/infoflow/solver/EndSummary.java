package soot.jimple.infoflow.solver;

import java.util.Objects;

import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * A data class for end summaries that are computed once a method has been
 * processed so that the data flows through that method can be re-used
 * 
 * @author Steven Arzt
 *
 * @param <N> The type for statements in the CFG
 * @param <D> The type of the data flow abstraction
 */
public class EndSummary<N, D extends FastSolverLinkedNode<D, N>> {

	/**
	 * The exit point of the callee to which the summary applies
	 */
	public N eP;

	/**
	 * The taint abstraction at eP
	 */
	public D d4;

	/**
	 * The abstraction at the beginning of the callee
	 */
	public D calleeD1;

	public EndSummary(N eP, D d4, D calleeD1) {
		this.eP = eP;
		this.d4 = d4;
		this.calleeD1 = calleeD1;
	}

	@Override
	public int hashCode() {
		return Objects.hash(calleeD1, d4, eP);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndSummary other = (EndSummary) obj;
		return Objects.equals(calleeD1, other.calleeD1) && Objects.equals(d4, other.d4) && Objects.equals(eP, other.eP);
	}

}
