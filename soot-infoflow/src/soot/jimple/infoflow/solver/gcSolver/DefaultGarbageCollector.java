package soot.jimple.infoflow.solver.gcSolver;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.infoflow.collect.ConcurrentCountingMap;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * The default garbage collector implementation
 * 
 * @author Steven Arzt
 *
 */
public class DefaultGarbageCollector<N, D> extends AbstractGarbageCollector<N, D> {

	private ConcurrentCountingMap<SootMethod> jumpFnCounter = new ConcurrentCountingMap<>();
	private final Set<SootMethod> gcScheduleSet = new ConcurrentHashSet<>();
	private final AtomicInteger gcedMethods = new AtomicInteger();
	private final AtomicInteger gcedEdges = new AtomicInteger();

	/**
	 * The number of methods to collect as candidates for garbage collection, before
	 * halting the taint propagation and actually cleaning them up
	 */
	private int GC_THRESHOLD = 0;

	public DefaultGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	@Override
	public void notifyEdgeSchedule(PathEdge<N, D> edge) {
		SootMethod sm = icfg.getMethodOf(edge.getTarget());
		jumpFnCounter.increment(sm);
		synchronized (gcScheduleSet) {
			gcScheduleSet.add(sm);
		}
	}

	@Override
	public void notifyTaskProcessed(PathEdge<N, D> edge) {
		jumpFnCounter.decrement(icfg.getMethodOf(edge.getTarget()));
	}

	@Override
	public void gc() {
		if (gcScheduleSet.size() > GC_THRESHOLD) {
			// Get the methods for which no propagation tasks are scheduled right now. This
			// set is approximate, because other threads may add new tasks while we're
			// collecting our GC candidates.
			ConcurrentCountingMap<SootMethod> snapshot;
			synchronized (gcScheduleSet) {
				snapshot = jumpFnCounter.snapshot(gcScheduleSet);
			}
			Set<SootMethod> toRemove = snapshot.getByValue(0);

			if (!toRemove.isEmpty()) {
				// Check and add the candidates for GC to our global mark list
				for (Iterator<SootMethod> it = toRemove.iterator(); it.hasNext();) {
					if (hasActiveDependencies(it.next(), snapshot))
						it.remove();
				}

				// Clean up the methods
				if (toRemove.size() > GC_THRESHOLD) {
					for (Iterator<SootMethod> it = toRemove.iterator(); it.hasNext();) {
						SootMethod sm = it.next();
						Set<PathEdge<N, D>> oldFunctions = jumpFunctions.get(sm);
						if (oldFunctions != null)
							gcedEdges.addAndGet(oldFunctions.size());
						if (jumpFunctions.remove(sm))
							gcedMethods.incrementAndGet();
						it.remove();
						gcScheduleSet.remove(sm);
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given method has any open dependencies that prevent its
	 * jump functions from being garbage collected
	 * 
	 * @param method           The method to check
	 * @param referenceCounter The counter that keeps track of active references to
	 *                         taint abstractions
	 * @return True it the method has active dependencies and thus cannot be
	 *         garbage-collected, false otherwise
	 */
	private boolean hasActiveDependencies(SootMethod method, ConcurrentCountingMap<SootMethod> referenceCounter) {
		Set<SootMethod> references = referenceProvider.getMethodReferences(method, null);
		for (SootMethod ref : references) {
			if (referenceCounter.get(ref) > 0)
				return true;
		}
		return false;
	}

	@Override
	public int getGcedMethods() {
		return gcedMethods.get();
	}

	@Override
	public int getGcedEdges() {
		return gcedEdges.get();
	}

}
