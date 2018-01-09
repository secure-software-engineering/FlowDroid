package soot.jimple.infoflow.aliasing;

import java.util.Set;

import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

public abstract class AbstractInteractiveAliasStrategy extends AbstractAliasStrategy {

	public AbstractInteractiveAliasStrategy(InfoflowManager manager) {
		super(manager);
	}

	@Override
	public void computeAliasTaints
			(final Abstraction d1, final Stmt src,
			final Value targetValue, Set<Abstraction> taintSet,
			SootMethod method, Abstraction newAbs) {
		// nothing to do here
	}
	
}
