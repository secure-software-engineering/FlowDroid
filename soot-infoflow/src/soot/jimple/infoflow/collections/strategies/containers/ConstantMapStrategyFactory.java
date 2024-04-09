package soot.jimple.infoflow.collections.strategies.containers;

import soot.jimple.infoflow.InfoflowManager;

/**
 * This factory can be used to instantiate a {@link ConstantMapStrategy}.
 * 
 * @author Marc Miltenberger
 */
public class ConstantMapStrategyFactory implements IContainerStrategyFactory {

	@Override
	public IContainerStrategy create(InfoflowManager manager) {
		return new ConstantMapStrategy(manager);
	}

}
