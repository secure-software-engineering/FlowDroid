package soot.jimple.infoflow.solver;

import java.util.HashSet;
import java.util.Set;

/**
 * Peer group for synchronizing multiple IFDS solvers
 * 
 * @author Steven Arzt
 *
 */
public abstract class SolverPeerGroup {

	protected Set<IInfoflowSolver> solvers = new HashSet<>();

	public SolverPeerGroup() {
	}

	/**
	 * Adds a data flow solver to this peer group
	 * 
	 * @param solver The solver to add
	 */
	public void addSolver(IInfoflowSolver solver) {
		this.solvers.add(solver);
	}

}
