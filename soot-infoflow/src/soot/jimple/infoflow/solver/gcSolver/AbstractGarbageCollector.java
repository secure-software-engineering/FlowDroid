package soot.jimple.infoflow.solver.gcSolver;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * Abstract base class for garbage collectors
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractGarbageCollector<N, D> implements IGarbageCollector<N, D> {

	protected final BiDiInterproceduralCFG<N, SootMethod> icfg;
	protected final IGCReferenceProvider<D, N> referenceProvider;
	protected final ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions;

	public AbstractGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		this.icfg = icfg;
		this.referenceProvider = new OnDemandReferenceProvider<>(icfg);
		this.jumpFunctions = jumpFunctions;
	}

}
