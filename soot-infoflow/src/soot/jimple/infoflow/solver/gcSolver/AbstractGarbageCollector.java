package soot.jimple.infoflow.solver.gcSolver;

import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Abstract base class for garbage collectors
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractGarbageCollector<N, D> implements IGarbageCollector<N, D> {

	protected final BiDiInterproceduralCFG<N, SootMethod> icfg;
	protected final IGCReferenceProvider<D, N> referenceProvider;

	public AbstractGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg) {
		this.icfg = icfg;
		this.referenceProvider = new AheadOfTimeReferenceProvider<>(icfg);
	}

}
