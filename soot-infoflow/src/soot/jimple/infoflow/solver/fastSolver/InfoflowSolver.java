/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.solver.fastSolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import heros.FlowFunction;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * We are subclassing the JimpleIFDSSolver because we need the same executor for
 * both the forward and the backward analysis Also we need to be able to insert
 * edges containing new taint information
 * 
 */
public class InfoflowSolver extends IFDSSolver<Unit, TaintAbstraction, BiDiInterproceduralCFG<Unit, SootMethod>>
		implements IInfoflowSolver {

	private IFollowReturnsPastSeedsHandler followReturnsPastSeedsHandler = null;
	private final AbstractInfoflowProblem problem;

	public InfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
		super(problem);
		this.problem = problem;
		this.executor = executor;
		problem.setSolver(this);
	}

	@Override
	protected InterruptableExecutor getExecutor() {
		return executor;
	}

	@Override
	public boolean processEdge(PathEdge<Unit, TaintAbstraction> edge) {
		propagate(edge.factAtSource(), edge.getTarget(), edge.factAtTarget(), null, false);
		return true;
	}

	@Override
	public void injectContext(IInfoflowSolver otherSolver, SootMethod callee, TaintAbstraction d3, Unit callSite,
			TaintAbstraction d2, TaintAbstraction d1) {
		if (!addIncoming(callee, d3, callSite, d1, d2))
			return;

		Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(callSite);
		applyEndSummaryOnCall(d1, callSite, d2, returnSiteNs, callee, d3);
	}

	@Override
	protected Set<TaintAbstraction> computeReturnFlowFunction(FlowFunction<TaintAbstraction> retFunction, TaintAbstraction d1,
			TaintAbstraction d2, Unit callSite, Collection<TaintAbstraction> callerSideDs) {
		if (retFunction instanceof SolverReturnFlowFunction) {
			// Get the d1s at the start points of the caller
			return ((SolverReturnFlowFunction) retFunction).computeTargets(d2, d1, callerSideDs);
		} else
			return retFunction.computeTargets(d2);
	}

	@Override
	protected Set<TaintAbstraction> computeNormalFlowFunction(FlowFunction<TaintAbstraction> flowFunction, TaintAbstraction d1,
			TaintAbstraction d2) {
		if (flowFunction instanceof SolverNormalFlowFunction)
			return ((SolverNormalFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	protected Set<TaintAbstraction> computeCallToReturnFlowFunction(FlowFunction<TaintAbstraction> flowFunction, TaintAbstraction d1,
			TaintAbstraction d2) {
		if (flowFunction instanceof SolverCallToReturnFlowFunction)
			return ((SolverCallToReturnFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	protected Set<TaintAbstraction> computeCallFlowFunction(FlowFunction<TaintAbstraction> flowFunction, TaintAbstraction d1,
			TaintAbstraction d2) {
		if (flowFunction instanceof SolverCallFlowFunction)
			return ((SolverCallFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	public void cleanup() {
		this.jumpFunctions = new MyConcurrentHashMap<PathEdge<Unit, TaintAbstraction>, TaintAbstraction>();
		this.incoming.clear();
		this.endSummary.clear();
	}

	@Override
	public Set<Pair<Unit, TaintAbstraction>> endSummary(SootMethod m, TaintAbstraction d3) {
		return super.endSummary(m, d3);
	}

	@Override
	protected void processExit(PathEdge<Unit, TaintAbstraction> edge) {
		super.processExit(edge);

		if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
			final TaintAbstraction d1 = edge.factAtSource();
			final Unit u = edge.getTarget();
			final TaintAbstraction d2 = edge.factAtTarget();

			final SootMethod methodThatNeedsSummary = icfg.getMethodOf(u);
			final Map<Unit, Map<TaintAbstraction, TaintAbstraction>> inc = incoming(d1, methodThatNeedsSummary);

			if (inc == null || inc.isEmpty())
				followReturnsPastSeedsHandler.handleFollowReturnsPastSeeds(d1, u, d2);
		}
	}

	@Override
	public void setFollowReturnsPastSeedsHandler(IFollowReturnsPastSeedsHandler handler) {
		this.followReturnsPastSeedsHandler = handler;
	}

	@Override
	public long getPropagationCount() {
		return propagationCount;
	}

	@Override
	public AbstractInfoflowProblem getTabulationProblem() {
		return problem;
	}

}
