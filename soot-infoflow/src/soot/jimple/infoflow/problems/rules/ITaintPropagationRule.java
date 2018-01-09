package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Common interface for taint propagation rules
 * 
 * @author Steven Arzt
 *
 */
public interface ITaintPropagationRule {
	
	/**
	 * Propagates a flow along a normal statement this is not a call or return
	 * site
	 * @param d1 The context abstraction
	 * @param source The abstraction to propagate over the statement
	 * @param stmt The statement at which to propagate the abstraction
	 * @param destStmt The next statement to which control flow will continue after
	 * processing stmt
	 * @param killSource Outgoing value for the rule to specify whether
	 * the incoming taint shall be killed
	 * @param killAll Outgoing value that receives whether all taints shall be
	 * killed and nothing shall be propagated onwards
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1,
			Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource,
			ByReferenceBoolean killAll);

	/**
	 * Propagates a flow across a call site
	 * @param d1 The context abstraction
	 * @param source The abstraction to propagate over the statement
	 * @param stmt The statement at which to propagate the abstraction
	 * @param dest The destination method into which to propagate the abstraction
	 * @param killAll Outgoing value for the rule to specify whether
	 * all taints shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<Abstraction> propagateCallFlow(Abstraction d1,
			Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll);
	
	/**
	 * Propagates a flow along a the call-to-return edge at a call site
	 * @param d1 The context abstraction
	 * @param source The abstraction to propagate over the statement
	 * @param stmt The statement at which to propagate the abstraction
	 * @param killSource Outgoing value for the rule to specify whether
	 * the incoming taint shall be killed
	 * @param killAll Outgoing value for the rule to specify whether
	 * all taints shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll);
	
	/**
	 * Propagates a flow along a the return edge
	 * @param callerD1s The context abstraction at the caller side
	 * @param source The abstraction to propagate over the statement
	 * @param stmt The statement at which to propagate the abstraction
	 * @param callSite The call site of the call from which we return
	 * @param retSite The return site to which the execution returns after
	 * leaving the current method
	 * @param killAll Outgoing value for the rule to specify whether
	 * all taints shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Collection<Abstraction> propagateReturnFlow(
			Collection<Abstraction> callerD1s, Abstraction source,
			Stmt stmt, Stmt retSite, Stmt callSite,
			ByReferenceBoolean killAll);
	
}
