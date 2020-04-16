package soot.jimple.infoflow.solver.gcSolver;

import java.util.Collection;
import java.util.HashSet;

import soot.SootMethod;

/**
 * Set of multiple garbage collectors that share a set of active dependencies
 * 
 * @author Steven Arzt
 *
 */
public class GarbageCollectorPeerGroup implements IGarbageCollectorPeer {

	private final Collection<IGarbageCollectorPeer> peers;

	public GarbageCollectorPeerGroup() {
		this.peers = new HashSet<>();
	}

	public GarbageCollectorPeerGroup(Collection<IGarbageCollectorPeer> peers) {
		this.peers = peers;
	}

	@Override
	public boolean hasActiveDependencies(SootMethod method) {
		for (IGarbageCollectorPeer peer : peers) {
			if (peer.hasActiveDependencies(method))
				return true;
		}
		return false;
	}

	/**
	 * Adds a garbage collector to this peer group
	 * 
	 * @param peer The garbage collector to add
	 */
	public void addGarbageCollector(IGarbageCollectorPeer peer) {
		this.peers.add(peer);
	}

}
