package soot.jimple.infoflow.solver.memory;

import soot.Unit;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.FlowDroidMemoryManager;

/**
 * A factory class that creates instances of the default FlowDroid memory
 * manager
 * 
 * @author Steven Arzt
 *
 */
public class DefaultMemoryManagerFactory implements IMemoryManagerFactory {

	/**
	 * Constructs a new instance of the AccessPathManager class
	 */
	public DefaultMemoryManagerFactory() {
	}

	@Override
	public IMemoryManager<AbstractDataFlowAbstraction, Unit> getMemoryManager(boolean tracingEnabled) {
		return new FlowDroidMemoryManager(tracingEnabled);
	}

}
