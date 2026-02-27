package soot.jimple.infoflow.memory.reasons;

import soot.jimple.infoflow.memory.ISolverTerminationReason;

/**
 * Utility class for dealing with reasons why an IFDS solver was terminated
 * 
 * @author Steven Arzt
 */
public class SolverTerminationReasons {

	/**
	 * Checks whether the given reason for terminating the data flow solver is
	 * memory-related
	 * 
	 * @param reason The solver termination reason to check
	 * @return True if the given reason is related to memory, false otherwise
	 */
	public static boolean isMemoryRelatedTermination(ISolverTerminationReason reason) {
		if (reason instanceof OutOfMemoryReason)
			return true;
		if (reason instanceof MultiReason) {
			MultiReason multiReason = (MultiReason) reason;
			return multiReason.hasReason(OutOfMemoryReason.class);
		}
		return false;
	}

}
