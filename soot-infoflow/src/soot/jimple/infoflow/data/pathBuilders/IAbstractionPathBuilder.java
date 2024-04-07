package soot.jimple.infoflow.data.pathBuilders;

import java.util.Set;

import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

/**
 * Common interface for all path construction algorithms. These algorithms
 * reconstruct the path from the sink to the source.
 * 
 * @author Steven Arzt
 */
public interface IAbstractionPathBuilder extends IMemoryBoundedSolver {

	/**
	 * Callback interface that is triggered whenever a path builder has computed a
	 * result value
	 */
	public interface OnPathBuilderResultAvailable {

		/**
		 * Method that is called when a new source-to-sink mapping is available
		 * 
		 * @param source The source from which the data flow originates
		 * @param sink   The sink at which the data flow ends
		 */
		public void onResultAvailable(ResultSourceInfo source, ResultSinkInfo sink);

	}

	/**
	 * Computes the path of tainted data between the source and the sink
	 * 
	 * @param res The data flow tracker results
	 */
	public void computeTaintPaths(final Set<AbstractionAtSink> res);

	/**
	 * Gets the constructed result paths
	 * 
	 * @return The constructed result paths
	 */
	public InfoflowResults getResults();

	/**
	 * Adds a handler that gets called when the path reconstructor has found a new
	 * source-to-sink connection
	 * 
	 * @param handler The handler to add
	 */
	public void addResultAvailableHandler(OnPathBuilderResultAvailable handler);

	/**
	 * Incrementally runs the path builder after some paths have already been
	 * computed. This method is usually called after the taint propagation has
	 * finished when incremental path building has been used in between.
	 */
	public void runIncrementalPathComputation();

}
