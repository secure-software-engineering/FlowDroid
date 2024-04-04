package soot.jimple.infoflow.solver.gcSolver.fpc;

import heros.solver.Pair;
import soot.SootMethod;
import soot.jimple.infoflow.collect.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AbstrationDependencyGraph<D> implements IGraph<Pair<SootMethod, D>> {
	private final ReentrantLock lock = new ReentrantLock();
	private final Set<Pair<SootMethod, D>> nodes = new ConcurrentHashSet<>();
	private final Map<Pair<SootMethod, D>, Set<Pair<SootMethod, D>>> succMap = new ConcurrentHashMap<>();
	private final Map<Pair<SootMethod, D>, Set<Pair<SootMethod, D>>> predMap = new ConcurrentHashMap<>();

	@Override
	public Set<Pair<SootMethod, D>> getNodes() {
		return nodes;
	}

	@Override
	public Set<Pair<SootMethod, D>> succsOf(Pair<SootMethod, D> node) {
		return succMap.getOrDefault(node, Collections.emptySet());
	}

	@Override
	public Set<Pair<SootMethod, D>> predsOf(Pair<SootMethod, D> node) {
		return predMap.getOrDefault(node, Collections.emptySet());
	}

	@Override
	public void addNode(Pair<SootMethod, D> node) {
		nodes.add(node);
	}

	@Override
	public void addEdge(Pair<SootMethod, D> n1, Pair<SootMethod, D> n2) {
		addNode(n1);
		addNode(n2);
		succMap.computeIfAbsent(n1, k -> new ConcurrentHashSet<>()).add(n2);
		predMap.computeIfAbsent(n2, k -> new ConcurrentHashSet<>()).add(n1);
	}

	@Override
	public boolean contains(Pair<SootMethod, D> node) {
		return nodes.contains(node);
	}

	@Override
	public void removeEdge(Pair<SootMethod, D> n1, Pair<SootMethod, D> n2) {
		succsOf(n1).remove(n2);
		predsOf(n2).remove(n1);
	}

	@Override
	public void remove(Pair<SootMethod, D> node) {
		nodes.remove(node);
		for (Pair<SootMethod, D> pred : predsOf(node)) {
			removeEdge(pred, node);
		}
		for (Pair<SootMethod, D> succ : succsOf(node)) {
			removeEdge(node, succ);
		}
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}

	public int nodeSize() {
		return this.nodes.size();
	}

	public int edgeSize() {
		int ret = 0;
		for (Set<Pair<SootMethod, D>> vs : succMap.values()) {
			ret += vs.size();
		}
		return ret;
	}

	public Set<Pair<SootMethod, D>> reachableClosure(Pair<SootMethod, D> source) {
		final Set<Pair<SootMethod, D>> visited = new ConcurrentHashSet<>();
		final Deque<Pair<SootMethod, D>> stack = new ArrayDeque<>();
		stack.push(source);
		while (!stack.isEmpty()) {
			final Pair<SootMethod, D> node = stack.pop();
			visited.add(node);
			succsOf(node).stream().filter(n -> !visited.contains(n)).forEach(stack::push);
		}
		return visited;
	}
}
