package soot.jimple.infoflow.aliasing;

import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.IInfoflowSolver;

/**
 * Aliasing strategy that relies on taints being propagated everywhere for true
 * lazy alias checking
 * 
 * @author Steven Arzt
 *
 */
public class LazyAliasingStrategy extends AbstractInteractiveAliasStrategy {

	public LazyAliasingStrategy(InfoflowManager manager) {
		super(manager);
	}

	@Override
	public boolean isInteractive() {
		// We always check alias relationships on demand
		return true;
	}

	@Override
	public boolean mayAlias(AccessPath ap1, AccessPath ap2) {
		// If it's the same access path, the two alias trivially
		if (ap1 == ap2 || ap1.equals(ap2))
			return true;
		
		// Check the full access path for aliasing
		PointsToSet ptaAP1 = getPointsToSet(ap1);
		PointsToSet ptaAP2 = getPointsToSet(ap2);
		if (ptaAP1.hasNonEmptyIntersection(ptaAP2))
			return true;
		
		// No aliasing found
		return false;
	}

	/**
	 * Gets the points-to-set for the given access path
	 * @param accessPath The access path for which to get the points-to-set
	 * @return The points-to-set for the given access path
	 */
	private PointsToSet getPointsToSet(AccessPath accessPath) {
		if (accessPath.isLocal())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue());
		else if (accessPath.isInstanceFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue(), accessPath.getFirstField());
		else if (accessPath.isStaticFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getFirstField());
		else
			throw new RuntimeException("Unexepected access path type");
	}

	@Override
	public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver, SootMethod callee, Unit callSite,
			Abstraction source, Abstraction d1) {
		// nothing to do here
	}

	@Override
	public boolean isFlowSensitive() {
		return true;
	}

	@Override
	public boolean requiresAnalysisOnReturn() {
		// There is no upfront analysis, not even on return
		return false;
	}

	@Override
	public boolean hasProcessedMethod(SootMethod method) {
		return true;
	}
	

	@Override
	public boolean isLazyAnalysis() {
		return true;
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
