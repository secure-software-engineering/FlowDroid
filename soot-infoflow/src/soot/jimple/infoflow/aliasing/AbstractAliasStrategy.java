package soot.jimple.infoflow.aliasing;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowManager;

/**
 * Common base class for alias strategies
 * 
 * @author Steven Arzt
 */
public abstract class AbstractAliasStrategy implements IAliasingStrategy {

	protected final InfoflowManager manager;
	
	public AbstractAliasStrategy(InfoflowManager manager) {
		this.manager = manager;
	}
	
	@Override
	public boolean hasProcessedMethod(SootMethod method) {
		return true;
	}

}
