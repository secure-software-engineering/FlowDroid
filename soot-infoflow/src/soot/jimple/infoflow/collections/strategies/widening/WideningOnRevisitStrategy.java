package soot.jimple.infoflow.collections.strategies.widening;

import java.util.*;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.util.ConcurrentHashMultiMap;
import soot.util.IdentityHashSet;

/**
 * Widens each fact that revisits a statement. DO NOT USE IN PRODUCTION, this is mainly available for testing.
 *
 * @author Tim Lange
 */
@Deprecated
public class WideningOnRevisitStrategy extends AbstractWidening {
	// Cache of abstractions seen at a shift statement
	private final ConcurrentHashMultiMap<Unit, Abstraction> seenAbstractions;

	public WideningOnRevisitStrategy(InfoflowManager manager) {
		super(manager);
		this.seenAbstractions = new ConcurrentHashMultiMap<>();
	}

	@Override
	public Abstraction widen(Abstraction d2, Abstraction d3, Unit u) {
		Stmt stmt = (Stmt) u;
		// Only shifting can produce infinite ascending chains,
		// check whether this unit shifted the result
		if (!stmt.containsInvokeExpr() || !isShift(d2, d3))
			return d3;

		// BFS through the abstraction graph
		// Check: have we seen the incoming fact already?
		IdentityHashSet<Abstraction> visited = new IdentityHashSet<>();
		Deque<Abstraction> q = new ArrayDeque<>();
		q.add(d3.getPredecessor());
		if (d3.getNeighborCount() > 0)
			q.addAll(d3.getNeighbors());
		while (!q.isEmpty()) {
			Abstraction pred = q.pop();
			if (seenAbstractions.contains(u, pred)) {
				// Widen because this is a revisit
				return forceWiden(d3, u);
			}

			if (pred.getPredecessor() != null && visited.add(pred.getPredecessor()))
				q.add(pred.getPredecessor());
			if (pred.getNeighborCount() > 0) {
				pred.getNeighbors().forEach(
						n -> {
							if (visited.add(n))
								q.add(n);
						}
				);
			}
		}

		// Add the outgoing fact to the seen abstractions
		seenAbstractions.put(u, d3);
		return d3;
	}
}
