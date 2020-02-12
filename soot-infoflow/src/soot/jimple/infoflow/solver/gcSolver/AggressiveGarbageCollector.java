package soot.jimple.infoflow.solver.gcSolver;

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
		while (jumpFunctions.size() > GC_THRESHOLD) {
			jumpFunctions.iterator().remove();
		}
	}

	@Override
	public int getGcedMethods() {
		// TODO Auto-generated method stub
		return 0;
	}

}
