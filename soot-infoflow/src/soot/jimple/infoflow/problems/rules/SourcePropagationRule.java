package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
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

	public SourcePropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	private Collection<Abstraction> propagate(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		if (source == getZeroValue()) {
			// Check whether this can be a source at all
			final SourceInfo sourceInfo = getManager().getSourceSinkManager() != null
					? getManager().getSourceSinkManager().getSourceInfo(stmt, getManager())
					: null;

			// We never propagate zero facts onwards
			killSource.value = true;

			// Is this a source?
			if (sourceInfo != null && !sourceInfo.getAccessPaths().isEmpty()) {
				Set<Abstraction> res = new HashSet<>();
				for (AccessPath ap : sourceInfo.getAccessPaths()) {
					// Create the new taint abstraction
					Abstraction abs = new Abstraction(sourceInfo.getDefinition(), ap, stmt, sourceInfo.getUserData(),
							false, false);
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
								getAliasing().computeAliases(d1, stmt, vb.getValue(), res,
										getManager().getICFG().getMethodOf(stmt), abs);
						}
					}

					// Set the corresponding call site
					if (stmt.containsInvokeExpr())
						abs.setCorrespondingCallSite(stmt);
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
		return propagate(d1, source, stmt, killSource, killAll);
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return propagate(d1, source, stmt, killSource, null);
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
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
