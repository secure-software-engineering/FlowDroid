package soot.jimple.infoflow.aliasing;

import java.util.Set;

import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.TaintAbstraction;

public abstract class AbstractInteractiveAliasStrategy extends AbstractAliasStrategy {

	public AbstractInteractiveAliasStrategy(InfoflowManager manager) {
		super(manager);
	}

	@Override
	public void computeAliasTaints
			(final TaintAbstraction d1, final Stmt src,
			final Value targetValue, Set<TaintAbstraction> taintSet,
			SootMethod method, TaintAbstraction newAbs) {
		// nothing to do here
	}
	
}
