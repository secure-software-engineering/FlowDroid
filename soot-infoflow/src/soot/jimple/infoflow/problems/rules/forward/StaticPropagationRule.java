package soot.jimple.infoflow.problems.rules.forward;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating taint on static fields
 * 
 * @author Steven Arzt
 *
 */
public class StaticPropagationRule extends AbstractTaintPropagationRule {

	public StaticPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
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
		final StaticFieldTrackingMode staticFieldMode = getManager().getConfig().getStaticFieldTrackingMode();

		// Do not analyze static initializers if static field tracking is
		// disabled
		if (staticFieldMode == StaticFieldTrackingMode.None) {
			if (dest.isStaticInitializer() || source.getAccessPath().isStaticFieldRef()) {
				killAll.value = true;
				return null;
			}
		}

		final AccessPath ap = source.getAccessPath();
		if (ap.isStaticFieldRef()) {
			// Do not propagate static fields that are not read inside the
			// callee
			boolean isLazyAnalysis = false;
			Aliasing aliasing = getAliasing();
			if (aliasing != null) {
				IAliasingStrategy strategy = aliasing.getAliasingStrategy();
				isLazyAnalysis = strategy != null && strategy.isLazyAnalysis();
			}
			if (isLazyAnalysis || manager.getICFG().isStaticFieldRead(dest, ap.getFirstField())) {
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
		// Static field tracking can be disabled
		if (getManager().getConfig().getStaticFieldTrackingMode() == StaticFieldTrackingMode.None
				&& source.getAccessPath().isStaticFieldRef()) {
			killAll.value = true;
			return null;
		}

		// nothing to do here
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1, Abstraction source, Stmt stmt,
                                                       Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		// We only handle taints on static variables
		if (!source.getAccessPath().isStaticFieldRef())
			return null;

		// Static field tracking can be disabled
		if (getManager().getConfig().getStaticFieldTrackingMode() == StaticFieldTrackingMode.None
				&& source.getAccessPath().isStaticFieldRef()) {
			killAll.value = true;
			return null;
		}

		// Simply pass on the taint
		return Collections.singleton(source.deriveNewAbstraction(source.getAccessPath(), stmt));
	}

}
