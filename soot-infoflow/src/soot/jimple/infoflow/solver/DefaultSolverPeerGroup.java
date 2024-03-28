package soot.jimple.infoflow.solver;

import heros.SynchronizedBy;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.util.ConcurrentHashMultiMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Peer group for synchronizing multiple IFDS solvers. Manages the calling contexts.
 * 
 * @author Steven Arzt
 */
public class DefaultSolverPeerGroup implements ISolverPeerGroup {
	// edges going along calls
	// see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final ConcurrentHashMultiMap<Pair<SootMethod, Abstraction>, IncomingRecord<Unit, Abstraction>> incoming = new ConcurrentHashMultiMap<>();

	protected Set<IInfoflowSolver> solvers = new HashSet<>();

	public DefaultSolverPeerGroup() {
	}

	public void addSolver(IInfoflowSolver solver) {
		this.solvers.add(solver);
		solver.setPeerGroup(this);
	}

	@Override
	public Set<IncomingRecord<Unit, Abstraction>> incoming(Abstraction d1, SootMethod m) {
		return incoming.get(new Pair<>(m, d1));
	}

	@Override
	public boolean addIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		IncomingRecord<Unit, Abstraction> newRecord = new IncomingRecord<>(n, d1, d2, d3);
		IncomingRecord<Unit, Abstraction> rec = incoming.putIfAbsent(new Pair<>(m, d3), newRecord);

		if (rec == null)
			for (IInfoflowSolver solver : solvers)
				solver.applySummary(m, d3, n, d2, d1);

		// The solver peer group already applies the summary for all solvers.
		// Thus, no need to call the same method again in the IFDS solver.
		return rec == null;
	}
}
