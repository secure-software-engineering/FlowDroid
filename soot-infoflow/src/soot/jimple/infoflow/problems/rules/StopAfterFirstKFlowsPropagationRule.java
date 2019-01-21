package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

public class StopAfterFirstKFlowsPropagationRule extends AbstractTaintPropagationRule {

	public StopAfterFirstKFlowsPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	/**
	 * Checks whether the data flow analysis must be stopped and sets the kill-flag
	 * if so
	 * 
	 * @param killAll The variable that receives the kill-flag in case the analysis
	 *                must be stopped
	 */
	private void checkStop(ByReferenceBoolean killAll) {
		if (getManager().getConfig().getStopAfterFirstKFlows() <= getResults().getResults().size())
			killAll.value = true;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

}
