package soot.jimple.infoflow.solver.cfg;

import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;

/**
 * Inverse interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class BackwardsInfoflowCFG extends InfoflowCFG {

	private final IInfoflowCFG baseCFG;

	public BackwardsInfoflowCFG(IInfoflowCFG baseCFG) {
		super(new BackwardsInterproceduralCFG(baseCFG));
		this.baseCFG = baseCFG;
	}

	public IInfoflowCFG getBaseCFG() {
		return this.baseCFG;
	}

	@Override
	public boolean isStaticFieldRead(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldRead(method, variable);
	}

	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldUsed(method, variable);
	}

	@Override
	public boolean hasSideEffects(SootMethod method) {
		return baseCFG.hasSideEffects(method);
	}

	@Override
	public boolean methodReadsValue(SootMethod m, Value v) {
		return baseCFG.methodReadsValue(m, v);
	}

	@Override
	public boolean methodWritesValue(SootMethod m, Value v) {
		return baseCFG.methodWritesValue(m, v);
	}

	@Override
	public UnitContainer getPostdominatorOf(Unit u) {
		return baseCFG.getPostdominatorOf(u);
	}

	@Override
	public void purge() {
		baseCFG.purge();
		super.purge();
	}

}
