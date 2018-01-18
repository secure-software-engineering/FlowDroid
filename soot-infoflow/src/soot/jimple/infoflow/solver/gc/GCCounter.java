package soot.jimple.infoflow.solver.gc;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import soot.jimple.infoflow.collect.ConcurrentIdentityHashMap;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

class GCCounter<N, D extends FastSolverLinkedNode<D, N>> {

	private final AtomicInteger scheduledTasks = new AtomicInteger();
	private volatile ConcurrentMap<GCCounter<N, D>, Object> callees;

	public void incTasks() {
		scheduledTasks.incrementAndGet();
	}

	public void decTasks() {
		if (scheduledTasks.get() == 0)
			throw new RuntimeException("GC counter underflow");
		scheduledTasks.decrementAndGet();
	}

	public void addCallee(GCCounter<N, D> callee) {
		if (callees == null) {
			synchronized (this) {
				if (callees == null)
					callees = new ConcurrentIdentityHashMap<GCCounter<N, D>, Object>();
			}
		}
		callees.put(callee, callee);
	}

	public boolean canGC() {
		// Get rid of callees in which no jobs are open anymore
		if (scheduledTasks.get() == 0) {
			if (callees != null) {
				for (Iterator<GCCounter<N, D>> calleeIt = callees.keySet().iterator(); calleeIt.hasNext();) {
					GCCounter<N, D> callee = calleeIt.next();
					if (callee.canGC())
						calleeIt.remove();
				}
			}
			return callees == null || callees.isEmpty();
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callees == null) ? 0 : callees.hashCode());
		result = prime * result + ((scheduledTasks == null) ? 0 : scheduledTasks.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GCCounter<?, ?> other = (GCCounter<?, ?>) obj;
		if (callees == null) {
			if (other.callees != null)
				return false;
		} else if (!callees.equals(other.callees))
			return false;
		if (scheduledTasks == null) {
			if (other.scheduledTasks != null)
				return false;
		} else if (!scheduledTasks.equals(other.scheduledTasks))
			return false;
		return true;
	}

}
