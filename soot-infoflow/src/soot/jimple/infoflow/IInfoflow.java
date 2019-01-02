/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import soot.jimple.Stmt;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.memory.IMemoryManagerFactory;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintWrapperDataFlowAnalysis;
import soot.jimple.infoflow.threading.IExecutorFactory;

/**
 * interface for the main infoflow class
 *
 */
public interface IInfoflow extends ITaintWrapperDataFlowAnalysis {

	/**
	 * Gets the configuration to be used for the data flow analysis
	 * 
	 * @return The configuration to be used for the data flow analysis
	 */
	public InfoflowConfiguration getConfig();

	/**
	 * Sets the configuration to be used for the data flow analysis
	 * 
	 * @param config The configuration to be used for the data flow analysis
	 */
	public void setConfig(InfoflowConfiguration config);

	/**
	 * Sets the handler class to be used for modeling the effects of native methods
	 * on the taint state
	 * 
	 * @param handler The native call handler to use
	 */
	public void setNativeCallHandler(INativeCallHandler handler);

	/**
	 * List of preprocessors that need to be executed in order before the
	 * information flow.
	 * 
	 * @param preprocessors the pre-processors
	 */
	public void setPreProcessors(Collection<? extends PreAnalysisHandler> preprocessors);

	/**
	 * Sets the set of post-processors that shall be executed after the data flow
	 * analysis has finished
	 * 
	 * @param postprocessors The post-processors to execute on the results
	 */
	public void setPostProcessors(Collection<? extends PostAnalysisHandler> postprocessors);

