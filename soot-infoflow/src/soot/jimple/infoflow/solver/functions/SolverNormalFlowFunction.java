package soot.jimple.infoflow.solver.functions;

import heros.FlowFunction;

import java.util.Set;

import soot.jimple.infoflow.data.Abstraction;

/**
 * A special implementation of the normal flow function that allows access to
 * the fact associated with the method's start point (i.e. the current context).
 *  
 * @author Steven Arzt
 */
public abstract class SolverNormalFlowFunction implements FlowFunction<Abstraction> {

	@Override
	public Set<Abstraction> computeTargets(Abstraction source) {
		return computeTargets(null, source);
	}

	/**
	 * Computes the abstractions at the next node in the CFG.
	 * @param d1 The abstraction at the beginning of the current method, i.e.
	 * the context
	 * @param d2 The abstraction at the current node
	 * @return The set of abstractions at the next node
	 */
	public abstract Set<Abstraction> computeTargets(Abstraction d1, Abstraction d2);
	
}
