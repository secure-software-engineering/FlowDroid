package soot.jimple.infoflow.taintWrappers;

import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Taint wrapper that does not actually provide a library model, but that counts
 * various statistics and evaluation data. It is not exclusive for any method
 * and never returns any taints.
 * 
 * @author Steven Arzt
 *
 */
public class RecordingTaintWrapper extends AbstractTaintWrapper {

	private final Set<SootMethod> targetMethods = new HashSet<>();

	@Override
	public Set<TaintAbstraction> getAliasesForMethod(Stmt stmt, TaintAbstraction d1, TaintAbstraction taintedPath) {
		return null;
	}

	@Override
	public boolean supportsCallee(SootMethod method) {
		return false;
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		return false;
	}

	@Override
	protected boolean isExclusiveInternal(Stmt stmt, AccessPath taintedPath) {
		return false;
	}

	@Override
	public Set<AccessPath> getTaintsForMethodInternal(Stmt stmt, AccessPath taintedPath) {
		if (stmt.containsInvokeExpr())
			targetMethods.add(stmt.getInvokeExpr().getMethod());
		return null;
	}

	/**
	 * Gets the set with all methods for which this taint wrapper was queried
	 * 
	 * @return A set containing all methods for which this taint wrapper was queried
	 */
	public Set<SootMethod> getCallees() {
		return this.targetMethods;
	}

}
