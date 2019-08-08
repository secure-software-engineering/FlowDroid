package soot.jimple.infoflow.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Taint propagation handler that processes a sequence of inner handlers. For
 * outputs, all generated abstractions are merged into a final abstraction set.
 * 
 * @author Steven Arzt
 *
 */
public class SequentialTaintPropagationHandler implements TaintPropagationHandler {

	private final List<TaintPropagationHandler> innerHandlers;

	/**
	 * Creates a new, empty sequence of taint propagation handlers
	 */
	public SequentialTaintPropagationHandler() {
		this.innerHandlers = new ArrayList<>();
	}

	/**
	 * Creates a sequence of taint propagation handlers from the given list
	 * 
	 * @param handlers
	 *            A list of taint propagation handlers to which all calls shall be
	 *            relayed
	 */
	public SequentialTaintPropagationHandler(List<TaintPropagationHandler> handlers) {
		this.innerHandlers = new ArrayList<>(handlers);
	}

	/**
	 * Adds a new handler to this sequence of handlers
	 * 
	 * @param handler
	 *            The handler to add to the sequence
	 */
	public void addHandler(TaintPropagationHandler handler) {
		this.innerHandlers.add(handler);
	}

	/**
	 * Gets the inner handlers registered with this object
	 * 
	 * @return The inner handlers registered with this object
	 */
	public List<TaintPropagationHandler> getHandlers() {
		return innerHandlers;
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
		for (TaintPropagationHandler handler : innerHandlers)
			handler.notifyFlowIn(stmt, taint, manager, type);
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		Set<Abstraction> resultSet = new HashSet<>();
		for (TaintPropagationHandler handler : innerHandlers) {
			Set<Abstraction> handlerResults = handler.notifyFlowOut(stmt, d1, incoming, outgoing, manager, type);
			if (handlerResults != null && !handlerResults.isEmpty())
				resultSet.addAll(handlerResults);
		}
		return resultSet;
	}

}
