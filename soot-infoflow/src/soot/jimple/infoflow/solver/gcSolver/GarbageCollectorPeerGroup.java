package soot.jimple.infoflow.solver.gcSolver;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Set of multiple garbage collectors that share a set of active dependencies
 * 
 * @author Steven Arzt
 *
 */
public class GarbageCollectorPeerGroup<A> implements IGarbageCollectorPeer<A> {

	private final Collection<IGarbageCollectorPeer<A>> peers;

	public GarbageCollectorPeerGroup() {
		this.peers = new ArrayList<>();
	}

	public GarbageCollectorPeerGroup(Collection<IGarbageCollectorPeer<A>> peers) {
		this.peers = peers;
	}

	@Override
	public boolean hasActiveDependencies(A abstraction) {
		for (IGarbageCollectorPeer<A> peer : peers) {
			if (peer.hasActiveDependencies(abstraction))
				return true;
		}
		return false;
	}

	@Override
	public void notifySolverTerminated() {
		for(IGarbageCollectorPeer<A> peer : peers) {
			peer.notifySolverTerminated();
		}
	}

	/**
	 * Adds a garbage collector to this peer group
	 * 
	 * @param peer The garbage collector to add
	 */
	public void addGarbageCollector(IGarbageCollectorPeer<A> peer) {
		this.peers.add(peer);
	}

}
