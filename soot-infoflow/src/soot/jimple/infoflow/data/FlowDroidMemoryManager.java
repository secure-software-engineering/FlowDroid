package soot.jimple.infoflow.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Unit;
import soot.jimple.infoflow.solver.memory.IMemoryManager;

/**
 * Memory manager implementation for FlowDroid
 * 
 * @author Steven Arzt
 *
 */
public class FlowDroidMemoryManager implements IMemoryManager<AbstractDataFlowAbstraction, Unit> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Special class for encapsulating taint abstractions for a full equality check
	 * including those fields (predecessor, etc.) that are normally left out
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class AbstractionCacheKey {

		private final TaintAbstraction abs;

		public AbstractionCacheKey(TaintAbstraction abs) {
			this.abs = abs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * abs.hashCode();
			result = prime * result + ((abs.getCurrentStmt() == null) ? 0 : abs.getCurrentStmt().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbstractionCacheKey other = (AbstractionCacheKey) obj;

			if (!abs.equals(other.abs))
				return false;
			if (abs.getCurrentStmt() != other.abs.getCurrentStmt())
				return false;

			return true;
		}

	}

	private ConcurrentMap<AccessPath, AccessPath> apCache = new ConcurrentHashMap<>();
	private ConcurrentHashMap<AbstractionCacheKey, TaintAbstraction> absCache = new ConcurrentHashMap<>();
	private AtomicInteger reuseCounter = new AtomicInteger();

	private final boolean tracingEnabled;
	private boolean useAbstractionCache = false;

	/**
	 * Constructs a new instance of the AccessPathManager class
	 */
	public FlowDroidMemoryManager() {
		this(false);
	}

	/**
	 * Constructs a new instance of the AccessPathManager class
	 * 
	 * @param tracingEnabled True if performance tracing data shall be recorded
	 */
	public FlowDroidMemoryManager(boolean tracingEnabled) {
		this.tracingEnabled = tracingEnabled;

		logger.info("Initializing FlowDroid memory manager...");
		if (this.tracingEnabled)
			logger.info("FDMM: Tracing enabled. This may negatively affect performance.");
	}

	/**
	 * Gets the cached equivalent of the given access path
	 * 
	 * @param ap The access path for which to get the cached equivalent
	 * @return The cached equivalent of the given access path
	 */
	private AccessPath getCachedAccessPath(AccessPath ap) {
		AccessPath oldAP = apCache.putIfAbsent(ap, ap);
		if (oldAP == null)
			return ap;

		// We can re-use an old access path
		if (tracingEnabled && oldAP != ap)
			reuseCounter.incrementAndGet();
		return oldAP;
	}

	/**
	 * Gets a cached equivalent abstraction for the given abstraction if we have
	 * one, otherwise returns null
	 * 
	 * @param abs The abstraction for which to perform a cache lookup
	 * @return The cached abstraction equivalent to the given one of it exists,
	 *         otherwise null
	 */
	private TaintAbstraction getCachedAbstraction(TaintAbstraction abs) {
		TaintAbstraction oldAbs = absCache.putIfAbsent(new AbstractionCacheKey(abs), abs);
		if (oldAbs != null && oldAbs != abs)
			if (tracingEnabled)
				reuseCounter.incrementAndGet();
		return oldAbs;
	}

	/**
	 * Gets the number of access paths that have been re-used through caching
	 * 
	 * @return The number of access paths that have been re-used through caching
	 */
	public int getReuseCount() {
		return this.reuseCounter.get();
	}

	@Override
	public AbstractDataFlowAbstraction handleMemoryObject(AbstractDataFlowAbstraction obj) {
		if (obj instanceof TaintAbstraction) {
			TaintAbstraction taint = (TaintAbstraction) obj;

			if (useAbstractionCache) {
				// We check for a cached version of the complete abstraction
				TaintAbstraction cachedAbs = getCachedAbstraction(taint);
				if (cachedAbs != null)
					return cachedAbs;
			}

			// We check for a cached version of the access path
			AccessPath newAP = getCachedAccessPath(taint.getAccessPath());
			taint.setAccessPath(newAP);
		}

		return obj;
	}

	@Override
	public AbstractDataFlowAbstraction handleGeneratedMemoryObject(AbstractDataFlowAbstraction input,
			AbstractDataFlowAbstraction output) {
		// We we just pass the same object on, there is nothing to optimize
		if (input == output) {
			return output;
		}

		if (output instanceof TaintAbstraction && input instanceof TaintAbstraction) {
			TaintAbstraction inputTaint = (TaintAbstraction) input;
			TaintAbstraction outputTaint = (TaintAbstraction) output;

			// If the abstraction didn't change at all, we can use the old one
			if (input.equals(output)) {
				if (outputTaint.getCurrentStmt() == null || inputTaint.getCurrentStmt() == outputTaint.getCurrentStmt())
					return input;
				if (inputTaint.getCurrentStmt() == null) {
					synchronized (input) {
						if (inputTaint.getCurrentStmt() == null) {
							inputTaint.setCurrentStmt(outputTaint.getCurrentStmt());
							return input;
						}
					}
				}
			}
		}

		return output;
	}

	/**
	 * Sets whether the memory manager shall use the abstraction cache
	 * 
	 * @param useAbstractionCache True if the abstraction cache shall be used,
	 *                            otherwise false
	 */
	public void setUseAbstractionCache(boolean useAbstractionCache) {
		this.useAbstractionCache = useAbstractionCache;
	}

}
