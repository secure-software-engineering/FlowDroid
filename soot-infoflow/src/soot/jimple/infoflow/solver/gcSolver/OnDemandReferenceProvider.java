package soot.jimple.infoflow.solver.gcSolver;

import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.SynchronizedBy;
import heros.solver.IDESolver;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Implementation of a reference provider that computes transitive dependency
 * sets on demand
 * 
 * @author Steven Arzt
 *
 * @param <N>
 */
public class OnDemandReferenceProvider<N> extends AbstractReferenceProvider<SootMethod, N> {

	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod, Set<SootMethod>> methodToReferences = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootMethod, Set<SootMethod>>() {

				@Override
				public Set<SootMethod> load(SootMethod key) throws Exception {
					return getTransitiveCallees(key);
				}

			});

	public OnDemandReferenceProvider(BiDiInterproceduralCFG<N, SootMethod> icfg) {
		super(icfg);
	}

	@Override
	public Set<SootMethod> getAbstractionReferences(SootMethod method) {
		return methodToReferences.getUnchecked(method);
	}

}
