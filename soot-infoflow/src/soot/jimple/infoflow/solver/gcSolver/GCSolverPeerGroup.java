package soot.jimple.infoflow.solver.gcSolver;

import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.SolverPeerGroup;

/**
 * Specialized solver peer group for garbage-collecting solvers
 * 
 * @author Steven Arzt
 *
 */
public class GCSolverPeerGroup extends SolverPeerGroup {

	private GarbageCollectorPeerGroup gcPeerGroup = new GarbageCollectorPeerGroup();

	public GCSolverPeerGroup() {
	}

	/**
	 * Creates the peer group for the garbage collectors
	 * 
	 * @return The garbage collector peer group
	 */
	public GarbageCollectorPeerGroup getGCPeerGroup() {
		if (gcPeerGroup == null) {
			gcPeerGroup = new GarbageCollectorPeerGroup();
			for (IInfoflowSolver solver : solvers) {
				if (solver instanceof InfoflowSolver) {
					InfoflowSolver gcSolver = (InfoflowSolver) solver;
					gcPeerGroup.addGarbageCollector((IGarbageCollectorPeer) gcSolver.createGarbageCollector());
				}
			}
		}
		return gcPeerGroup;
	}

}
