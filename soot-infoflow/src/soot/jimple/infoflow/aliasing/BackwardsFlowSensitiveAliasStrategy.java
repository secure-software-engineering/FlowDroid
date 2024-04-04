package soot.jimple.infoflow.aliasing;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;

import java.util.Set;

/**
 * A fully flow-sensitive aliasing strategy
 * 
 * @author Steven Arzt
 */
public class BackwardsFlowSensitiveAliasStrategy extends AbstractBulkAliasStrategy {

	private final IInfoflowSolver bSolver;

	public BackwardsFlowSensitiveAliasStrategy(InfoflowManager manager, IInfoflowSolver backwardsSolver) {
		super(manager);
		this.bSolver = backwardsSolver;
	}

	@Override
	public void computeAliasTaints(final Abstraction d1, final Stmt src, final Value targetValue,
			Set<Abstraction> taintSet, SootMethod method, Abstraction newAbs) {
		// Start the backwards solver
		assert manager.getICFG() instanceof BackwardsInfoflowCFG;
		// sometimes we need to revisit the statement itself, so
		// looping through predecessors isn't always needed
		bSolver.processEdge(new PathEdge<Unit, Abstraction>(d1, src, newAbs));
	}

	@Override
	public void injectCallingContext(Abstraction d3, IInfoflowSolver fSolver, SootMethod callee, Unit callSite,
			Abstraction source, Abstraction d1) {
		bSolver.injectContext(fSolver, callee, d3, callSite, source, d1);
	}

	@Override
	public boolean isFlowSensitive() {
		return true;
	}

	@Override
	public boolean requiresAnalysisOnReturn() {
		return false;
	}

	@Override
	public IInfoflowSolver getSolver() {
		return bSolver;
	}

	@Override
	public void cleanup() {
		bSolver.cleanup();
	}

}
