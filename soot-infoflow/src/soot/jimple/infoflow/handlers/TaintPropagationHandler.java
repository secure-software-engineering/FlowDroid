package soot.jimple.infoflow.handlers;

import java.util.Set;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Handler interface for callbacks during taint propagation
 * 
 * @author Steven Arzt
 * @author Malte Viering
 */
public interface TaintPropagationHandler {

	/**
	 * Enumeration containing the supported types of data flow edges
	 */
	public enum FlowFunctionType {
		NormalFlowFunction, CallFlowFunction, CallToReturnFlowFunction, ReturnFlowFunction
	}

	/**
	 * Handler function that is invoked when a taint is proagated in the data flow
	 * engine
	 * 
	 * @param stmt
	 *            The statement over which the taint is propagated
	 * @param taint
	 *            The taint being propagated
	 * @param cfg
	 *            The interprocedural control flow graph containing the current
	 *            method
	 * @param type
	 *            The type of data flow edge being processed
	 */
	public void notifyFlowIn(Unit stmt, Abstraction taint, IInfoflowCFG cfg, FlowFunctionType type);

	/**
	 * Handler function that is invoked when a new taint is generated in the data
	 * flow engine
	 * 
	 * @param stmt
	 *            The statement over which the taint is propagated
	 * @param d1
	 *            The abstraction at the beginning of the current method
	 * @param incoming
	 *            The original abstraction from which the outgoing ones were
	 *            computed
	 * @param outgoing
	 *            The set of taints being propagated
	 * @param cfg
	 *            The interprocedural control flow graph containing the current
	 *            method
	 * @param type
	 *            The type of data flow edge being processed
	 * @return The new abstractions to be propagated on. If you do not want to
	 *         change the normal propagation behavior, just return the value of the
	 *         "taints" parameter as-is.
	 */
	public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			IInfoflowCFG cfg, FlowFunctionType type);

}
