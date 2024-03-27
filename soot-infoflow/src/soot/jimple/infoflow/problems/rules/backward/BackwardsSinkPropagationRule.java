package soot.jimple.infoflow.problems.rules.backward;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.cfg.FlowDroidSinkStatement;
import soot.jimple.infoflow.cfg.FlowDroidSourceStatement;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule to introduce unconditional taints at sinks Caution: As in backwards
 * analysis sinks and sources are swapped internally this works with sources
 *
 * @author Steven Arzt
 * @author Tim Lange
 */
public class BackwardsSinkPropagationRule extends AbstractTaintPropagationRule {

	public BackwardsSinkPropagationRule(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	private Collection<Abstraction> propagate(Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		if (source == getZeroValue()) {
			// Check whether this can be a source at all
			if (!(manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager))
				return null;
			final IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();
			final SourceInfo sinkInfo = ssm.getInverseSinkInfo(stmt, getManager());
			// We never propagate zero facts onwards
			killSource.value = true;

			// Is this a source?
			if (sinkInfo != null && !sinkInfo.getAccessPaths().isEmpty()) {
				// Do not introduce taints inside exclusive methods
				Collection<Unit> callers = manager.getICFG().getCallersOf(manager.getICFG().getMethodOf(stmt));
				boolean isExclusive = !callers.isEmpty()
						&& callers.stream().map(u -> (Stmt) u)
							.allMatch(callerStmt -> callerStmt.containsInvokeExpr()
													&& manager.getTaintWrapper() != null
													&& manager.getTaintWrapper().isExclusive(callerStmt, zeroValue));
				if (isExclusive)
					return null;

				Set<Abstraction> res = new HashSet<>();
				for (AccessPath ap : sinkInfo.getAccessPaths()) {
					// Implicit flows are introduced in the corresponding rule
					if (ap.isEmpty())
						continue;

					// Create the new taint abstraction
					Abstraction abs = new Abstraction(sinkInfo.getDefinitionsForAccessPath(ap), ap, stmt,
							sinkInfo.getUserData(), false, false);
					abs.setCorrespondingCallSite(stmt);
					abs = abs.deriveNewAbstractionWithTurnUnit(stmt);

					res.add(abs);
				}
				return res;
			}
			if (killAll != null)
				killAll.value = true;
		}
		return null;
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return propagate(source, stmt, killSource, killAll);
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return propagate(source, stmt, killSource, null);
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// Normally, we don't inspect source methods
		killAll.value = killAll.value
				|| (!getManager().getConfig().getInspectSources() && stmt.hasTag(FlowDroidSourceStatement.TAG_NAME));
		killAll.value = killAll.value
				|| (!getManager().getConfig().getInspectSinks() && stmt.hasTag(FlowDroidSinkStatement.TAG_NAME));
		return null;
	}
}
