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
public abstract class AbstractGarbageCollector<N, D, A> implements IGarbageCollector<N, D> {

	protected final BiDiInterproceduralCFG<N, SootMethod> icfg;
	protected final IGCReferenceProvider<A> referenceProvider;
	protected final ConcurrentHashMultiMap<A, PathEdge<N, D>> jumpFunctions;

	public AbstractGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<A, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<A> referenceProvider) {
		this.icfg = icfg;
		this.referenceProvider = referenceProvider;
		this.jumpFunctions = jumpFunctions;
		initialize();
	}

	public AbstractGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<A, PathEdge<N, D>> jumpFunctions) {
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
	protected abstract IGCReferenceProvider<A> createReferenceProvider();

	protected long getRemainingPathEdgeCount() {
		return jumpFunctions.values().size();
	}

}
