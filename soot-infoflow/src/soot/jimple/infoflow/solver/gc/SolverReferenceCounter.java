package soot.jimple.infoflow.solver.gc;

import soot.SootMethod;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.collect.MyConcurrentHashMap.IValueFactory;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

public class SolverReferenceCounter<N, D extends FastSolverLinkedNode<D, N>> {

	private final IValueFactory<GCCounter<N, D>> counterFactory = new IValueFactory<GCCounter<N, D>>() {

		@Override
		public GCCounter<N, D> createValue() {
			return new GCCounter<N, D>();
		}

	};

	private final MyConcurrentHashMap<GCContext<N, D>, GCCounter<N, D>> countingMap = new MyConcurrentHashMap<>();

	private GCCounter<N, D> getCounter(GCContext<N, D> context) {
		return countingMap.putIfAbsentElseGet(context, counterFactory);
	}

	public void incTasks(D context, SootMethod method) {
		GCContext<N, D> gccontext = new GCContext<>(context, method);
		GCCounter<N, D> ctr = getCounter(gccontext);
		ctr.incTasks();
	}

	public void decTasks(D context, SootMethod method) {
		GCContext<N, D> gccontext = new GCContext<>(context, method);
		decTasks(gccontext);
	}

	public void decTasks(GCContext<N, D> gccontext) {
		GCCounter<N, D> ctr = getCounter(gccontext);
		if (ctr != null)
			ctr.decTasks();
	}

	public void addCallee(D context, SootMethod method, D calleeContext, SootMethod calleeMethod) {
		GCContext<N, D> gccontext = new GCContext<>(context, method);
		GCContext<N, D> calleeGCContext = new GCContext<N, D>(calleeContext, calleeMethod);
		GCCounter<N, D> ctr = getCounter(gccontext);
		GCCounter<N, D> ctrCallee = getCounter(calleeGCContext);
		ctr.addCallee(ctrCallee);
	}

	public boolean canGC(GCContext<N, D> gccontext) {
		GCCounter<N, D> ctr = getCounter(gccontext);
		return ctr == null || ctr.canGC();
	}

}
