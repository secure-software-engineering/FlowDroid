package soot.jimple.infoflow.memory.reasons;

import soot.jimple.infoflow.memory.ISolverTerminationReason;

/**
 * Reason for terminating a data flow solver that has run out of time
 * 
 * @author Steven Arzt
 *
 */
public class TimeoutReason implements ISolverTerminationReason {

	private final long timeElapsed;
	private final long timeout;

	/**
	 * Creates a new instance of the {@link TimeoutReason} class
	 * 
	 * @param timeElapsed
	 *            The time that has elapsed so far
	 * @param timeout
	 *            The maximum time that the solver was allowed to spend
	 */
	public TimeoutReason(long timeElapsed, long timeout) {
		this.timeElapsed = timeElapsed;
		this.timeout = timeout;
	}

	/**
	 * Gets the time that has elapsed so far
	 * 
	 * @return The time that the solver has spent so far
	 */
	public long getTimeElapsed() {
		return timeElapsed;
	}

	/**
	 * Gets the maximum amount of time that the solver was allowed to spend on the
	 * task
	 * 
	 * @return The maximum amount of time that the solver was allowed to spend on
	 *         the task
	 */
	public long getTimeout() {
		return timeout;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timeElapsed ^ (timeElapsed >>> 32));
		result = prime * result + (int) (timeout ^ (timeout >>> 32));
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
		TimeoutReason other = (TimeoutReason) obj;
		if (timeElapsed != other.timeElapsed)
			return false;
		if (timeout != other.timeout)
			return false;
		return true;
	}

	@Override
	public ISolverTerminationReason combine(ISolverTerminationReason terminationReason) {
		return new MultiReason(this, terminationReason);
	}

}
