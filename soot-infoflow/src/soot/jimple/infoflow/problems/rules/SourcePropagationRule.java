package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

/**
 * Rule to introduce unconditional taints at sources
 * 
 * @author Steven Arzt
 *
 */
public class SourcePropagationRule extends AbstractTaintPropagationRule {

	public SourcePropagationRule(InfoflowManager manager, TaintAbstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	private Collection<AbstractDataFlowAbstraction> propagate(SolverState<Unit, AbstractDataFlowAbstraction> state,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		if (source == getZeroValue()) {
			// Check whether this can be a source at all
			final SourceInfo sourceInfo = getManager().getSourceSinkManager() != null
					? getManager().getSourceSinkManager().getSourceInfo(stmt, getManager())
					: null;

			// We never propagate zero facts onwards
			killSource.value = true;

			// Is this a source?
			if (sourceInfo != null && !sourceInfo.getAccessPaths().isEmpty()) {
				Set<AbstractDataFlowAbstraction> res = new HashSet<>();
				for (AccessPath ap : sourceInfo.getAccessPaths()) {
					// Create the new taint abstraction
					TaintAbstraction abs = new TaintAbstraction(sourceInfo.getDefinition(), ap, stmt,
							sourceInfo.getUserData(), false, false);
					res.add(abs);

					// Compute the aliases. This is only relevant for variables that are not
					// entirely overwritten.
					for (ValueBox vb : stmt.getUseBoxes()) {
						if (ap.startsWith(vb.getValue())) {
							// We need a relaxed "can have aliases" check here.
							// Even if we have a local, the source/sink manager
							// is free to taint the complete local while keeping
							// aliases valid (no overwrite). The startsWith()
							// above already gets rid of constants, etc.
							if (!TypeUtils.isStringType(vb.getValue().getType()) || ap.getCanHaveImmutableAliases())
								getAliasing().computeAliases(state.derive(abs), vb.getValue(), res,
										getManager().getICFG().getMethodOf(stmt));
						}
					}
				}
				return res;
			}
			if (killAll != null)
				killAll.value = true;
		}
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		return propagate(state, killSource, killAll);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		return propagate(state, killSource, null);
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

		// Normally, we don't inspect source methods
		if (!getManager().getConfig().getInspectSources() && getManager().getSourceSinkManager() != null) {
			final SourceInfo sourceInfo = getManager().getSourceSinkManager().getSourceInfo(stmt, getManager());
			if (sourceInfo != null)
				killAll.value = true;
		}

		// By default, we don't inspect sinks either
		if (!getManager().getConfig().getInspectSinks() && getManager().getSourceSinkManager() != null) {
			final boolean isSink = getManager().getSourceSinkManager().getSinkInfo(stmt, getManager(),
					source.getAccessPath()) != null;
			if (isSink)
				killAll.value = true;
		}

		return null;
	}

}
