package soot.jimple.infoflow.river;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import heros.solver.PathEdge;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

/**
 * TaintPropagationHandler querying the backward analysis when reaching an
 * additional sink. Attach to the forward analysis.
 * 
 * @author Tim Lange
 */
public class SecondaryFlowGenerator implements TaintPropagationHandler {
	// SourceSinkManager that also keeps track of conditions
	private IConditionalFlowManager condFlowManager = null;

	/**
	 * Ensures the condFlowManager field is always set.
	 *
	 * @param manager Infoflow Manager
	 */
	private void ensureCondFlowManager(InfoflowManager manager) {
		if (condFlowManager != null)
			return;

		if (!manager.getConfig().getAdditionalFlowsEnabled())
			throw new IllegalStateException("Additional flows are not enabled!");

		ISourceSinkManager ssm = manager.getSourceSinkManager();
		if (ssm instanceof IConditionalFlowManager) {
			condFlowManager = (IConditionalFlowManager) ssm;
			return;
		}

		throw new IllegalStateException("Additional Flows enabled but no ConditionalFlowManager in place!");
	}

	@Override
	public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
		// NO-OP
	}

	@Override
	public Set<Abstraction> notifyFlowOut(Unit unit, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, FlowFunctionType type) {
		// We only need to handle CallToReturn edges
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return outgoing;

		// Check whether any use matches the incoming taint
		if (!isReadAt(unit, incoming.getAccessPath()))
			return outgoing;

		ensureCondFlowManager(manager);

		Stmt stmt = (Stmt) unit;
		HashSet<Abstraction> additionalAbsSet = new HashSet<>();

		// Check for sink contexts
		if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			Abstraction baseTaint = getTaintFromLocal(outgoing, ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase());

			// Is the base tainted in the outgoing set?
			if (baseTaint != null && baseTaint.getAccessPath().getBaseType() instanceof RefType) {
				RefType ref = (RefType) baseTaint.getAccessPath().getBaseType();
				if (condFlowManager.isConditionalSink(stmt, ref.getSootClass())) {
					Abstraction newAbs = createAdditionalFlowAbstraction(baseTaint, stmt);
					additionalAbsSet.add(newAbs);
				}
			}
		}

		// Check for usage contexts
		for (AdditionalFlowInfoSpecification spec : manager.getUsageContextProvider().needsAdditionalInformation(stmt, outgoing))
			additionalAbsSet.add(createAdditionalFlowAbstraction(spec, stmt, manager));

		// Query the backward analysis
		for (Abstraction addAbs : additionalAbsSet)
			for (Unit pred : manager.getICFG().getPredsOf(unit))
				manager.additionalManager.getMainSolver().processEdge(new PathEdge<>(d1, pred, addAbs));

		return outgoing;
	}

	/**
	 * Creates a new abstraction that is injected into the backward direction.
	 *
	 * @param baseTaint Taint of the base local
	 * @param stmt Current statement
	 * @return New abstraction
	 */
	protected Abstraction createAdditionalFlowAbstraction(Abstraction baseTaint, Stmt stmt) {
		Abstraction newAbs = new Abstraction(Collections.singleton(ConditionalSecondarySourceDefinition.INSTANCE),
				baseTaint.getAccessPath(), stmt, null, false, false);
		newAbs.setCorrespondingCallSite(stmt);
		newAbs.setSourceContext(new AdditionalFlowInfoSourceContext(ConditionalSecondarySourceDefinition.INSTANCE,
				baseTaint.getAccessPath(), stmt));
		return newAbs.deriveNewAbstractionWithTurnUnit(stmt);
	}

	/**
	 * Creates a new abstraction that is injected into the backward direction.
	 *
	 * @param spec Flow Specification
	 * @param stmt Current statement
	 * @param manager Infoflow Manager
	 * @return New abstraction
	 */
	protected Abstraction createAdditionalFlowAbstraction(AdditionalFlowInfoSpecification spec, Stmt stmt, InfoflowManager manager) {
		AccessPath ap = spec.toAccessPath(manager);
		ISourceSinkDefinition def = spec.getDefinition();
		Abstraction newAbs = new Abstraction(Collections.singleton(def), ap, stmt, null, false, false);
		newAbs.setCorrespondingCallSite(stmt);
		newAbs.setSourceContext(new AdditionalFlowInfoSourceContext(def, ap, stmt));
		return newAbs.deriveNewAbstractionWithTurnUnit(stmt);
	}

	/**
	 * Check whether baseLocal is tainted in the outgoing set.
	 * Assumes baseLocal is an object and the check happens at a call site.
	 *
	 * @param outgoing outgoing taint set
	 * @param baseLocal base local
	 * @return corresponding abstraction if baseLocal is tainted else null
	 */
	protected Abstraction getTaintFromLocal(Set<Abstraction> outgoing, Value baseLocal) {
		for (Abstraction abs : outgoing)
			if (abs.getAccessPath().getPlainValue() == baseLocal)
				return abs;

		return null;
	}

	/**
	 * Check whether the access path is read at unit.
	 *
	 * @param unit unit
	 * @param ap access path
	 * @return true if ap is read at unit
	 */
	protected boolean isReadAt(Unit unit, AccessPath ap) {
		for (ValueBox box : unit.getUseBoxes())
			if (box.getValue() == ap.getPlainValue())
				return true;

		return false;
	}
}
