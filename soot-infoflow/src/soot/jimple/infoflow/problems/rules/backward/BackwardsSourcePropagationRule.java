package soot.jimple.infoflow.problems.rules.backward;

import java.util.Collection;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.river.Utils;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for recording abstractions that arrive at sources The sources are
 * swapped for the sinks, thats why all the other methods call for sink even
 * though they work on the sources
 *
 * @author Steven Arzt
 * @author Tim Lange
 */
public class BackwardsSourcePropagationRule extends AbstractTaintPropagationRule {

	private boolean killState = false;

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			checkForSource(d1, source, stmt, returnStmt.getOp());
		} else if (stmt instanceof IfStmt) {
			final IfStmt ifStmt = (IfStmt) stmt;
			checkForSource(d1, source, stmt, ifStmt.getCondition());
		} else if (stmt instanceof LookupSwitchStmt) {
			final LookupSwitchStmt switchStmt = (LookupSwitchStmt) stmt;
			checkForSource(d1, source, stmt, switchStmt.getKey());
		} else if (stmt instanceof TableSwitchStmt) {
			final TableSwitchStmt switchStmt = (TableSwitchStmt) stmt;
			checkForSource(d1, source, stmt, switchStmt.getKey());
		} else if (stmt instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) stmt;
			checkForSource(d1, source, stmt, assignStmt.getRightOp());
		} else if (stmt instanceof IdentityStmt) {
			IdentityStmt identityStmt = (IdentityStmt) stmt;
			checkForSource(d1, source, stmt, identityStmt.getLeftOp());
		}

		return null;
	}

	/**
	 * Checks whether the given taint abstraction at the given statement triggers a
	 * sink. If so, a new result is recorded
	 * 
	 * @param d1     The context abstraction
	 * @param source The abstraction that has reached the given statement
	 * @param stmt   The statement that was reached
	 * @param retVal The value to check
	 */
	private void checkForSource(Abstraction d1, Abstraction source, Stmt stmt, final Value retVal) {
		// The incoming value may be a complex expression. We have to look at
		// every simple value contained within it.
		final AccessPath ap = source.getAccessPath();
		if (!(manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager))
			return;
		final IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();
		final Aliasing aliasing = getAliasing();

		if (ap != null && ssm != null && aliasing != null && source.isAbstractionActive()) {
			for (Value val : BaseSelector.selectBaseList(retVal, false)) {
				if (aliasing.mayAlias(val, ap.getPlainValue())) {
					SinkInfo sourceInfo = ssm.getInverseSourceInfo(stmt, getManager(), source.getAccessPath());
					if (sourceInfo != null && !getResults()
							.addResult(new AbstractionAtSink(sourceInfo.getDefinitions(), source, stmt)))
						killState = true;
				}
			}
		}
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// If we are in the kill state, we stop the analysis
		if (killAll != null)
			killAll.value |= killState;

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {

		if (!(manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager))
			return null;
		final IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();

		// We only report leaks for active taints, not for alias queries
		if (source.isAbstractionActive() && !source.getAccessPath().isStaticFieldRef()
				&& !source.getAccessPath().isEmpty()) {
			// Is the taint even visible inside the callee?
			if (!stmt.containsInvokeExpr() || Utils.isTaintVisibleInCallee(stmt, source, getAliasing())) {
				// Get the sink descriptor
				SinkInfo sourceInfo = ssm.getInverseSourceInfo(stmt, getManager(), source.getAccessPath());

				// If we have already seen the same taint at the same sink, there is no need to
				// propagate this taint any further.
				if (sourceInfo != null) {
					boolean result = getResults()
							.addResult(new AbstractionAtSink(sourceInfo.getDefinitions(), source, stmt));
					if (!result)
						killState = true;
				}
			}
		}

		// If we are in the kill state, we stop the analysis
		if (killAll != null)
			killAll.value |= killState;

		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		// If we are in the kill state, we stop the analysis
		if (killAll != null)
			killAll.value |= killState;

		return null;
	}

}
