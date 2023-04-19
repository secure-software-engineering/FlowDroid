package soot.jimple.infoflow.problems.rules.forward;

import java.util.Collection;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for recording abstractions that arrive at sinks
 * 
 * @author Steven Arzt
 */
public class SinkPropagationRule extends AbstractTaintPropagationRule {

	private boolean killState = false;

	public SinkPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			checkForSink(d1, source, stmt, returnStmt.getOp());
		} else if (stmt instanceof IfStmt) {
			final IfStmt ifStmt = (IfStmt) stmt;
			checkForSink(d1, source, stmt, ifStmt.getCondition());
		} else if (stmt instanceof LookupSwitchStmt) {
			final LookupSwitchStmt switchStmt = (LookupSwitchStmt) stmt;
			checkForSink(d1, source, stmt, switchStmt.getKey());
		} else if (stmt instanceof TableSwitchStmt) {
			final TableSwitchStmt switchStmt = (TableSwitchStmt) stmt;
			checkForSink(d1, source, stmt, switchStmt.getKey());
		} else if (stmt instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) stmt;
			checkForSink(d1, source, stmt, assignStmt.getRightOp());
		}

		return null;
	}

	/**
	 * Checks whether the given taint abstraction at the given satement triggers a
	 * sink. If so, a new result is recorded
	 * 
	 * @param d1     The context abstraction
	 * @param source The abstraction that has reached the given statement
	 * @param stmt   The statement that was reached
	 * @param retVal The value to check
	 */
	private void checkForSink(Abstraction d1, Abstraction source, Stmt stmt, final Value retVal) {
		// The incoming value may be a complex expression. We have to look at
		// every simple value contained within it.
		final AccessPath ap = source.getAccessPath();
		final Aliasing aliasing = getAliasing();
		final ISourceSinkManager sourceSinkManager = getManager().getSourceSinkManager();

		if (ap != null && sourceSinkManager != null && aliasing != null && source.isAbstractionActive()) {
			for (Value val : BaseSelector.selectBaseList(retVal, false)) {
				if (aliasing.mayAlias(val, ap.getPlainValue())) {
					SinkInfo sinkInfo = sourceSinkManager.getSinkInfo(stmt, getManager(), source.getAccessPath());
					if (sinkInfo != null) {
						if (!getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinitions(), source, stmt)))
							killState = true;
					}
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

	/**
	 * Checks whether the given taint is visible inside the method called at the
	 * given call site
	 * 
	 * @param stmt   A call site where a sink method is called
	 * @param source The taint that has arrived at the given statement
	 * @return True if the callee has access to the tainted value, false otherwise
	 */
	protected boolean isTaintVisibleInCallee(Stmt stmt, Abstraction source) {
		// If we don't have an alias analysis anymore, we probably in the shutdown phase
		// anyway
		if (getAliasing() == null)
			return false;

		InvokeExpr iexpr = stmt.getInvokeExpr();
		boolean found = false;

		// Is an argument tainted?
		final Value apBaseValue = source.getAccessPath().getPlainValue();
		if (apBaseValue != null) {
			for (int i = 0; i < iexpr.getArgCount(); i++) {
				if (getAliasing().mayAlias(iexpr.getArg(i), apBaseValue)) {
					if (source.getAccessPath().getTaintSubFields() || source.getAccessPath().isLocal())
						return true;
				}
			}
		}

		// Is the base object tainted?
		if (!found && iexpr instanceof InstanceInvokeExpr) {
			if (((InstanceInvokeExpr) iexpr).getBase() == source.getAccessPath().getPlainValue())
				return true;
		}

		return false;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// We only report leaks for active taints, not for alias queries
		if (source.isAbstractionActive() && !source.getAccessPath().isStaticFieldRef()) {
			// Is the taint even visible inside the callee?
			if (!stmt.containsInvokeExpr() || isTaintVisibleInCallee(stmt, source)) {
				// Is this a sink?
				final ISourceSinkManager ssm = getManager().getSourceSinkManager();
				if (ssm != null) {
					// Get the sink descriptor
					SinkInfo sinkInfo = ssm.getSinkInfo(stmt, getManager(), source.getAccessPath());

					// If we have already seen the same taint at the same sink, there is no need to
					// propagate this taint any further.
					if (sinkInfo != null
							&& !getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinitions(), source, stmt))) {
						killState = true;
					}
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
		// Check whether this return is treated as a sink
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			final ISourceSinkManager ssm = getManager().getSourceSinkManager();
			final Aliasing aliasing = getAliasing();

			boolean matches = source.getAccessPath().isLocal() || source.getAccessPath().getTaintSubFields();

			if (matches && source.isAbstractionActive() && ssm != null && aliasing != null
					&& aliasing.mayAlias(source.getAccessPath().getPlainValue(), returnStmt.getOp())) {
				SinkInfo sinkInfo = ssm.getSinkInfo(returnStmt, getManager(), source.getAccessPath());
				if (sinkInfo != null
						&& !getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinitions(), source, returnStmt)))
					killState = true;
			}
		}

		// If we are in the kill state, we stop the analysis
		if (killAll != null)
			killAll.value |= killState;

		return null;
	}

}
