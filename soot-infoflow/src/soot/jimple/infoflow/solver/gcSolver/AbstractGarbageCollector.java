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
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<D, N> referenceProvider) {
		this.icfg = icfg;
		this.referenceProvider = referenceProvider;
		this.jumpFunctions = jumpFunctions;
		initialize();
	}

	public AbstractGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		this.icfg = icfg;
		this.referenceProvider = createReferenceProvider();
		this.jumpFunctions = jumpFunctions;
		initialize();
	}

	/**
	 * Initializes the garbage collector
	 */
	protected void initialize() {
	}

	/**
	 * Creates the reference provider that garbage collectors can use to identify
	 * dependencies
	 * 
	 * @return The new reference provider
	 */
	protected IGCReferenceProvider<D, N> createReferenceProvider() {
		return new OnDemandReferenceProvider<>(icfg);
	}

}
