package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.RefType;
import soot.SootMethod;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

/**
 * Rule for using external taint wrappers
 * 
 * @author Steven Arzt
 *
 */
public class WrapperPropagationRule extends AbstractTaintPropagationRule {

	public WrapperPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	/**
	 * Computes the taints produced by a taint wrapper object
	 * 
	 * @param d1     The context (abstraction at the method's start node)
	 * @param iStmt  The call statement the taint wrapper shall check for well-
	 *               known methods that introduce black-box taint propagation
	 * @param source The taint source
	 * @return The taints computed by the wrapper
	 */
	private Set<Abstraction> computeWrapperTaints(Abstraction d1, final Stmt iStmt, Abstraction source) {
		// Do not process zero abstractions
		if (source == getZeroValue())
			return null;

		// If we don't have a taint wrapper, there's nothing we can do here
		if (getManager().getTaintWrapper() == null)
			return null;

		// Do not check taints that are not mentioned anywhere in the call
		if (!source.getAccessPath().isStaticFieldRef() && !source.getAccessPath().isEmpty()) {
			boolean found = false;

			// The base object must be tainted
			if (iStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) iStmt.getInvokeExpr();
				found = getAliasing().mayAlias(iiExpr.getBase(), source.getAccessPath().getPlainValue());
			}

			// or one of the parameters must be tainted
			if (!found)
				for (int paramIdx = 0; paramIdx < iStmt.getInvokeExpr().getArgCount(); paramIdx++)
					if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(),
							iStmt.getInvokeExpr().getArg(paramIdx))) {
						found = true;
						break;
					}

			// If nothing is tainted, we don't have any taints to propagate
			if (!found)
				return null;
		}

		// Do not apply the taint wrapper to statements that are sources on their own
		if (!getManager().getConfig().getInspectSources()) {
			// Check whether this can be a source at all
			final SourceInfo sourceInfo = getManager().getSourceSinkManager() != null
					? getManager().getSourceSinkManager().getSourceInfo(iStmt, getManager())
					: null;
			if (sourceInfo != null)
				return null;
		}

		Set<Abstraction> res = getManager().getTaintWrapper().getTaintsForMethod(iStmt, d1, source);
		if (res != null) {
			Set<Abstraction> resWithAliases = new HashSet<>(res);
			for (Abstraction abs : res) {
				// The new abstraction gets activated where it was generated
				if (!abs.equals(source)) {
					// If the taint wrapper creates a new taint, this must be propagated
					// backwards as there might be aliases for the base object
					// Note that we don't only need to check for heap writes such as a.x = y,
					// but also for base object taints ("a" in this case).
					final AccessPath val = abs.getAccessPath();
					boolean taintsObjectValue = val.getBaseType() instanceof RefType
							&& abs.getAccessPath().getBaseType() instanceof RefType
							&& (!TypeUtils.isStringType(val.getBaseType()) || val.getCanHaveImmutableAliases());
					boolean taintsStaticField = getManager().getConfig()
							.getStaticFieldTrackingMode() != StaticFieldTrackingMode.None
							&& abs.getAccessPath().isStaticFieldRef();

					// If the tainted value gets overwritten, it cannot have aliases afterwards
					boolean taintedValueOverwritten = (iStmt instanceof DefinitionStmt)
							? Aliasing.baseMatches(((DefinitionStmt) iStmt).getLeftOp(), abs)
							: false;

					if (!taintedValueOverwritten)
						if (taintsStaticField || (taintsObjectValue && abs.getAccessPath().getTaintSubFields())
								|| Aliasing.canHaveAliases(iStmt, val.getPlainValue(), abs))
							getAliasing().computeAliases(d1, iStmt, val.getPlainValue(), resWithAliases,
									getManager().getICFG().getMethodOf(iStmt), abs);
				}
			}
			res = resWithAliases;
		}

		return res;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// Compute the taint wrapper taints
		Collection<Abstraction> wrapperTaints = computeWrapperTaints(d1, stmt, source);
		if (wrapperTaints != null) {
			// If the taint wrapper generated an abstraction for
			// the incoming access path, we assume it to be handled
			// and do not pass on the incoming abstraction on our own
			for (Abstraction wrapperAbs : wrapperTaints)
				if (wrapperAbs.getAccessPath().equals(source.getAccessPath())) {
					if (wrapperAbs != source)
						killSource.value = true;
					break;
				}
		}
		return wrapperTaints;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// If we have an exclusive taint wrapper for the target
		// method, we do not perform an own taint propagation.
		if (getManager().getTaintWrapper() != null && getManager().getTaintWrapper().isExclusive(stmt, source)) {
			// taint is propagated in CallToReturnFunction, so we do not need any taint
			// here:
			killAll.value = true;
		}
		return null;
	}

}
