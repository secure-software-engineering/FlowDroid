package soot.jimple.infoflow.callmappers;

import java.util.ArrayDeque;

import soot.MethodSubSignature;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.AbstractParameterMapping;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.DirectParameterMapping;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.DirectTarget;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.IndirectTarget;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.InvocationVirtualEdgeTarget;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.VirtualEdge;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries.VirtualEdgeTarget;

/**
 * Maps caller and callee parameters in a reflective method call
 * 
 * @author Marc Miltenberger
 */
public class VirtualEdgeTargetCallerCalleeMapper implements ICallerCalleeArgumentMapper {

	private DirectTarget directTarget;
	private InvocationVirtualEdgeTarget virtualEdgeTarget;

	public VirtualEdgeTargetCallerCalleeMapper(InvocationVirtualEdgeTarget t, DirectTarget dt) {
		this.virtualEdgeTarget = t;
		this.directTarget = dt;
	}

	@Override
	public int getCallerIndexOfCalleeParameter(int calleeParamIndex) {
		if (calleeParamIndex == BASE_OBJECT)
			return virtualEdgeTarget.getArgIndex();
		else {
			for (AbstractParameterMapping i : directTarget.getParameterMappings()) {
				if (i instanceof DirectParameterMapping) {
					DirectParameterMapping dm = (DirectParameterMapping) i;
					if (calleeParamIndex == dm.getTargetIndex())
						return dm.getSourceIndex();
				} else
					throw new RuntimeException("Unsupported mapping type: " + i.getClass());
			}
			return UNKNOWN;
		}

	}

	@Override
	public int getCalleeIndexOfCallerParameter(int callerParamIndex) {
		if (callerParamIndex == BASE_OBJECT)
			// The base object of the caller is not relevant for virtual edges
			return UNKNOWN;
		else {
			for (AbstractParameterMapping i : directTarget.getParameterMappings()) {
				if (i instanceof DirectParameterMapping) {
					DirectParameterMapping dm = (DirectParameterMapping) i;
					if (callerParamIndex == dm.getSourceIndex())
						return dm.getTargetIndex();
				} else
					throw new RuntimeException("Unsupported mapping type: " + i.getClass());
			}
			return UNKNOWN;
		}
	}

	public static VirtualEdgeTargetCallerCalleeMapper determineVirtualEdgeMapping(InfoflowManager manager,
			InvokeExpr ie, SootMethod callee) {
		if (ie == null)
			return null;
		VirtualEdge summary;
		VirtualEdgesSummaries summaries = manager.getVirtualEdgeSummaries();
		if (ie instanceof StaticInvokeExpr)
			summary = summaries.getVirtualEdgesMatchingFunction(ie.getMethod().getSignature());
		else
			summary = summaries.getVirtualEdgesMatchingSubSig(new MethodSubSignature(ie.getMethod().makeRef()));
		if (summary != null) {
			for (VirtualEdgeTarget t : summary.getTargets()) {
				if (t instanceof InvocationVirtualEdgeTarget) {
					DirectTarget dt = null;
					if (t instanceof IndirectTarget) {
						ArrayDeque<IndirectTarget> targetQueue = new ArrayDeque<>();
						targetQueue.add((IndirectTarget) t);
						IndirectTarget c;
						boolean matched = false;
						while ((c = targetQueue.poll()) != null) {
							for (VirtualEdgeTarget d : c.getTargets()) {
								if (d instanceof IndirectTarget)
									targetQueue.add(((IndirectTarget) d));
								else if (d instanceof DirectTarget) {
									dt = (DirectTarget) d;
									if (matchDirectTarget(manager, dt, callee)) {
										matched = true;
										break;
									}
								}

							}
						}
						if (!matched)
							continue;
					} else {
						dt = (DirectTarget) t;
						if (!matchDirectTarget(manager, dt, callee))
							continue;
					}

					return new VirtualEdgeTargetCallerCalleeMapper((InvocationVirtualEdgeTarget) t, dt);
				}
			}
		}
		return null;
	}

	private static boolean matchDirectTarget(InfoflowManager manager, DirectTarget dt, SootMethod callee) {
		RefType tt = dt.getTargetType();
		if (tt != null) {
			if (tt.hasSootClass() && tt.getSootClass().resolvingLevel() >= SootClass.HIERARCHY)
				if (!manager.getTypeUtils().checkCast(callee.getDeclaringClass().getType(), tt))
					return false;
		}

		return manager.getTypeUtils().isOverriden(dt.getTargetMethod(), callee);
	}

}
