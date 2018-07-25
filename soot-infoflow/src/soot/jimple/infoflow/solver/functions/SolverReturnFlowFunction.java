package soot.jimple.infoflow.solver.functions;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import soot.jimple.infoflow.solver.ngsolver.FlowFunction;
import soot.jimple.infoflow.solver.ngsolver.SolverState;

/**
 * A special implementation of the return flow function that allows access to
 * the facts associated with the start points of the callees (i.e. the contexts
 * to which we return).
 * 
 * @author Steven Arzt
 */
public interface SolverReturnFlowFunction<N, D> extends FlowFunction<N, D> {

	@Override
	public default Set<D> computeTargets(SolverState<N, D> state) {
		return computeTargets(state, Collections.<D>emptySet());
	}

	/**
	 * Computes the abstractions at the return site.
	 * 
	 * @param source    The abstraction at the exit node
	 * @param calleeD1  The abstraction at the start point of the callee
	 * @param callerD1s The abstractions at the start nodes of all methods to which
	 *                  we return (i.e. the contexts to which this flow function
	 *                  will be applied).
	 * @return The set of abstractions at the return site.
	 */
	public abstract Set<D> computeTargets(SolverState<N, D> state, Collection<D> callerD1s);

}
