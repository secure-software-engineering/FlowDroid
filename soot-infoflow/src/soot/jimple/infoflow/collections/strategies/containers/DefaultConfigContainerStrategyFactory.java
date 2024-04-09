package soot.jimple.infoflow.collections.strategies.containers;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.config.PreciseCollectionStrategy;

/**
 * Selects the container strategy based on the infoflow configuration
 */
public class DefaultConfigContainerStrategyFactory implements IContainerStrategyFactory {

	@Override
	public IContainerStrategy create(InfoflowManager manager) {
		final PreciseCollectionStrategy strategy = manager.getConfig().getPreciseCollectionStrategy();
		switch (strategy) {
		case CONSTANT_MAP_SUPPORT:
			return new ConstantMapStrategy(manager);
		case NONE:
			return null;
		default:
			throw new RuntimeException("Not implemented strategy " + strategy);
		}
	}

}
