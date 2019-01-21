package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule that implements type checs
 * 
 * @author Steven Arzt
 *
 */
public class TypingPropagationRule extends AbstractTaintPropagationRule {

	public TypingPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// Check for a typecast on the right side of an assignment
		if (!source.getAccessPath().isStaticFieldRef() && stmt instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			if (defStmt.getRightOp() instanceof CastExpr) {
				CastExpr ce = (CastExpr) defStmt.getRightOp();
				if (ce.getOp() == source.getAccessPath().getPlainValue()) {
					// If the typecast is not compatible with the current type, we
					// have to kill the taint
					if (!getManager().getTypeUtils().checkCast(source.getAccessPath(), ce.getCastType())) {
						killAll.value = true;
					}
				}
			}
		}

		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

}
