package soot.jimple.infoflow.callmappers;

import soot.MethodSubSignature;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;

/**
 * Looks up the responsible caller-callee argument mapper.
 * See also {@link ICallerCalleeArgumentMapper}.
 * 
 * @author Marc Miltenberger
 */
public class CallerCalleeManager {

	/**
	 * Returns the responsible caller-callee argument mapper for the given call site and callee.
	 * @param manager the infoflow manager
	 * @param callSite the callsite
	 * @param callee the callee
	 * @return the corresponding caller-callee mapper
	 */
	public static ICallerCalleeArgumentMapper getMapper(InfoflowManager manager, Stmt callSite, SootMethod callee) {
		if (callSite != null && callSite.containsInvokeExpr()) {
			final InvokeExpr ie = callSite.getInvokeExpr();

			final boolean isReflectiveCallSite = manager.getICFG().isReflectiveCallSite(callSite);
			if (isReflectiveCallSite)
				return ReflectionCallerCalleeMapper.INSTANCE;
			final VirtualEdgeTargetCallerCalleeMapper vedgemapper = VirtualEdgeTargetCallerCalleeMapper
					.determineVirtualEdgeMapping(manager, ie, callee);
			if (vedgemapper != null)
				return vedgemapper;
			if (!manager.getTypeUtils().isOverriden(new MethodSubSignature(ie.getMethod().makeRef()), callee))
				return UnknownCallerCalleeMapper.INSTANCE;
			return IdentityCallerCalleeMapper.INSTANCE;
		}
		return null;
	}

}
