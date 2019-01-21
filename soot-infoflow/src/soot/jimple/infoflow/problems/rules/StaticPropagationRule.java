package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating taint on static fields
 * 
 * @author Steven Arzt
 *
 */
public class StaticPropagationRule extends AbstractTaintPropagationRule {

	public StaticPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		// nothing to do here
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// Do not analyze static initializers if static field tracking is
		// disabled
		if (!getManager().getConfig().getEnableStaticFieldTracking() && dest.isStaticInitializer()) {
			killAll.value = true;
			return null;
		}

		// Do not propagate static taints if static field tracking is disabled
		if (source.getAccessPath().isStaticFieldRef() && !getManager().getConfig().getEnableStaticFieldTracking()) {
			killAll.value = true;
			return null;
		}

		final AccessPath ap = source.getAccessPath();
		if (ap.isStaticFieldRef()) {
			// Do not propagate static fields that are not read inside the
			// callee
			if (getAliasing().getAliasingStrategy().isLazyAnalysis()
					|| manager.getICFG().isStaticFieldRead(dest, ap.getFirstField())) {
				TaintAbstraction newAbs = source.deriveNewAbstraction(ap, stmt);
				if (newAbs != null)
					return Collections.singleton(newAbs);
			}
		}

		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		// nothing to do here
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		// We only handle taints on static variables
		if (!source.getAccessPath().isStaticFieldRef())
			return null;

		// Static field tracking can be disabled
		if (!getManager().getConfig().getEnableStaticFieldTracking() && source.getAccessPath().isStaticFieldRef()) {
			killAll.value = true;
			return null;
		}

		// Simply pass on the taint
		return Collections.singleton(source.deriveNewAbstraction(source.getAccessPath(), stmt));
	}

}
