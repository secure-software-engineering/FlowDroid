package soot.jimple.infoflow.codeOptimization;

import java.util.Collection;
import java.util.Iterator;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.callgraph.ReachableMethods;

/**
 * There is one special case: If a source stmt is a static method without any
 * parameters, which is called from a static method also without any parameters
 * (see static2Test), the assign stmt s = staticSource() is be the first
 * statement in the iCFG (excluded the dummy main method). The CallToReturn flow
 * function only gets applied if a return site is available. In backwards
 * analysis the iCFG is reversed and the assign stmt therefore has no return
 * site. But we need the execution of the CallToReturn flow function to register
 * the arrival at the source.
 *
 * To fix this edge case, we add a NOP in static methods called from an entry
 * point.
 */
public class AddNopStmt implements ICodeOptimizer {
	InfoflowConfiguration config;

	@Override
	public void initialize(InfoflowConfiguration config) {
		this.config = config;
	}

	@Override
	public void run(InfoflowManager manager, Collection<SootMethod> entryPoints, ISourceSinkManager sourcesSinks,
			ITaintPropagationWrapper taintWrapper) {
		// only methods called from the dummy main methods are considered
		for (SootMethod entryPoint : entryPoints) {
			if (!entryPoint.hasActiveBody())
				continue;

			for (Unit unit : entryPoint.getActiveBody().getUnits()) {
				// only method calls are considered
				if (!(unit instanceof InvokeStmt))
					continue;

				InvokeExpr iExpr = ((InvokeStmt) unit).getInvokeExpr();
				// non-static/parameterized methods always have IdentityStmts
				// assigning this/parameters before any invoke
				if (!(iExpr instanceof StaticInvokeExpr) || iExpr.getArgCount() != 0)
					continue;

				Collection<SootMethod> callees = manager.getICFG().getCalleesOfCallAt(unit);
				for (SootMethod callee : callees) {
					if (!callee.hasActiveBody())
						continue;

					UnitPatchingChain units = callee.getActiveBody().getUnits();
					// The only possibility at this point is a tainted return value
					// So we only apply the NOP if the first stmt is an assign stmt
					if (units.getFirst() instanceof AssignStmt) {
						NopStmt nop = Jimple.v().newNopStmt();
						nop.addTag(SimulatedCodeElementTag.TAG);
						units.addFirst(nop);
					}
				}
			}
		}

		// same for clinit methods
		// which are scheduled manually
		ReachableMethods rm = Scene.v().getReachableMethods();
		for (Iterator<MethodOrMethodContext> iter = rm.listener(); iter.hasNext();) {
			SootMethod sm = iter.next().method();
			if (sm.hasActiveBody() && sm.getSubSignature().contains("void <clinit>()")) {
				UnitPatchingChain units = sm.getActiveBody().getUnits();
				if (units.getFirst() instanceof AssignStmt) {
					NopStmt nop = Jimple.v().newNopStmt();
					nop.addTag(SimulatedCodeElementTag.TAG);
					units.addFirst(nop);
				}
			}
		}
	}
}
