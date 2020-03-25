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
public class AggressiveGarbageCollector<N, D> extends AbstractGarbageCollector<N, D> {

	private final AtomicInteger gcedEdges = new AtomicInteger();

	/**
	 * The number of methods for which to collect jump functions, before halting the
	 * taint propagation and actually cleaning them up
	 */
	private int GC_THRESHOLD = 10;

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
		while (jumpFunctions.size() > GC_THRESHOLD) {
			it.next();
			it.remove();
			gcedEdges.incrementAndGet();
		}
	}

	@Override
	public int getGcedMethods() {
		// We don't keep track of individual methods
		return 0;
	}

	@Override
	public int getGcedEdges() {
		return gcedEdges.get();
	}

}
