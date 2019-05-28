package soot.jimple.infoflow.solver.gcSolver;

import java.util.Set;

import soot.SootMethod;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Interface for all implementations that can provide reference counting. These
 * classes answer the following question: Given a method X, in which methods can
 * the solver transitively spawn new analysis tasks starting from X?
 * 
 * @author Steven Arzt
 *
 */
public interface IGCReferenceProvider<D, N> {

	/**
	 * Given a method and a context, gets the set of methods that in which the
	 * solver can transitively spawn new analysis tasks
	 * 
	 * @param method
	 * @param context
	 * @return
	 */
	public Set<SootMethod> getMethodReferences(SootMethod method, FastSolverLinkedNode<D, N> context);

}
