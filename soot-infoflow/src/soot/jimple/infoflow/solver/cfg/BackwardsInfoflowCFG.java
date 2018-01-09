package soot.jimple.infoflow.solver.cfg;

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
	
}
