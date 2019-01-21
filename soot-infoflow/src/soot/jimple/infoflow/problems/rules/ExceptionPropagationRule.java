package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating exceptional data flows
 * 
 * @author Steven Arzt
 *
 */
public class ExceptionPropagationRule extends AbstractTaintPropagationRule {

	public ExceptionPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// Do not process zero abstractions
		if (source == getZeroValue())
			return null;

		// Do we catch an exception here?
		if (source.getExceptionThrown() && stmt instanceof DefinitionStmt) {
			DefinitionStmt def = (DefinitionStmt) stmt;
			if (def.getRightOp() instanceof CaughtExceptionRef) {
				killSource.value = true;
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
						def.getLeftOp());
				return ap == null ? null : Collections.singleton(source.deriveNewAbstractionOnCatch(ap));
			}
		}

		// Do we throw an exception here?
		if (stmt instanceof ThrowStmt) {
			ThrowStmt throwStmt = (ThrowStmt) stmt;
			if (getAliasing().mayAlias(throwStmt.getOp(), source.getAccessPath().getPlainValue())) {
				killSource.value = true;
				return Collections.singleton(source.deriveNewAbstractionOnThrow(throwStmt));
			}
		}

		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		// We don't need to do anything here
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		// If we throw an exception with a tainted operand, we need to
		// handle this specially
		if (stmt instanceof ThrowStmt && retSite instanceof DefinitionStmt) {
			DefinitionStmt defRetStmt = (DefinitionStmt) retSite;
			if (defRetStmt.getRightOp() instanceof CaughtExceptionRef) {
				ThrowStmt throwStmt = (ThrowStmt) stmt;
				if (getAliasing().mayAlias(throwStmt.getOp(), source.getAccessPath().getPlainValue()))
					return Collections.singleton(source.deriveNewAbstractionOnThrow(throwStmt));
			}
		}

		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		return null;
	}

}
