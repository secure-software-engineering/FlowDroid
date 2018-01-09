package soot.jimple.infoflow.data.pathBuilders;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowManager;

/**
 * Abstract base class for all abstraction path builders
 * 
 * @author Steven Arzt
 */
public abstract class AbstractAbstractionPathBuilder implements IAbstractionPathBuilder {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final InfoflowManager manager;
	protected final PathConfiguration pathConfig;
	protected Set<OnPathBuilderResultAvailable> resultAvailableHandlers = null;

	/**
	 * Creates a new instance of the {@link AbstractAbstractionPathBuilder} class
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 */
	public AbstractAbstractionPathBuilder(InfoflowManager manager) {
		this.manager = manager;
		this.pathConfig = manager.getConfig().getPathConfiguration();
	}

	@Override
	public void addResultAvailableHandler(OnPathBuilderResultAvailable handler) {
		if (this.resultAvailableHandlers == null)
			this.resultAvailableHandlers = new HashSet<>();
		this.resultAvailableHandlers.add(handler);
	}

}
