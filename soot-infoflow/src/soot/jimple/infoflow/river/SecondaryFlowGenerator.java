package soot.jimple.infoflow.river;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import heros.solver.PathEdge;
import soot.Local;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ConditionalSinkInfo;
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
	public boolean notifyFlowOut(Unit unit, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing,
			InfoflowManager manager, TaintPropagationResults results, FlowFunctionType type) {
		// We only need to handle CallToReturn edges
		if (type != FlowFunctionType.CallToReturnFlowFunction)
			return false;

		// Check whether any use matches the incoming taint
		if (!Utils.isReadAt(unit, incoming.getAccessPath()))
			return false;

		ensureCondFlowManager(manager);

		Stmt stmt = (Stmt) unit;
		HashSet<Abstraction> additionalAbsSet = new HashSet<>();

		// Check for sink contexts
		if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			Value baseLocal = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
			Abstraction baseTaint = Utils.getTaintFromLocal(outgoing, baseLocal);

			// Is the base tainted in the outgoing set?
			if (baseTaint != null && baseTaint.getAccessPath().getBaseType() instanceof RefType) {
				RefType ref = (RefType) baseTaint.getAccessPath().getBaseType();
				ConditionalSinkInfo info = condFlowManager.getConditionalSinkInfo(stmt, ref.getSootClass());
				if (info != null) {
					Set<Local> locals = info.getTriggeredAdditionalFlows(stmt);
					if (locals != null) {
						for (Local l : locals) {
							Abstraction newAbs;
							if (l == baseLocal) {
								newAbs = createAdditionalFlowAbstraction(baseTaint, stmt);
							} else {
								newAbs = createAdditionalFlowAbstraction(baseTaint.deriveNewAbstraction(
										manager.getAccessPathFactory().createAccessPath(l, true), stmt), stmt);
							}
							additionalAbsSet.add(newAbs);
						}
					}
				}
			}
		}

		// Check for usage contexts
		for (AdditionalFlowInfoSpecification spec : manager.getUsageContextProvider().needsAdditionalInformation(stmt,
				outgoing))
			additionalAbsSet.add(createAdditionalFlowAbstraction(spec, stmt, manager));

		// Query the backward analysis
		for (Abstraction addAbs : additionalAbsSet)
			for (Unit pred : manager.getICFG().getPredsOf(unit))
				// from forwards to backwards analysis
				manager.additionalManager.getMainSolver().processEdge(new PathEdge<>(d1, pred, addAbs));
		return false;
	}

	/**
	 * Creates a new abstraction that is injected into the backward direction.
	 *
	 * @param baseTaint Taint of the base local
	 * @param stmt      Current statement
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
	 * @param spec    Flow Specification
	 * @param stmt    Current statement
	 * @param manager Infoflow Manager
	 * @return New abstraction
	 */
	protected Abstraction createAdditionalFlowAbstraction(AdditionalFlowInfoSpecification spec, Stmt stmt,
			InfoflowManager manager) {
		AccessPath ap = spec.toAccessPath(manager);
		ISourceSinkDefinition def = spec.getDefinition();
		Abstraction newAbs = new Abstraction(Collections.singleton(def), ap, stmt, null, false, false);
		newAbs.setCorrespondingCallSite(stmt);
		newAbs.setSourceContext(new AdditionalFlowInfoSourceContext(def, ap, stmt));
		return newAbs.deriveNewAbstractionWithTurnUnit(stmt);
	}

}
