package soot.jimple.infoflow.codeOptimization;

import java.util.Collection;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Interface for all code optimization implementations
 * 
 * @author Steven Arzt
 *
 */
public interface ICodeOptimizer {

	/**
	 * Initializes the code optimizer. This method is called before any actual
	 * optimizations are carried out.
	 * 
	 * @param config
	 *            The data flow analyzer's configuration
	 */
	public void initialize(InfoflowConfiguration config);

	/**
	 * Runs the coe optimization
	 * 
	 * @param manager
	 *            The data flow manager for interacting with the solver
	 * @param entryPoints
	 *            The set of entry points for the data flow analysis
	 * @param sourcesSinks
	 *            The SourceSinkManager
	 * @param taintWrapper
	 *            The taint wrapper
	 */
	public void run(InfoflowManager manager, Collection<SootMethod> entryPoints, ISourceSinkManager sourcesSinks,
			ITaintPropagationWrapper taintWrapper);

}
