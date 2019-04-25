package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

/**
 * Rule for using external taint wrappers
 * 
 * @author Steven Arzt
 *
 */
public class WrapperPropagationRule extends AbstractTaintPropagationRule {

	public WrapperPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		return null;
	}

	/**
	 * Computes the taints produced by a taint wrapper object
	 * 
	 * @param state The IFDS solver state
	 * @return The taints computed by the wrapper
	 */
	private Set<TaintAbstraction> computeWrapperTaints(SolverState<Unit, AbstractDataFlowAbstraction> state) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final TaintAbstraction d1 = (TaintAbstraction) state.getSourceVal();
		final Stmt iStmt = (Stmt) state.getTarget();

		// Do not process zero abstractions
		if (source == getZeroValue())
			return null;

		// If we don't have a taint wrapper, there's nothing we can do here
		if (getManager().getTaintWrapper() == null)
			return null;

		// Do not check taints that are not mentioned anywhere in the call
		if (!source.getAccessPath().isStaticFieldRef() && !source.getAccessPath().isEmpty()) {
			boolean found = false;

			// The base object must be tainted
			if (iStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) iStmt.getInvokeExpr();
				found = getAliasing().mayAlias(iiExpr.getBase(), source.getAccessPath().getPlainValue());
			}

			// or one of the parameters must be tainted
			if (!found)
				for (int paramIdx = 0; paramIdx < iStmt.getInvokeExpr().getArgCount(); paramIdx++)
					if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(),
							iStmt.getInvokeExpr().getArg(paramIdx))) {
						found = true;
						break;
					}

			// If nothing is tainted, we don't have any taints to propagate
			if (!found)
				return null;
		}

		// Do not apply the taint wrapper to statements that are sources on their own
		if (!getManager().getConfig().getInspectSources()) {
			// Check whether this can be a source at all
			final SourceInfo sourceInfo = getManager().getSourceSinkManager() != null
					? getManager().getSourceSinkManager().getSourceInfo(iStmt, getManager())
					: null;
			if (sourceInfo != null)
				return null;
		}

		Set<TaintAbstraction> res = getManager().getTaintWrapper().getTaintsForMethod(iStmt, d1, source);
		if (res != null) {
			Set<TaintAbstraction> resWithAliases = new HashSet<>(res);
			for (TaintAbstraction abs : res) {
				// The new abstraction gets activated where it was generated
				if (!abs.equals(source)) {
					// If the taint wrapper creates a new taint, this must be propagated
					// backwards as there might be aliases for the base object
					// Note that we don't only need to check for heap writes such as a.x = y,
					// but also for base object taints ("a" in this case).
					final AccessPath val = abs.getAccessPath();
					boolean taintsObjectValue = val.getBaseType() instanceof RefType
							&& abs.getAccessPath().getBaseType() instanceof RefType
							&& (!TypeUtils.isStringType(val.getBaseType()) || val.getCanHaveImmutableAliases());
					boolean taintsStaticField = getManager().getConfig().getEnableStaticFieldTracking()
							&& abs.getAccessPath().isStaticFieldRef();

					// If the tainted value gets overwritten, it cannot have aliases afterwards
					boolean taintedValueOverwritten = (iStmt instanceof DefinitionStmt)
							? Aliasing.baseMatches(((DefinitionStmt) iStmt).getLeftOp(), abs)
							: false;

					if (!taintedValueOverwritten)
						if (taintsStaticField || (taintsObjectValue && abs.getAccessPath().getTaintSubFields())
								|| Aliasing.canHaveAliases(iStmt, val.getPlainValue(), abs))
							getAliasing().computeAliases(state.derive(abs), val.getPlainValue(), resWithAliases,
									getManager().getICFG().getMethodOf(iStmt));
				}
			}
			res = resWithAliases;
		}

		return res;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		// Compute the taint wrapper taints
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		Collection<TaintAbstraction> wrapperTaints = computeWrapperTaints(state);
		if (wrapperTaints != null) {
			// If the taint wrapper generated an abstraction for
			// the incoming access path, we assume it to be handled
			// and do not pass on the incoming abstraction on our own
			for (TaintAbstraction wrapperAbs : wrapperTaints)
				if (wrapperAbs.getAccessPath().equals(source.getAccessPath())) {
					if (wrapperAbs != source)
						killSource.value = true;
					break;
				}
		}
		return wrapperTaints == null ? null
				: wrapperTaints.stream().map(v -> (AbstractDataFlowAbstraction) v).collect(Collectors.toSet());
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// If we have an exclusive taint wrapper for the target
		// method, we do not perform an own taint propagation.
		if (getManager().getTaintWrapper() != null && getManager().getTaintWrapper().isExclusive(stmt, source)) {
			// taint is propagated in CallToReturnFunction, so we do not need any taint
			// here:
			killAll.value = true;
		}
		return null;
	}

}
