package soot.jimple.infoflow.util;

import java.util.Collections;
import java.util.Set;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;

/**
 * Prints all propagations to stdout. Useful to debug small test cases.
 *
 * @author Tim Lange
 */
public class DebugFlowFunctionTaintPropagationHandler implements TaintPropagationHandler {
	public static class MethodFilter {
		private final Set<String> signatures;

		public MethodFilter(String signature) {
			this.signatures = Collections.singleton(signature);
		}

		public MethodFilter(Set<String> signatures) {
			this.signatures = signatures;
		}

		private boolean evaluate(String curr) {
			for (String signature : signatures)
				if (curr.contains(signature))
					return true;
			return false;
		}
	}

	private final String prefix;
	private final MethodFilter filter;

	public DebugFlowFunctionTaintPropagationHandler() {
		this("", null);
	}

	public DebugFlowFunctionTaintPropagationHandler(MethodFilter filter) {
		this("", filter);
	}

	public DebugFlowFunctionTaintPropagationHandler(String prefix, MethodFilter filter) {
		this.prefix = prefix;
		this.filter = filter;
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
		// no-op
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		if (this.filter != null && !this.filter.evaluate(manager.getICFG().getMethodOf(stmt).toString()))
			return outgoing;

		String typeString = "";
		switch (type) {
		case CallToReturnFlowFunction:
			typeString = "CallToReturn";
			break;
		case ReturnFlowFunction:
			typeString = "Return";
			break;
		case CallFlowFunction:
			typeString = "Call";
			break;
		case NormalFlowFunction:
			typeString = "Normal";
			break;
		}
		if (type == FlowFunctionType.ReturnFlowFunction && outgoing != null) {
			Unit out = stmt;
			for (Abstraction abs : outgoing) {
				if (abs.getCorrespondingCallSite() != null) {
					out = abs.getCorrespondingCallSite();
					break;
				}
			}
			System.out.println(this.prefix + " " + typeString + " @ " + out + ":\n\tIn: " + incoming + "\n\tOut: "
					+ outgoing + "\n");
		} else
			System.out.println(this.prefix + " " + typeString + " @ " + stmt + ":\n\tIn: " + incoming + "\n\tOut: "
					+ outgoing + "\n");

		return outgoing;
	}
}
