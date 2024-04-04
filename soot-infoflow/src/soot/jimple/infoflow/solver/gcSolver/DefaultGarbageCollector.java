package soot.jimple.infoflow.solver.gcSolver;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * The default garbage collector implementation
 * 
 * @author Steven Arzt
 *
 */
public class DefaultGarbageCollector<N, D> extends MethodLevelReferenceCountingGarbageCollector<N, D> {

	public DefaultGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	public DefaultGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<SootMethod> referenceProvider) {
		super(icfg, jumpFunctions, referenceProvider);
	}

	@Override
	public void gc() {
		gcImmediate();
	}

	@Override
	public void notifySolverTerminated() {
		// nothing to do here
	}

}
