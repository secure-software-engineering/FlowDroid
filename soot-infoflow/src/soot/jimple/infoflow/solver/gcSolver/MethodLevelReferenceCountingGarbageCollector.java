package soot.jimple.infoflow.solver.gcSolver;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.infoflow.collect.ConcurrentCountingMap;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

import java.util.Set;

public class MethodLevelReferenceCountingGarbageCollector<N, D>
		extends AbstractReferenceCountingGarbageCollector<N, D, SootMethod> {
	public MethodLevelReferenceCountingGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<SootMethod> referenceProvider) {
		super(icfg, jumpFunctions, referenceProvider);
	}

	public MethodLevelReferenceCountingGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	/**
	 * Checks whether the given method has any open dependencies that prevent its
	 * jump functions from being garbage collected
	 *
	 * @param method           The method to check
	 * @param referenceCounter The counter that keeps track of active references to
	 *                         taint abstractions
	 * @return True it the method has active dependencies and thus cannot be
	 *         garbage-collected, false otherwise
	 */
	private boolean hasActiveDependencies(SootMethod method, ConcurrentCountingMap<SootMethod> referenceCounter) {
		int changeCounter = -1;
		do {
			// Update the change counter for the next round
			changeCounter = referenceCounter.getChangeCounter();

			// Check the method itself
			if (referenceCounter.get(method) > 0)
				return true;

			// Check the transitive callees
			Set<SootMethod> references = referenceProvider.getAbstractionReferences(method);
			for (SootMethod ref : references) {
				if (referenceCounter.get(ref) > 0)
					return true;
			}
		} while (checkChangeCounter && changeCounter != referenceCounter.getChangeCounter());
		return false;
	}

	@Override
	public boolean hasActiveDependencies(SootMethod method) {
		return hasActiveDependencies(method, jumpFnCounter);
	}

	@Override
	protected SootMethod genAbstraction(PathEdge<N, D> edge) {
		return icfg.getMethodOf(edge.getTarget());
	}

	@Override
	public void gc() {

	}

	@Override
	public void notifySolverTerminated() {

	}

	@Override
	protected IGCReferenceProvider<SootMethod> createReferenceProvider() {
		return new OnDemandReferenceProvider<>(icfg);
	}
}
