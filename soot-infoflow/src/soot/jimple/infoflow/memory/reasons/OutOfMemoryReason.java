package soot.jimple.infoflow.memory.reasons;

import soot.jimple.infoflow.memory.ISolverTerminationReason;

/**
 * Reason for terminating a data flow solver that has run out of memory
 * 
 * @author Steven Arzt
 *
 */
public class OutOfMemoryReason implements ISolverTerminationReason {

	private final long currentMemory;

	/**
	 * Creates a new instance of the {@link OutOfMemoryReason} class
	 * 
	 * @param currentMemory
	 *            The amount of memory that is currently in use
	 */
	public OutOfMemoryReason(long currentMemory) {
		this.currentMemory = currentMemory;
	}

	public long getCurrentMemory() {
		return currentMemory;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (currentMemory ^ (currentMemory >>> 32));
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
		OutOfMemoryReason other = (OutOfMemoryReason) obj;
		if (currentMemory != other.currentMemory)
			return false;
		return true;
	}

	@Override
	public ISolverTerminationReason combine(ISolverTerminationReason terminationReason) {
		return new MultiReason(this, terminationReason);
	}

}
