package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.util.ByReferenceBoolean;

public class StopAfterFirstKFlowsPropagationRule extends AbstractTaintPropagationRule {

	public StopAfterFirstKFlowsPropagationRule(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	/**
	 * Checks whether the data flow analysis must be stopped and sets the kill-flag
	 * if so
	 * 
	 * @param killAll
	 *            The variable that receives the kill-flag in case the analysis must
	 *            be stopped
	 */
	private void checkStop(ByReferenceBoolean killAll) {
		if (getManager().getConfig().getStopAfterFirstKFlows() <= getResults().getResults().size())
			killAll.value = true;
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		checkStop(killAll);
		return null;
	}

}
