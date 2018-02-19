package soot.jimple.infoflow.codeOptimization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.scalar.ConditionalBranchFolder;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;
import soot.jimple.toolkits.scalar.DeadAssignmentEliminator;
import soot.jimple.toolkits.scalar.UnreachableCodeEliminator;
import soot.util.queue.QueueReader;

/**
 * Code optimizer that performs an interprocedural dead-code elimination on all
 * application classes
 * 
 * @author Steven Arzt
 *
 */
public class DeadCodeEliminator implements ICodeOptimizer {

	private InfoflowConfiguration config;

	@Override
	public void initialize(InfoflowConfiguration config) {
		this.config = config;
	}

	@Override
	public void run(InfoflowManager manager, Collection<SootMethod> entryPoints, ISourceSinkManager sourcesSinks,
			ITaintPropagationWrapper taintWrapper) {
		// Perform an intra-procedural constant propagation to prepare for the
		// inter-procedural one
		for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
			MethodOrMethodContext sm = rdr.next();
			if (sm.method() == null || !sm.method().hasActiveBody())
				continue;

			// Exclude the dummy main method
			if (entryPoints.contains(sm.method()))
				continue;

			List<Unit> callSites = getCallsInMethod(sm.method());

			ConstantPropagatorAndFolder.v().transform(sm.method().getActiveBody());
			DeadAssignmentEliminator.v().transform(sm.method().getActiveBody());

			// Remove the dead callgraph edges
			List<Unit> newCallSites = getCallsInMethod(sm.method());
			if (callSites != null)
				for (Unit u : callSites)
					if (newCallSites == null || !newCallSites.contains(u))
						Scene.v().getCallGraph().removeAllEdgesOutOf(u);
		}

		// Perform an inter-procedural constant propagation and code cleanup
		InterproceduralConstantValuePropagator ipcvp = new InterproceduralConstantValuePropagator(manager, entryPoints,
				sourcesSinks, taintWrapper);
		ipcvp.setRemoveSideEffectFreeMethods(
				config.getCodeEliminationMode() == CodeEliminationMode.RemoveSideEffectFreeCode
						&& config.getImplicitFlowMode() != ImplicitFlowMode.AllImplicitFlows);
		ipcvp.setExcludeSystemClasses(config.getIgnoreFlowsInSystemPackages());
		ipcvp.transform();

		// Get rid of all dead code
		for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
			MethodOrMethodContext sm = rdr.next();

			if (sm.method() == null || !sm.method().hasActiveBody())
				continue;
			if (config.getIgnoreFlowsInSystemPackages()
					&& SystemClassHandler.isClassInSystemPackage(sm.method().getDeclaringClass().getName()))
				continue;

			ConditionalBranchFolder.v().transform(sm.method().getActiveBody());

			// Delete all dead code. We need to be careful and patch the cfg so
			// that it does not retain edges for call statements we have deleted
			List<Unit> callSites = getCallsInMethod(sm.method());
			UnreachableCodeEliminator.v().transform(sm.method().getActiveBody());
			List<Unit> newCallSites = getCallsInMethod(sm.method());
			if (callSites != null)
				for (Unit u : callSites)
					if (newCallSites == null || !newCallSites.contains(u))
						Scene.v().getCallGraph().removeAllEdgesOutOf(u);
		}
	}

	/**
	 * Gets a list of all units that invoke other methods in the given method
	 * 
	 * @param method
	 *            The method from which to get all invocations
	 * @return The list of units calling other methods in the given method if there
	 *         is at least one such unit. Otherwise null.
	 */
	private List<Unit> getCallsInMethod(SootMethod method) {
		List<Unit> callSites = null;
		for (Unit u : method.getActiveBody().getUnits())
			if (((Stmt) u).containsInvokeExpr()) {
				if (callSites == null)
					callSites = new ArrayList<Unit>();
				callSites.add(u);
			}
		return callSites;
	}

}
