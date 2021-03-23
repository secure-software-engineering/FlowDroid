package soot.jimple.infoflow.methodSummary.generator.gaps;

import java.util.Collection;
import java.util.Set;

import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * A pseudo gap manager that never creates any gaps
 * 
 * @author Steven Arzt
 *
 */
public class NullGapManager implements IGapManager {

	@Override
	public GapDefinition getOrCreateGapForCall(MethodSummaries flows, Stmt gapCall) {
		return null;
	}

	@Override
	public GapDefinition getGapForCall(Stmt gapCall) {
		return null;
	}

	@Override
	public boolean isLocalReferencedInGap(Local local) {
		return false;
	}

	@Override
	public Set<GapDefinition> getGapDefinitionsForLocalUse(Local local) {
		return null;
	}

	@Override
	public Set<GapDefinition> getGapDefinitionsForLocalDef(Local local) {
		return null;
	}

	@Override
	public boolean needsGapConstruction(Stmt stmt, Abstraction abs, IInfoflowCFG icfg) {
		return false;
	}

	@Override
	public Collection<Stmt> getAllGapStmts() {
		return null;
	}

}
