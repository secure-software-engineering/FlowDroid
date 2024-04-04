package soot.jimple.infoflow.solver.gcSolver.fpc;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.infoflow.solver.gcSolver.IGCReferenceProvider;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

public class AggressiveGarbageCollector<N, D> extends FineGrainedReferenceCountingGarbageCollector<N, D> {
	public AggressiveGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<Pair<SootMethod, D>, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<Pair<SootMethod, D>> referenceProvider) {
		super(icfg, jumpFunctions, referenceProvider);
	}

	public AggressiveGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<Pair<SootMethod, D>, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	@Override
	protected IGCReferenceProvider<Pair<SootMethod, D>> createReferenceProvider() {
		return null;
	}

	@Override
	public boolean hasActiveDependencies(Pair<SootMethod, D> abstraction) {
		int changeCounter = -1;
		do {
			// Update the change counter for the next round
			changeCounter = jumpFnCounter.getChangeCounter();

			// Check the method itself
			if (jumpFnCounter.get(abstraction) > 0)
				return true;

		} while (checkChangeCounter && changeCounter != jumpFnCounter.getChangeCounter());
		return false;
	}
}
