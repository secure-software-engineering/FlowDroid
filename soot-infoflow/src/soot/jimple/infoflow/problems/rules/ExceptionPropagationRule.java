package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating exceptional data flows
 * 
 * @author Steven Arzt
 *
 */
public class ExceptionPropagationRule extends AbstractTaintPropagationRule {

	public ExceptionPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
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
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// We don't need to do anything here
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
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
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		return null;
	}

}
