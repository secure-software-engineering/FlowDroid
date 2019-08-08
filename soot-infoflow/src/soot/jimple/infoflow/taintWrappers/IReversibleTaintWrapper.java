package soot.jimple.infoflow.taintWrappers;

import java.util.Set;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

/**
 * While a normal {@link ITaintPropagationWrapper} answers the question which
 * taints are valid after a given statement has been called with a given
 * incoming taint abstraction, a {@link IReversibleTaintWrapper} can also
 * identify those taint that were possibly tainted before a given statement if
 * the result of that statement is a given taint.
 * 
 * Note that reversing a taint wrapper will very likely be less precise than
 * computing it forward, because many methods combine taint from multiple
 * sources. When reversing, we have to assume that all those previous taints are
 * possible.
 * 
 * @author Steven Arzt
 *
 */
public interface IReversibleTaintWrapper extends ITaintPropagationWrapper {

	/**
	 * Checks an invocation statement for black-box taint propagation. This allows
	 * the wrapper to artificially propagate taints over method invocations without
	 * requiring the analysis to look inside the method.
	 * 
	 * Note that this method is the inverse of <code>getTaintsForMethod</code>. It
	 * obtains the taints that could have been tainted <i>before</i> a certain
	 * statement was executed given a taint that was active after the call.
	 * 
	 * @param stmt        The invocation statement which to check for black-box
	 *                    taint propagation
	 * @param d1          The abstraction at the beginning of the method that calls
	 *                    the wrapped method
	 * @param taintedPath The tainted field or value to propagate. Note that this
	 *                    taint must be valid <i>after</i> the execution of the
	 *                    black-box method.
	 * @return The list of possibly tainted values before the invocation statement
	 *         referenced in {@link Stmt} has been executed
	 */
	public Set<Abstraction> getInverseTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath);

}
