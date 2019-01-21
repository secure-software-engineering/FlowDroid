package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.Unit;
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
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.LeakAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
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

	public SinkPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		final Stmt stmt = (Stmt) state.getTarget();
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			return checkForSink(state, returnStmt.getOp());
		} else if (stmt instanceof IfStmt) {
			final IfStmt ifStmt = (IfStmt) stmt;
			return checkForSink(state, ifStmt.getCondition());
		} else if (stmt instanceof LookupSwitchStmt) {
			final LookupSwitchStmt switchStmt = (LookupSwitchStmt) stmt;
			return checkForSink(state, switchStmt.getKey());
		} else if (stmt instanceof TableSwitchStmt) {
			final TableSwitchStmt switchStmt = (TableSwitchStmt) stmt;
			return checkForSink(state, switchStmt.getKey());
		} else if (stmt instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) stmt;
			return checkForSink(state, assignStmt.getRightOp());
		}

		return null;
	}

	/**
	 * Checks whether the given taint abstraction at the given satement triggers a
	 * sink. If so, a new result is recorded
	 * 
	 * @param state  The IFDS solver state
	 * @param retVal The value to check
	 * @return The data flow abstractions to propagate onward. These abstractions
	 *         can be used to add leaks to IFDS method summaries
	 */
	private Collection<AbstractDataFlowAbstraction> checkForSink(SolverState<Unit, AbstractDataFlowAbstraction> state,
			final Value retVal) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// The incoming value may be a complex expression. We have to look at
		// every simple value contained within it.
		for (Value val : BaseSelector.selectBaseList(retVal, false)) {
			final AccessPath ap = source.getAccessPath();
			final ISourceSinkManager sourceSinkManager = getManager().getSourceSinkManager();

			if (ap != null && sourceSinkManager != null && source.isAbstractionActive()
					&& getAliasing().mayAlias(val, ap.getPlainValue())) {
				SinkInfo sinkInfo = sourceSinkManager.getSinkInfo(stmt, getManager(), source.getAccessPath());
				if (sinkInfo != null) {
					if (!getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, stmt)))
						killState = true;
					return Collections.singleton(new LeakAbstraction(sinkInfo.getDefinition(), source, stmt));
				}
			}
		}
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
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
	protected boolean isTaintVisibleInCallee(Stmt stmt, TaintAbstraction source) {
		InvokeExpr iexpr = stmt.getInvokeExpr();
		boolean found = false;

		// Is an argument tainted?
		for (int i = 0; i < iexpr.getArgCount(); i++) {
			if (getAliasing().mayAlias(iexpr.getArg(i), source.getAccessPath().getPlainValue())) {
				if (source.getAccessPath().getTaintSubFields() || source.getAccessPath().isLocal())
					return true;
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
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// We only report leaks for active taints, not for alias queries
		if (source.isAbstractionActive() && !source.getAccessPath().isStaticFieldRef()) {
			// Is the taint even visible inside the callee?
			if (!stmt.containsInvokeExpr() || isTaintVisibleInCallee(stmt, source)) {
				// Is this a sink?
				if (getManager().getSourceSinkManager() != null) {
					// Get the sink descriptor
					SinkInfo sinkInfo = getManager().getSourceSinkManager().getSinkInfo(stmt, getManager(),
							source.getAccessPath());

					// If we have already seen the same taint at the dame sink, there is no need to
					// propagate this taint any further.
					if (sinkInfo != null) {
						if (!getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, stmt)))
							killState = true;
						return Collections.singleton(new LeakAbstraction(sinkInfo.getDefinition(), source, stmt));
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
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		// Check whether this return is treated as a sink
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			boolean matches = source.getAccessPath().isLocal() || source.getAccessPath().getTaintSubFields();
			if (matches && source.isAbstractionActive() && getManager().getSourceSinkManager() != null
					&& getAliasing().mayAlias(source.getAccessPath().getPlainValue(), returnStmt.getOp())) {
				SinkInfo sinkInfo = getManager().getSourceSinkManager().getSinkInfo(returnStmt, getManager(),
						source.getAccessPath());
				if (sinkInfo != null
						&& !getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, returnStmt)))
					killState = true;
			}
		}

		// If we are in the kill state, we stop the analysis
		if (killAll != null)
			killAll.value |= killState;

		return null;
	}

}
