package soot.jimple.infoflow.collections.strategies.widening;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;

/**
 * Widening through a taint propagation handler. Because of the nature, a full
 * set copy is needed whenever one fact is widened as well as this is called on
 * every edge instead of only when needed.
 *
 * @author Tim Lange
 */
public class WideningTaintPropagationHandler implements TaintPropagationHandler {
	private volatile WideningStrategy<Unit, Abstraction> wideningStrategy;
	private final Function<InfoflowManager, WideningStrategy<Unit, Abstraction>> wideningStrategySupplier;

	public WideningTaintPropagationHandler(
			Function<InfoflowManager, WideningStrategy<Unit, Abstraction>> wideningStrategySupplier) {
		this.wideningStrategySupplier = wideningStrategySupplier;
	}

	private synchronized WideningStrategy<Unit, Abstraction> getWideningStrategy(InfoflowManager manager) {
		if (wideningStrategy == null)
			wideningStrategy = wideningStrategySupplier.apply(manager);
		return wideningStrategy;
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {

	}

	@Override
	public boolean notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return false;

		WideningStrategy<Unit, Abstraction> wideningStrategy = getWideningStrategy(manager);
		List<Abstraction> toAdd = new ArrayList<>();
		Iterator<Abstraction> it = outgoing.iterator();
		while (it.hasNext()) {
			Abstraction abs = it.next();
			Abstraction widened = wideningStrategy.widen(incoming, abs, stmt);
			if (widened != abs) {
				toAdd.add(widened);
				it.remove();
			}
		}
		outgoing.addAll(toAdd);
		return false;
	}
}
