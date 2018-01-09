package soot.jimple.infoflow.handlers;

import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Handler that allows clients to execute certain tasks after the data flow
 * analysis has run. Clients can then modify the results before they are
 * returned to the ResultsAvailableHandler callbacks.
 * 
 * @author Steven Arzt
 *
 */
public interface PostAnalysisHandler {
	
	/**
	 * This method is called after the data flow analysis has finished.
	 * @param results The data flow results
	 * @param cfg The interprocedural data flow graph
	 * @return The new data flow results, potentially changed by the client
	 */
	public InfoflowResults onResultsAvailable(InfoflowResults results,
			IInfoflowCFG cfg);

}
