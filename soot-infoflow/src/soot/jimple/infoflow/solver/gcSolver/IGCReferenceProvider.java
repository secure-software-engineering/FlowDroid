package soot.jimple.infoflow.solver.gcSolver;

import java.util.Set;

/**
 * Interface for all implementations that can provide reference counting. These
 * classes answer the following question: Given an abstraction X, in which
 * abstractions can the solver transitively spawn new analysis tasks starting
 * from X?
 * 
 * @author Steven Arzt
 *
 */
public interface IGCReferenceProvider<A> {

	/**
	 * Given an abstraction, gets the set of abstractions that in which the solver
	 * can transitively spawn new analysis tasks
	 * 
	 * @param abstraction
	 * @return the set of abstractions that in which the solver can transitively
	 *         spawn new analysis tasks
	 */
	public Set<A> getAbstractionReferences(A abstraction);

}
