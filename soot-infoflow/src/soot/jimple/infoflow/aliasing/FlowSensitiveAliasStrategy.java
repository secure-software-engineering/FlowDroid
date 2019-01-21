package soot.jimple.infoflow.aliasing;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.ngsolver.SolverState;

/**
 * A fully flow-sensitive aliasing strategy
 * 
 * @author Steven Arzt
 */
public class FlowSensitiveAliasStrategy extends AbstractBulkAliasStrategy {

	private final IInfoflowSolver bSolver;

	public FlowSensitiveAliasStrategy(InfoflowManager manager, IInfoflowSolver backwardsSolver) {
		super(manager);
		this.bSolver = backwardsSolver;
	}

	@Override
	public void computeAliasTaints(SolverState<Unit, AbstractDataFlowAbstraction> state,
			Set<? extends AbstractDataFlowAbstraction> taintSet, SootMethod method) {
		final Stmt src = (Stmt) state.getTarget();

		// Start the backwards solver
		AbstractDataFlowAbstraction targetVal = state.getTargetVal();
		if (targetVal instanceof TaintAbstraction) {
			TaintAbstraction taint = (TaintAbstraction) targetVal;
			AbstractDataFlowAbstraction bwAbs = taint.deriveInactiveAbstraction(src);
			for (Unit predUnit : manager.getICFG().getPredsOf(src))
				bSolver.processEdge(state.derive(predUnit, bwAbs));
		}
	}

	@Override
	public void injectCallingContext(AbstractDataFlowAbstraction d3, IInfoflowSolver fSolver, SootMethod callee,
			SolverState<Unit, AbstractDataFlowAbstraction> solverState) {
		bSolver.injectContext(fSolver, callee, d3, solverState);
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