	/**
	 * Computes the information flow on a list of entry point methods. This list is
	 * used to construct an artificial main method following the Android life cycle
	 * for all methods that are detected to be part of Android's application
	 * infrastructure (e.g. android.app.Activity.onCreate)
	 * 
	 * @param appPath           The path containing the client program's files
	 * @param libPath           The path to the main folder of the (unpacked)
	 *                          library class files
	 * @param entryPointCreator the entry point creator to use for generating the
	 *                          dummy main method
	 * @param sources           list of source class+method (as string conforms to
	 *                          SootMethod representation)
	 * @param sinks             list of sink class+method (as string conforms to
	 *                          SootMethod representation)
	 */
	public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator,
			List<String> sources, List<String> sinks);

	/**
	 * Computes the information flow on a list of entry point methods. This list is
	 * used to construct an artificial main method following the Android life cycle
	 * for all methods that are detected to be part of Android's application
	 * infrastructure (e.g. android.app.Activity.onCreate)
	 * 
	 * @param appPath     The path containing the client program's files
	 * @param libPath     the path to the main folder of the (unpacked) library
	 *                    class files
	 * @param entryPoints the entryPoints (string conforms to SootMethod
	 *                    representation)
	 * @param sources     list of source class+method (as string conforms to
	 *                    SootMethod representation)
	 * @param sinks       list of sink class+method (as string conforms to
	 *                    SootMethod representation)
	 */
	public void computeInfoflow(String appPath, String libPath, Collection<String> entryPoints,
			Collection<String> sources, Collection<String> sinks);

	/**
	 * Computes the information flow on a single method. This method is directly
	 * taken as the entry point into the program, even if it is an instance method.
	 * 
	 * @param appPath    The path containing the client program's files
	 * @param libPath    the path to the main folder of the (unpacked) library class
	 *                   files
	 * @param entryPoint the main method to analyze
	 * @param sources    list of source class+method (as string conforms to
	 *                   SootMethod representation)
	 * @param sinks      list of sink class+method (as string conforms to SootMethod
	 *                   representation)
	 */
	public void computeInfoflow(String appPath, String libPath, String entryPoint, Collection<String> sources,
			Collection<String> sinks);

	/**
	 * Computes the information flow on a list of entry point methods. This list is
	 * used to construct an artificial main method following the Android life cycle
	 * for all methods that are detected to be part of Android's application
	 * infrastructure (e.g. android.app.Activity.onCreate)
	 * 
	 * @param appPath           The path containing the client program's files
	 * @param libPath           the path to the main folder of the (unpacked)
	 *                          library class files
	 * @param entryPointCreator the entry point creator to use for generating the
	 *                          dummy main method
	 * @param sourcesSinks      manager class for identifying sources and sinks in
	 *                          the source code
	 */
	public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator,
			ISourceSinkManager sourcesSinks);

	/**
	 * Computes the information flow on a single method. This method is directly
	 * taken as the entry point into the program, even if it is an instance method.
	 * 
	 * @param appPath      The path containing the client program's files
	 * @param libPath      the path to the main folder of the (unpacked) library
	 *                     class files
	 * @param entryPoint   the main method to analyze
	 * @param sourcesSinks manager class for identifying sources and sinks in the
	 *                     source code
	 */
	public void computeInfoflow(String appPath, String libPath, String entryPoint, ISourceSinkManager sourcesSinks);

	/**
	 * getResults returns the results found by the analysis
	 * 
	 * @return the results
	 */
	public InfoflowResults getResults();

	/**
	 * A result is available if the analysis has finished - so if this method
	 * returns false the analysis has not finished yet or was not started (e.g. no
	 * sources or sinks found)
	 * 
	 * @return boolean that states if a result is available
	 */
	public boolean isResultAvailable();

	public void setIPCManager(IIPCManager ipcManager);

	/**
	 * Sets the Soot configuration callback to be used for this analysis
	 * 
	 * @param config The configuration callback to be used for the analysis
	 */
	public void setSootConfig(IInfoflowConfig config);

	/**
	 * Sets the path builder factory to be used in subsequent data flow analyses
	 * 
	 * @param factory The path bilder factory to use for constructing path
	 *                reconstruction algorithms
	 */
	public void setPathBuilderFactory(IPathBuilderFactory factory);

	/**
	 * Gets the concrete set of sources that have been collected in preparation for
	 * the taint analysis. This method will return null if source and sink logging
	 * has not been enabled (see InfoflowConfiguration. setLogSourcesAndSinks()),
	 * 
	 * @return The set of sources collected for taint analysis
	 */
	public Set<Stmt> getCollectedSources();

	/**
	 * Gets the concrete set of sinks that have been collected in preparation for
	 * the taint analysis. This method will return null if source and sink logging
	 * has not been enabled (see InfoflowConfiguration. setLogSourcesAndSinks()),
	 * 
	 * @return The set of sinks collected for taint analysis
	 */
	public Set<Stmt> getCollectedSinks();

	/**
	 * Adds a handler that is called when information flow results are available
	 * 
	 * @param handler The handler to add
	 */
	public void addResultsAvailableHandler(ResultsAvailableHandler handler);

	/**
	 * Removes a handler that is called when information flow results are available
	 * 
	 * @param handler The handler to remove
	 */
	public void removeResultsAvailableHandler(ResultsAvailableHandler handler);

	/**
	 * Aborts the data flow analysis. This is useful when the analysis controller is
	 * running in a different thread and the main thread (e.g., a GUI) wants to
	 * abort the analysis
	 */
	public void abortAnalysis();

	/**
	 * Sets a handler which is invoked whenever a taint is propagated
	 * 
	 * @param handler The handler to be invoked when propagating taints
	 */
	public void setTaintPropagationHandler(TaintPropagationHandler handler);

	/**
	 * Sets a handler which is invoked whenever an alias is propagated backwards
	 * 
	 * @param handler The handler to be invoked when propagating aliases
	 */
	public void setBackwardsPropagationHandler(TaintPropagationHandler handler);

	/**
	 * Sets the factory to be used for creating memory managers
	 * 
	 * @param factory The memory manager factory to use
	 */
	public void setMemoryManagerFactory(IMemoryManagerFactory factory);

	/**
	 * Sets the factory to be used for creating thread pool executors
	 * 
	 * @param executorFactory The executor factory to use
	 */
	public void setExecutorFactory(IExecutorFactory executorFactory);

	/**
	 * Sets the factory to be used for creating the propagation rule manager, which
	 * can then add features to the core data flow engine
	 * 
	 * @param ruleManagerFactory The factory class for the propagation rule manager
	 */
	public void setPropagationRuleManagerFactory(IPropagationRuleManagerFactory ruleManagerFactory);

}
