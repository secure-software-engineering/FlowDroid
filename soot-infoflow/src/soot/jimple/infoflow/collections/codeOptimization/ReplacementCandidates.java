package soot.jimple.infoflow.collections.codeOptimization;

import java.util.HashSet;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;

public class ReplacementCandidates extends HashSet<ReplacementCandidates.RTriple> {

	private static final long serialVersionUID = -9071828269301702785L;

	protected static class RTriple {
		private final SootMethod method;
		private final Stmt oldStmt;
		private final Stmt newStmt;

		private RTriple(SootMethod method, Stmt oldStmt, Stmt newStmt) {
			this.method = method;
			this.oldStmt = oldStmt;
			this.newStmt = newStmt;
		}

		private void replace() {
			method.getActiveBody().getUnits().swapWith(oldStmt, newStmt);
		}
	}

	public void add(SootMethod sm, Stmt oldStmt, Stmt newStmt) {
		add(new RTriple(sm, oldStmt, newStmt));
	}

	public void replace(IInfoflowCFG icfg) {
		HashSet<SootMethod> changedMethods = new HashSet<>();
		for (RTriple rt : this) {
			rt.replace();
			changedMethods.add(rt.method);
		}

		for (SootMethod sm : changedMethods) {
			ConstantPropagatorAndFolder.v().transform(sm.getActiveBody());
			icfg.notifyMethodChanged(sm);
		}
	}
}
