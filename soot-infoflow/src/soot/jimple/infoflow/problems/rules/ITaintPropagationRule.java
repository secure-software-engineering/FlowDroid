package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Common interface for taint propagation rules
 * 
 * @author Steven Arzt
 *
 */
public interface ITaintPropagationRule {

	/**
	 * Propagates a flow along a normal statement this is not a call or return site
	 * 
	 * @param stmt       The IFDS solver state
	 * @param destStmt   The next statement to which control flow will continue
	 *                   after processing stmt
	 * @param killSource Outgoing value for the rule to specify whether the incoming
	 *                   taint shall be killed
	 * @param killAll    Outgoing value that receives whether all taints shall be
	 *                   killed and nothing shall be propagated onwards
	 * @param flags      Optional flags that inform the rule manager about the
	 *                   status of the given taint abstraction or the data flow
	 *                   analysis as a whole
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags);

	/**
	 * Propagates a flow across a call site
	 * 
	 * @param state   The IFDS solver state
	 * @param dest    The destination method into which to propagate the abstraction
	 * @param killAll Outgoing value for the rule to specify whether all taints
	 *                shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll);

	/**
	 * Propagates a flow along a the call-to-return edge at a call site
	 * 
	 * @param state      The IFDS solver state
	 * @param killSource Outgoing value for the rule to specify whether the incoming
	 *                   taint shall be killed
	 * @param killAll    Outgoing value for the rule to specify whether all taints
	 *                   shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll);

	/**
	 * Propagates a flow along a the return edge
	 * 
	 * @param callerD1s The context abstraction at the caller side
	 * @param source    The abstraction to propagate over the statement
	 * @param stmt      The statement at which to propagate the abstraction
	 * @param callSite  The call site of the call from which we return
	 * @param retSite   The return site to which the execution returns after leaving
	 *                  the current method
	 * @param killAll   Outgoing value for the rule to specify whether all taints
	 *                  shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll);

}
