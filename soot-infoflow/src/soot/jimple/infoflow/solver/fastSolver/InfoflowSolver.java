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
public class InfoflowSolver extends IFDSSolver<Unit, Abstraction, IInfoflowCFG>
		implements IInfoflowSolver {

	protected IFollowReturnsPastSeedsHandler followReturnsPastSeedsHandler = null;
	protected final AbstractInfoflowProblem problem;

	protected ISolverPeerGroup peerGroup = null;

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
		propagate(edge.factAtSource(), edge.getTarget(), edge.factAtTarget(), null, false, ScheduleTarget.EXECUTOR);
		return true;
	}

	@Override
	public void injectContext(IInfoflowSolver otherSolver, SootMethod callee, Abstraction d3, Unit callSite,
							  Abstraction d2, Abstraction d1) {
		// The incoming data structure is shared in the peer group. No need to inject.
	}

	@Override
	public Set<IncomingRecord<Unit, Abstraction>> incoming(Abstraction d1, SootMethod m) {
		// Redirect to peer group
		return peerGroup.incoming(d1, m);
	}

	@Override
	public boolean addIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		// Redirect to peer group
		return peerGroup.addIncoming(m, d3, n, d1, d2);
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
		this.jumpFunctions = new MyConcurrentHashMap<PathEdge<Unit, Abstraction>, Abstraction>();
		this.incoming.clear();
		this.endSummary.clear();
		if (this.ffCache != null)
			this.ffCache.invalidate();
	}

	@Override
	public Set<EndSummary<Unit, Abstraction>> endSummary(SootMethod m, Abstraction d3) {
		return super.endSummary(m, d3);
	}

	@Override
	protected void processExit(PathEdge<Unit, Abstraction> edge) {
		super.processExit(edge);

		if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
			final Abstraction d1 = edge.factAtSource();
			final Unit u = edge.getTarget();
			final Abstraction d2 = edge.factAtTarget();

			final SootMethod methodThatNeedsSummary = icfg.getMethodOf(u);
			final Set<IncomingRecord<Unit, Abstraction>> inc = incoming(d1, methodThatNeedsSummary);

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
		return jumpFunctions.size();
	}

	@Override
	public AbstractInfoflowProblem getTabulationProblem() {
		return problem;
	}

	@Override
	public void setPeerGroup(ISolverPeerGroup solverPeerGroup) {
		this.peerGroup = solverPeerGroup;
	}

	@Override
	public void terminate() {
		// not required
	}

}
