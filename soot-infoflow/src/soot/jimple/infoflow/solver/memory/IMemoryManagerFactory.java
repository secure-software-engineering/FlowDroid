package soot.jimple.infoflow.solver.memory;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.FlowDroidMemoryManager.PathDataErasureMode;

/**
 * Common interface for all memory manager factories
 * 
 * @author Steven Arzt
 *
 */
public interface IMemoryManagerFactory {
	
	/**
	 * Creates a new instance of the memory manager
	 * @param tracingEnabled True if performance tracing data shall be recorded
	 * @param erasePathData Specifies whether data for tracking paths (current
	 * statement, corresponding call site) shall be erased.
	 * @return The memory manager
	 */
	public IMemoryManager<Abstraction, Unit> getMemoryManager(
			boolean tracingEnabled, PathDataErasureMode erasePathData);

}
