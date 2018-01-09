package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating taint on static fields
 * 
 * @author Steven Arzt
 *
 */
public class StaticPropagationRule extends AbstractTaintPropagationRule {

	public StaticPropagationRule(InfoflowManager manager, Aliasing aliasing, Abstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, aliasing, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// nothing to do here
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// Do not analyze static initializers if static field tracking is
		// disabled
		if (!getManager().getConfig().getEnableStaticFieldTracking() && dest.isStaticInitializer()) {
			killAll.value = true;
			return null;
		}

		// Do not propagate static taints if static field tracking is disabled
		if (source.getAccessPath().isStaticFieldRef() && !getManager().getConfig().getEnableStaticFieldTracking()) {
			killAll.value = true;
			return null;
		}

		final AccessPath ap = source.getAccessPath();
		if (ap.isStaticFieldRef()) {
			// Do not propagate static fields that are not read inside the
			// callee
			if (aliasing.getAliasingStrategy().isLazyAnalysis()
					|| manager.getICFG().isStaticFieldRead(dest, ap.getFirstField())) {
				Abstraction newAbs = source.deriveNewAbstraction(ap, stmt);
				if (newAbs != null)
					return Collections.singleton(newAbs);
			}
		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// nothing to do here
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		// We only handle taints on static variables
		if (!source.getAccessPath().isStaticFieldRef())
			return null;

		// Static field tracking can be disabled
		if (!getManager().getConfig().getEnableStaticFieldTracking() && source.getAccessPath().isStaticFieldRef()) {
			killAll.value = true;
			return null;
		}

		// Simply pass on the taint
		return Collections.singleton(source.deriveNewAbstraction(source.getAccessPath(), stmt));
	}

}
