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
package soot.jimple.infoflow.solver.ngsolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * We are subclassing the JimpleIFDSSolver because we need the same executor for
 * both the forward and the backward analysis Also we need to be able to insert
 * edges containing new taint information
 * 
 */
public class InfoflowSolver extends IFDSSolver<Unit, Abstraction, BiDiInterproceduralCFG<Unit, SootMethod>>
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
	public boolean processEdge(SolverState<Unit, Abstraction> state) {
		propagate(state, null, false);
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
	public void cleanup() {
		this.incoming.clear();
		this.endSummary.clear();
	}

	@Override
	public Set<Pair<Unit, Abstraction>> endSummary(SootMethod m, Abstraction d3) {
		return super.endSummary(m, d3);
	}

	@Override
	protected void processExit(SolverState<Unit, Abstraction> state) {
		super.processExit(state);

		if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
			final Abstraction d1 = state.sourceVal;
			final Unit u = state.target;
			final Abstraction d2 = state.targetVal;

			final SootMethod methodThatNeedsSummary = icfg.getMethodOf(u);
			final Map<Unit, Map<Abstraction, Abstraction>> inc = incoming(d1, methodThatNeedsSummary);

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
