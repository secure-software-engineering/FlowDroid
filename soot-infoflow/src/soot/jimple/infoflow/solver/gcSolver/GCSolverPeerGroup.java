package soot.jimple.infoflow.solver.gcSolver;

import soot.jimple.infoflow.solver.DefaultSolverPeerGroup;

/**
 * Specialized solver peer group for garbage-collecting solvers
 * 
 * @author Steven Arzt
 *
 */
public class GCSolverPeerGroup<A> extends DefaultSolverPeerGroup {

	private GarbageCollectorPeerGroup<A> gcPeerGroup = null;

	public GCSolverPeerGroup() {
	}

	/**
	 * Creates the peer group for the garbage collectors
	 * 
	 * @return The garbage collector peer group
	 */
	public GarbageCollectorPeerGroup<A> getGCPeerGroup() {
		if (gcPeerGroup == null)
			gcPeerGroup = new GarbageCollectorPeerGroup<>();
		return gcPeerGroup;
	}

}
