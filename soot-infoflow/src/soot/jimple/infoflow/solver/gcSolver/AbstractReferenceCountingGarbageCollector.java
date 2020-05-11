package soot.jimple.infoflow.solver.gcSolver;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.infoflow.collect.ConcurrentCountingMap;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.util.ExtendedAtomicInteger;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * Abstract base class for garbage collectors based on reference counting
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractReferenceCountingGarbageCollector<N, D> extends AbstractGarbageCollector<N, D>
		implements IGarbageCollectorPeer {

	private ConcurrentCountingMap<SootMethod> jumpFnCounter = new ConcurrentCountingMap<>();
	private final Set<SootMethod> gcScheduleSet = new ConcurrentHashSet<>();
	private final AtomicInteger gcedMethods = new AtomicInteger();
	private final AtomicInteger gcedEdges = new AtomicInteger();
	private final ExtendedAtomicInteger edgeCounterForThreshold = new ExtendedAtomicInteger();
	private GarbageCollectionTrigger trigger = GarbageCollectionTrigger.Immediate;
	private GarbageCollectorPeerGroup peerGroup = null;
	private boolean checkChangeCounter = false;

	protected boolean validateEdges = false;
	protected Set<PathEdge<N, D>> oldEdges = new HashSet<>();

	/**
	 * The number of methods to collect as candidates for garbage collection, before
	 * halting the taint propagation and actually cleaning them up
	 */
	protected int methodThreshold = 0;
	/**
	 * Wait for at least this number of edges before starting the garbage collection
	 */
	protected int edgeThreshold = 0;

	public AbstractReferenceCountingGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions,
			IGCReferenceProvider<D, N> referenceProvider) {
		super(icfg, jumpFunctions, referenceProvider);
	}

	public AbstractReferenceCountingGarbageCollector(BiDiInterproceduralCFG<N, SootMethod> icfg,
			ConcurrentHashMultiMap<SootMethod, PathEdge<N, D>> jumpFunctions) {
		super(icfg, jumpFunctions);
	}

	@Override
	public void notifyEdgeSchedule(PathEdge<N, D> edge) {
		SootMethod sm = icfg.getMethodOf(edge.getTarget());
		jumpFnCounter.increment(sm);
		gcScheduleSet.add(sm);
		if (trigger == GarbageCollectionTrigger.EdgeThreshold)
			edgeCounterForThreshold.incrementAndGet();

		if (validateEdges) {
			if (oldEdges.contains(edge))
				System.out.println("Edge re-scheduled");
		}
	}

	@Override
	public void notifyTaskProcessed(PathEdge<N, D> edge) {
		jumpFnCounter.decrement(icfg.getMethodOf(edge.getTarget()));
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
		int changeCounter = -1;
		do {
			// Update the change counter for the next round
			changeCounter = referenceCounter.getChangeCounter();

			// Check the method itself
			if (referenceCounter.get(method) > 0)
				return true;

			// Check the transitive callees
			Set<SootMethod> references = referenceProvider.getMethodReferences(method, null);
			for (SootMethod ref : references) {
				if (referenceCounter.get(ref) > 0)
					return true;
			}
		} while (checkChangeCounter && changeCounter != referenceCounter.getChangeCounter());
		return false;
	}

	@Override
	public boolean hasActiveDependencies(SootMethod method) {
		return hasActiveDependencies(method, jumpFnCounter);
	}

	/**
	 * Immediately performs garbage collection
	 */
	protected void gcImmediate() {
		if (gcScheduleSet != null && !gcScheduleSet.isEmpty()) {
			// Check our various triggers for garbage collection
			boolean gc = trigger == GarbageCollectionTrigger.Immediate;
			gc |= trigger == GarbageCollectionTrigger.MethodThreshold && gcScheduleSet.size() > methodThreshold;
			gc |= trigger == GarbageCollectionTrigger.EdgeThreshold && edgeCounterForThreshold.get() > edgeThreshold;

			// Perform the garbage collection if required
			if (gc) {
				int tempMethods = 0;
				onBeforeRemoveEdges();
				for (SootMethod sm : gcScheduleSet) {
					// Is it safe to remove this method?
					if (peerGroup != null) {
						if (peerGroup.hasActiveDependencies(sm))
							continue;
					} else if (hasActiveDependencies(sm))
						continue;

					// Get stats for the stuff we are about to remove
					Set<PathEdge<N, D>> oldFunctions = jumpFunctions.get(sm);
					if (oldFunctions != null) {
						int gcedSize = oldFunctions.size();
						gcedEdges.addAndGet(gcedSize);
						if (trigger == GarbageCollectionTrigger.EdgeThreshold)
							edgeCounterForThreshold.subtract(gcedSize);
					}

					// First unregister the method, then delete the edges. In case some other thread
					// concurrently schedules a new edge, the method gets back into the GC work list
					// this way.
					gcScheduleSet.remove(sm);
					if (jumpFunctions.remove(sm)) {
						gcedMethods.incrementAndGet();
						tempMethods++;
						if (validateEdges)
							oldEdges.addAll(oldFunctions);
					}
				}
				onAfterRemoveEdges(tempMethods);
			}
		}
	}

	/**
	 * Method that is called before the first edge is removed from the jump
	 * functions
	 */
	protected void onBeforeRemoveEdges() {
	}

	/**
	 * Method that is called after the last edge has been removed from the jump
	 * functions
	 * 
	 * @param gcedMethods The number of methods for which edges have been removed
	 */
	protected void onAfterRemoveEdges(int gcedMethods) {
	}

	@Override
	public int getGcedMethods() {
		return gcedMethods.get();
	}

	@Override
	public int getGcedEdges() {
		return gcedEdges.get();
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

	/**
	 * Sets the minimum number of edges that shall be propagated before triggering
	 * the garbage collection
	 * 
	 * @param threshold The minimum number of edges that shall be propagated before
	 *                  triggering the garbage collection
	 */
	public void setEdgeThreshold(int threshold) {
		this.edgeThreshold = threshold;
	}

	/**
	 * Set the trigger that defines when garbage collection shall be started
	 * 
	 * @param trigger The trigger that defines when garbage collection shall be
	 *                started
	 */
	public void setTrigger(GarbageCollectionTrigger trigger) {
		this.trigger = trigger;
	}

	/**
	 * Sets the peer group in which this solver operates. Peer groups are used to
	 * synchronize active dependencies between multiple solvers.
	 * 
	 * @param peerGroup The peer group
	 */
	public void setPeerGroup(GarbageCollectorPeerGroup peerGroup) {
		this.peerGroup = peerGroup;
		peerGroup.addGarbageCollector(this);
	}

	/**
	 * Sets whether the change counter shall be checked when identifying active
	 * method dependencies
	 * 
	 * @param checkChangeCounter True to ensure consistency using change counters,
	 *                           false otherwise
	 */
	public void setCheckChangeCounter(boolean checkChangeCounter) {
		this.checkChangeCounter = checkChangeCounter;
	}

}
