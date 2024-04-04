package soot.jimple.infoflow.solver.gcSolver;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * Aggressive garbage collector that optimizes memory usage, but may degrade
 * performance by removing path edges too early. This implementation does not
 * necessarily guarantee that the analysis terminates at all. Use with caution.
 * 
 * @author Steven Arzt
 *
 */
public class AggressiveGarbageCollector<N, D> extends AbstractGarbageCollector<N, D, SootMethod> {

	private final AtomicInteger gcedMethods = new AtomicInteger();

	/**
	 * The number of methods for which to collect jump functions, before halting the
	 * taint propagation and actually cleaning them up
	 */
	private int methodThreshold = 0;

	public AggressiveGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	@Override
	public void notifyEdgeSchedule(PathEdge<N, D> edge) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyTaskProcessed(PathEdge<N, D> edge) {
		// TODO Auto-generated method stub

	}

	@Override
	public void gc() {
		Iterator<Pair<SootMethod, PathEdge<N, D>>> it = jumpFunctions.iterator();
		while (jumpFunctions.size() > methodThreshold) {
			it.next();
			it.remove();
			gcedMethods.incrementAndGet();
		}
	}

	@Override
	public int getGcedAbstractions() {
		return gcedMethods.get();
	}

	@Override
	public int getGcedEdges() {
		// We don't keep track of individual edges
		return 0;
	}

	@Override
	protected IGCReferenceProvider<SootMethod> createReferenceProvider() {
		return new OnDemandReferenceProvider<>(icfg);
	}

	/**
	 * Sets the number of methods for which edges must have been added before
	 * garbage collection is started
	 * 
	 * @param threshold The threshold of new methods required to trigger garbage
	 *                  collection
	 */
	public void setMethodThreshold(int threshold) {
		this.methodThreshold = threshold;
	}

	@Override
	public void notifySolverTerminated() {
		// TODO Auto-generated method stub

	}

}
