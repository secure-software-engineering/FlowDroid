package soot.jimple.infoflow.solver.memory;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.FlowDroidMemoryManager;
import soot.jimple.infoflow.data.FlowDroidMemoryManager.PathDataErasureMode;

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
	public IMemoryManager<Abstraction, Unit> getMemoryManager(boolean tracingEnabled,
			PathDataErasureMode erasePathData) {
		return new FlowDroidMemoryManager(tracingEnabled, erasePathData);
	}

}
