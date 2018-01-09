package soot.jimple.infoflow.aliasing;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.IInfoflowSolver;

/**
 * Aliasing strategy that does nothing beyond comparing access paths based on
 * equality
 * 
 * @author Steven Arzt
 *
 */
public class NullAliasStrategy implements IAliasingStrategy {

	@Override
	public void computeAliasTaints(Abstraction d1, Stmt src, Value targetValue, Set<Abstraction> taintSet,
			SootMethod method, Abstraction newAbs) {
		//
	}

	@Override
	public boolean isInteractive() {
		return false;
	}

	@Override
	public boolean mayAlias(AccessPath ap1, AccessPath ap2) {
		return ap1.equals(ap2);
	}

	@Override
	public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver, SootMethod callee, Unit callSite,
			Abstraction source, Abstraction d1) {
		//
	}

	@Override
	public boolean isFlowSensitive() {
		return false;
	}

	@Override
	public boolean requiresAnalysisOnReturn() {
		return false;
	}

	@Override
	public boolean hasProcessedMethod(SootMethod method) {
		return false;
	}

	@Override
	public boolean isLazyAnalysis() {
		return false;
	}

	@Override
	public IInfoflowSolver getSolver() {
		return null;
	}

	@Override
	public void cleanup() {
		// nothing to do here
	}

}
