package soot.jimple.infoflow.callmappers;

import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;

public class CallerCalleeManager {

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

			return IdentityCallerCalleeMapper.INSTANCE;
		}
		return null;
	}

}
