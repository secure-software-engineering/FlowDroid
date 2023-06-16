package soot.jimple.infoflow.solver.gcSolver.fpc;

import java.util.Set;

public interface IGraph<N> {

	public Set<N> getNodes();

	public Set<N> succsOf(N n);

	public Set<N> predsOf(N n);

	public void addNode(N n);

	public void addEdge(N n1, N n2);

	public boolean contains(N n);

	/*
	 * removing the node itself and all edges it associated with.
	 */
	public void remove(N n);

	public void removeEdge(N n1, N n2);
}
