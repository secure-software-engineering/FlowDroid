package soot.jimple.infoflow.handlers;

import java.util.Set;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.solver.ngsolver.SolverState;

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
	 * @param stmt    The statement over which the taint is propagated
	 * @param taint   The taint being propagated
	 * @param manager The manager object that gives access to the data flow engine
	 * @param type    The type of data flow edge being processed
	 */
	public void notifyFlowIn(Unit stmt, AbstractDataFlowAbstraction taint, InfoflowManager manager,
			FlowFunctionType type);

	/**
	 * Handler function that is invoked when a new taint is generated in the data
	 * flow engine
	 * 
	 * @param solverState The state of the IFDS solver
	 * @param outgoing    The set of taints being propagated
	 * @param manager     The manager object that gives access to the data flow
	 *                    engine
	 * @param type        The type of data flow edge being processed
	 * @return The new abstractions to be propagated on. If you do not want to
	 *         change the normal propagation behavior, just return the value of the
	 *         "taints" parameter as-is.
	 */
	public Set<AbstractDataFlowAbstraction> notifyFlowOut(SolverState<Unit, AbstractDataFlowAbstraction> solverState,
			Set<AbstractDataFlowAbstraction> outgoing, InfoflowManager manager, FlowFunctionType type);

}
