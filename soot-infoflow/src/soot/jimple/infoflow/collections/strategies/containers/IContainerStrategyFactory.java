package soot.jimple.infoflow.collections.strategies.containers;

import soot.jimple.infoflow.InfoflowManager;

/**
 * This factory can be used to instantiate an {@link IContainerStrategy}.
 * 
 * @author Marc Miltenberger
 */
public interface IContainerStrategyFactory {

	/**
	 * Creates a new container strategy
	 * @param manager the infoflow manager
	 * @return the container strategy
	 */
	public IContainerStrategy create(InfoflowManager manager);
}
