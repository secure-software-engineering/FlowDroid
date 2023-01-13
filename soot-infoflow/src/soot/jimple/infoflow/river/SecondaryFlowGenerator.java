package soot.jimple.infoflow.river;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import heros.solver.PathEdge;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler.FlowFunctionType;

/**
 * TaintPropagationHandler querying the backward analysis when reaching a
 * conditional sink.
 * 
 * @author Tim Lange
 *
 */
public class SecondaryFlowGenerator implements TaintPropagationHandler {
	private Set<SootMethod> methodsForComplexFlows = new HashSet<>();

	public SecondaryFlowGenerator() {
		methodsForComplexFlows.add(Scene.v().grabMethod("<java.io.OutputStream: void write(byte[])>"));
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
		// NO-OP
		return;
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit unit, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// We only need to handle CallToReturn edges
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return outgoing;

		// Check whether any use matches the incoming taint
		Local taintedLocal = incoming.getAccessPath().getPlainValue();
		if (!unit.getUseBoxes().stream().anyMatch(paramBox -> paramBox.getValue() == taintedLocal))
			return outgoing;

		// Check whether the statement is a instance call suitable for complex flows
		Stmt stmt = (Stmt) unit;
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		if (!(invokeExpr instanceof InstanceInvokeExpr)
				|| !methodsForComplexFlows.contains(invokeExpr.getMethodRef().tryResolve()))
			return outgoing;

		// Is the base tainted in the outgoing set?
		Local baseLocal = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
		Optional<Abstraction> baseTaint = outgoing.stream().filter(out -> Aliasing.baseMatches(baseLocal, out))
				.findAny();
		if (!baseTaint.isPresent())
			return outgoing;

		Abstraction newAbs = new Abstraction(null, baseTaint.get().getAccessPath(), stmt, null, false, false);
		incoming.deriveNewAbstraction(baseTaint.get().getAccessPath(), (Stmt) unit);

		// Start the backward analysis
		for (Unit pred : manager.getICFG().getPredsOf(unit))
			manager.reverseManager.getMainSolver().processEdge(new PathEdge<>(d1, pred, newAbs));

		return outgoing;
	}

}
