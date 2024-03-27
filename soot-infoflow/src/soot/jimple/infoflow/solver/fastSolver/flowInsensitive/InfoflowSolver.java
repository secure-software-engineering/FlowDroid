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
package soot.jimple.infoflow.solver.fastSolver.flowInsensitive;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import heros.FlowFunction;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.*;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;

/**
 * We are subclassing the JimpleIFDSSolver because we need the same executor for
 * both the forward and the backward analysis Also we need to be able to insert
 * edges containing new taint information
 * 
 */
public class InfoflowSolver extends FlowInsensitiveSolver<Unit, Abstraction, IInfoflowCFG>
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
	public boolean processEdge(PathEdge<Unit, Abstraction> edge) {
		propagate(edge.factAtSource(), icfg.getMethodOf(edge.getTarget()), edge.factAtTarget(), null, false);
		return true;
	}

	@Override
	public void injectContext(IInfoflowSolver otherSolver, SootMethod callee, Abstraction d3, Unit callSite,
							  Abstraction d2, Abstraction d1) {
		if (!addIncoming(callee, d3, callSite, d1, d2))
			return;

		Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(callSite);
		applyEndSummaryOnCall(d1, callSite, d2, returnSiteNs, callee, d3);
	}

    @Override
    public void applySummary(SootMethod callee, Abstraction d3, Unit callSite, Abstraction d2, Abstraction d1) {
		Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(callSite);
		applyEndSummaryOnCall(d1, callSite, d2, returnSiteNs, callee, d3);
	}

    @Override
	protected Set<Abstraction> computeReturnFlowFunction(FlowFunction<Abstraction> retFunction, Abstraction d1,
			Abstraction d2, Unit callSite, Collection<Abstraction> callerSideDs) {
		if (retFunction instanceof SolverReturnFlowFunction) {
			// Get the d1s at the start points of the caller
			return ((SolverReturnFlowFunction) retFunction).computeTargets(d2, d1, callerSideDs);
		} else
			return retFunction.computeTargets(d2);
	}

	@Override
	protected Set<Abstraction> computeNormalFlowFunction(FlowFunction<Abstraction> flowFunction, Abstraction d1,
			Abstraction d2) {
		if (flowFunction instanceof SolverNormalFlowFunction)
			return ((SolverNormalFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	protected Set<Abstraction> computeCallToReturnFlowFunction(FlowFunction<Abstraction> flowFunction, Abstraction d1,
			Abstraction d2) {
		if (flowFunction instanceof SolverCallToReturnFlowFunction)
			return ((SolverCallToReturnFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	protected Set<Abstraction> computeCallFlowFunction(FlowFunction<Abstraction> flowFunction, Abstraction d1,
			Abstraction d2) {
		if (flowFunction instanceof SolverCallFlowFunction)
			return ((SolverCallFlowFunction) flowFunction).computeTargets(d1, d2);
		else
			return flowFunction.computeTargets(d2);
	}

	@Override
	public void cleanup() {
		this.jumpFunctions = new MyConcurrentHashMap<PathEdge<SootMethod, Abstraction>, Abstraction>();
		this.incoming.clear();
		this.endSummary.clear();
	}

	@Override
	public Set<EndSummary<Unit, Abstraction>> endSummary(SootMethod m, Abstraction d3) {
		return super.endSummary(m, d3);
	}

	@Override
	protected void processExit(Abstraction d1, Unit n, Abstraction d2) {
		super.processExit(d1, n, d2);

		if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
			final SootMethod methodThatNeedsSummary = icfg.getMethodOf(n);
			final Map<Unit, Map<Abstraction, Abstraction>> inc = incoming(d1, methodThatNeedsSummary);

			if (inc == null || inc.isEmpty())
				followReturnsPastSeedsHandler.handleFollowReturnsPastSeeds(d1, n, d2);
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
	public void setSolverId(boolean solverId) {
		super.setSolverId(solverId);
	}

	@Override
	public AbstractInfoflowProblem getTabulationProblem() {
		return this.problem;
	}

	@Override
	public void setPeerGroup(ISolverPeerGroup solverPeerGroup) {
		// We don't have support for peer groups and don't really need them either
	}

	@Override
	public void terminate() {
		// not required
	}

}
