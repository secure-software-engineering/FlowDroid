package soot.jimple.infoflow.solver.memory;

import soot.Unit;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;

/**
 * Common interface for all memory manager factories
 * 
 * @author Steven Arzt
 *
 */
public interface IMemoryManagerFactory {

	/**
	 * Creates a new instance of the memory manager
	 * 
	 * @param tracingEnabled True if performance tracing data shall be recorded
	 * @return The memory manager
	 */
	public IMemoryManager<AbstractDataFlowAbstraction, Unit> getMemoryManager(boolean tracingEnabled);

}
