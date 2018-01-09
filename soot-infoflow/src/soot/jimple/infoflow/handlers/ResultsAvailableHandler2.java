package soot.jimple.infoflow.handlers;

import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

/**
 * Extended handler that is used to notify clients not only when the data flow
 * analysis has finished, but also when individual results are available.
 * 
 * @author Steven Arzt
 *
 */
public interface ResultsAvailableHandler2 extends ResultsAvailableHandler {
	
	/**
	 * Notifies the handler that a new data flow result is available
	 * @param source The source from which the data flow originated
	 * @param sinks The sink at which the data flow ended
	 * @return True if the data flow analysis shall continue, otherwise false
	 */
	public boolean onSingleResultAvailable(ResultSourceInfo source,
			ResultSinkInfo sinks);

}
