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
package soot.jimple.infoflow.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import heros.solver.Pair;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.IccConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SootIntegrationMode;
import soot.jimple.infoflow.android.callbacks.AbstractCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.callbacks.DefaultCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.FastCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.filters.AlienFragmentFilter;
import soot.jimple.infoflow.android.callbacks.filters.AlienHostComponentFilter;
import soot.jimple.infoflow.android.callbacks.filters.ApplicationCallbackFilter;
import soot.jimple.infoflow.android.callbacks.filters.UnreachableConstructorFilter;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.android.data.AndroidMemoryManager;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.android.entryPointCreators.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.android.iccta.IccInstrumenter;
import soot.jimple.infoflow.android.iccta.IccResults;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.android.results.xml.InfoflowResultsSerializer;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.android.source.ConfigurationBasedCategoryFilter;
import soot.jimple.infoflow.android.source.UnsupportedSourceSinkFormatException;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.FlowDroidMemoryManager.PathDataErasureMode;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.results.InfoflowPerformanceData;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.rifl.RIFLSourceSinkDefinitionProvider;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.solver.memory.IMemoryManagerFactory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintWrapperDataFlowAnalysis;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class SetupApplication implements ITaintWrapperDataFlowAnalysis {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected ISourceSinkDefinitionProvider sourceSinkProvider;
	protected MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
	protected MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();

	protected InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

	protected Set<SootClass> entrypoints = null;
	protected Set<String> callbackClasses = null;
	protected AndroidEntryPointCreator entryPointCreator = null;
	protected IccInstrumenter iccInstrumenter = null;

	protected ARSCFileParser resources = null;
	protected ProcessManifest manifest = null;
	protected IValueProvider valueProvider = null;

	protected final boolean forceAndroidJar;
	protected ITaintPropagationWrapper taintWrapper;

	protected ISourceSinkManager sourceSinkManager = null;

	protected IInfoflowConfig sootConfig = new SootConfigForAndroid();
	protected BiDirICFGFactory cfgFactory = null;

	protected IIPCManager ipcManager = null;

	protected Set<Stmt> collectedSources = null;
	protected Set<Stmt> collectedSinks = null;

	protected String callbackFile = "AndroidCallbacks.txt";
	protected SootClass scView = null;

	protected Set<PreAnalysisHandler> preprocessors = new HashSet<>();
	protected Set<ResultsAvailableHandler> resultsAvailableHandlers = new HashSet<>();
	protected TaintPropagationHandler taintPropagationHandler = null;
	protected TaintPropagationHandler backwardsPropagationHandler = null;

	protected IInPlaceInfoflow infoflow = null;

	/**
	 * Class for aggregating the data flow results obtained through multiple runs of
	 * the data flow solver.
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class MultiRunResultAggregator implements ResultsAvailableHandler {

		private final InfoflowResults aggregatedResults = new InfoflowResults();
		private InfoflowResults lastResults = null;
		private IInfoflowCFG lastICFG = null;

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			this.aggregatedResults.addAll(results);
			this.lastResults = results;
			this.lastICFG = cfg;
		}

		/**
		 * Gets all data flow results aggregated so far
		 * 
		 * @return All data flow results aggregated so far
		 */
		public InfoflowResults getAggregatedResults() {
			return this.aggregatedResults;
		}

		/**
		 * Gets the total number of source-to-sink connections from the last partial
		 * result that was added to this aggregator
		 * 
		 * @return The results from the last run of the data flow analysis
		 */
		public InfoflowResults getLastResults() {
			return this.lastResults;
		}

		/**
		 * Clears the stored result set from the last data flow run
		 */
		public void clearLastResults() {
			this.lastResults = null;
			this.lastICFG = null;
		}

		/**
		 * Gets the ICFG that was returned together with the last set of data flow
		 * results
		 * 
		 * @return The ICFG that was returned together with the last set of data flow
		 *         results
		 */
		public IInfoflowCFG getLastICFG() {
			return this.lastICFG;
		}

	}

	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param config The data flow configuration to use
	 */
	public SetupApplication(InfoflowAndroidConfiguration config) {
		this(config, null);
	}

	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar      The path to the Android SDK's "platforms" directory if
	 *                        Soot shall automatically select the JAR file to be
	 *                        used or the path to a single JAR file to force one.
	 * @param apkFileLocation The path to the APK file to be analyzed
	 */
	public SetupApplication(String androidJar, String apkFileLocation) {
		this(getConfig(androidJar, apkFileLocation));
	}

	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar      The path to the Android SDK's "platforms" directory if
	 *                        Soot shall automatically select the JAR file to be
	 *                        used or the path to a single JAR file to force one.
	 * @param apkFileLocation The path to the APK file to be analyzed
	 * @param ipcManager      The IPC manager to use for modeling inter-component
	 *                        and inter-application data flows
	 */
	public SetupApplication(String androidJar, String apkFileLocation, IIPCManager ipcManager) {
		this(getConfig(androidJar, apkFileLocation), ipcManager);
	}

	/**
	 * Creates a basic data flow configuration with only the input files set
	 * 
	 * @param androidJar      The path to the Android SDK's "platforms" directory if
	 *                        Soot shall automatically select the JAR file to be
	 *                        used or the path to a single JAR file to force one.
	 * @param apkFileLocation The path to the APK file to be analyzed
	 * @return The new configuration
	 */
	private static InfoflowAndroidConfiguration getConfig(String androidJar, String apkFileLocation) {
		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		config.getAnalysisFileConfig().setTargetAPKFile(apkFileLocation);
		config.getAnalysisFileConfig().setAndroidPlatformDir(androidJar);
		return config;
	}

	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param config     The data flow configuration to use
	 * @param ipcManager The IPC manager to use for modelling inter-component and
	 *                   inter-application data flows
	 */
	public SetupApplication(InfoflowAndroidConfiguration config, IIPCManager ipcManager) {
		this.config = config;
		this.ipcManager = ipcManager;

		// We can use either a specific platform JAR file or automatically
		// select the right one
		if (config.getSootIntegrationMode() == SootIntegrationMode.CreateNewInstace) {
			String platformDir = config.getAnalysisFileConfig().getAndroidPlatformDir();
			if (platformDir == null || platformDir.isEmpty())
				throw new RuntimeException("Android platform directory not specified");
			File f = new File(platformDir);
			this.forceAndroidJar = f.isFile();
		} else {
			this.forceAndroidJar = false;
		}
	}

	/**
	 * Gets the set of sinks loaded into FlowDroid These are the sinks as they are
	 * defined through the SourceSinkManager.
	 * 
	 * @return The set of sinks loaded into FlowDroid
	 */
	public Set<SourceSinkDefinition> getSinks() {
		return this.sourceSinkProvider == null ? null : this.sourceSinkProvider.getSinks();
	}

	/**
	 * Gets the concrete instances of sinks that have been collected inside the app.
	 * This method returns null if source and sink logging has not been enabled (see
	 * InfoflowConfiguration.setLogSourcesAndSinks()).
	 * 
	 * @return The set of concrete sink instances in the app
	 */
	public Set<Stmt> getCollectedSinks() {
		return collectedSinks;
	}

	/**
	 * Prints the list of sinks registered with FlowDroud to stdout
	 */
	public void printSinks() {
		if (this.sourceSinkProvider == null) {
			logger.error("Sinks not calculated yet");
			return;
		}
		logger.info("Sinks:");
		for (SourceSinkDefinition am : getSinks()) {
			logger.info(String.format("- %s", am.toString()));
		}
		logger.info("End of Sinks");
	}

	/**
	 * Gets the set of sources loaded into FlowDroid. These are the sources as they
	 * are defined through the SourceSinkManager.
	 * 
	 * @return The set of sources loaded into FlowDroid
	 */
	public Set<SourceSinkDefinition> getSources() {
		return this.sourceSinkProvider == null ? null : this.sourceSinkProvider.getSources();
	}

	/**
	 * Gets the concrete instances of sources that have been collected inside the
	 * app. This method returns null if source and sink logging has not been enabled
	 * (see InfoflowConfiguration.setLogSourcesAndSinks()).
	 * 
	 * @return The set of concrete source instances in the app
	 */
	public Set<Stmt> getCollectedSources() {
		return collectedSources;
	}

	/**
	 * Prints the list of sources registered with FlowDroud to stdout
	 */
	public void printSources() {
		if (this.sourceSinkProvider == null) {
			logger.error("Sources not calculated yet");
			return;
		}
		logger.info("Sources:");
		for (SourceSinkDefinition am : getSources()) {
			logger.info(String.format("- %s", am.toString()));
		}
		logger.info("End of Sources");
	}

	/**
	 * Gets the set of classes containing entry point methods for the lifecycle
	 * 
	 * @return The set of classes containing entry point methods for the lifecycle
	 */
	public Set<SootClass> getEntrypointClasses() {
		return entrypoints;
	}

	/**
	 * Prints list of classes containing entry points to stdout
	 */
	public void printEntrypoints() {
		if (this.entrypoints == null)
			logger.error("Entry points not initialized");
		else {
			logger.info("Classes containing entry points:");
			for (SootClass sc : entrypoints)
				logger.info("\t" + sc.getName());
			logger.info("End of Entrypoints");
		}
	}

	/**
	 * Sets the class names of callbacks. If this value is null, it automatically
	 * loads the names from AndroidCallbacks.txt as the default behavior.
	 * 
	 * @param callbackClasses The class names of callbacks or null to use the
	 *                        default file.
	 */
	public void setCallbackClasses(Set<String> callbackClasses) {
		this.callbackClasses = callbackClasses;
	}

	public Set<String> getCallbackClasses() {
		return callbackClasses;
	}

	@Override
	public void setTaintWrapper(ITaintPropagationWrapper taintWrapper) {
		this.taintWrapper = taintWrapper;
	}

	@Override
	public ITaintPropagationWrapper getTaintWrapper() {
		return this.taintWrapper;
	}

	/**
	 * Parses common app resources such as the manifest file
	 * 
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	protected void parseAppResources() throws IOException, XmlPullParserException {
		final String targetAPK = config.getAnalysisFileConfig().getTargetAPKFile();

		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		this.manifest = new ProcessManifest(targetAPK);
		Set<String> entryPoints = manifest.getEntryPointClasses();
		this.entrypoints = new HashSet<>(entryPoints.size());
		for (String className : entryPoints) {
			SootClass sc = Scene.v().getSootClassUnsafe(className);
			if (sc != null)
				this.entrypoints.add(sc);
		}

		// Parse the resource file
		long beforeARSC = System.nanoTime();
		this.resources = new ARSCFileParser();
		this.resources.parse(targetAPK);
		logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods
	 * for the given APK file.
	 * 
	 * @param sourcesAndSinks A provider from which the analysis can obtain the list
	 *                        of sources and sinks
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	private void calculateCallbacks(ISourceSinkDefinitionProvider sourcesAndSinks)
			throws IOException, XmlPullParserException {
		calculateCallbacks(sourcesAndSinks, null);
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods
	 * for the entry point in the given APK file.
	 * 
	 * @param sourcesAndSinks A provider from which the analysis can obtain the list
	 *                        of sources and sinks
	 * @param entryPoint      The entry point for which to calculate the callbacks.
	 *                        Pass null to calculate callbacks for all entry points.
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	private void calculateCallbacks(ISourceSinkDefinitionProvider sourcesAndSinks, SootClass entryPoint)
			throws IOException, XmlPullParserException {
		// Add the callback methods
		LayoutFileParser lfp = null;
		if (config.getCallbackConfig().getEnableCallbacks()) {
			if (callbackClasses != null && callbackClasses.isEmpty()) {
				logger.warn("Callback definition file is empty, disabling callbacks");
			} else {
				lfp = createLayoutFileParser();
				switch (config.getCallbackConfig().getCallbackAnalyzer()) {
				case Fast:
					calculateCallbackMethodsFast(lfp, entryPoint);
					break;
				case Default:
					calculateCallbackMethods(lfp, entryPoint);
					break;
				default:
					throw new RuntimeException("Unknown callback analyzer");
				}
			}
		} else if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
			// Create the new iteration of the main method
			createMainMethod(null);
			constructCallgraphInternal();
		}

		logger.info("Entry point calculation done.");

		if (this.sourceSinkProvider != null) {
			// Get the callbacks for the current entry point
			Set<CallbackDefinition> callbacks;
			if (entryPoint == null)
				callbacks = this.callbackMethods.values();
			else
				callbacks = this.callbackMethods.get(entryPoint);

			// Create the SourceSinkManager
			sourceSinkManager = createSourceSinkManager(lfp, callbacks);
		}
	}

	/**
	 * Creates a new layout file parser. Derived classes can override this method to
	 * supply their own parser.
	 * 
	 * @return The newly created layout file parser.
	 */
	protected LayoutFileParser createLayoutFileParser() {
		return new LayoutFileParser(this.manifest.getPackageName(), this.resources);
	}

	/**
	 * Creates an instance of {@link ISourceSinkManager} that defines what FlowDorid
	 * shall consider as a source or sink, respectively.
	 * 
	 * @param lfp       The parser that handles the layout XML files
	 * @param callbacks The callbacks that have been collected so far
	 * @return The new source sink manager
	 */
	protected ISourceSinkManager createSourceSinkManager(LayoutFileParser lfp, Set<CallbackDefinition> callbacks) {
		AccessPathBasedSourceSinkManager sourceSinkManager = new AccessPathBasedSourceSinkManager(
				this.sourceSinkProvider.getSources(), this.sourceSinkProvider.getSinks(), callbacks, config,
				lfp == null ? null : lfp.getUserControlsByID());

		sourceSinkManager.setAppPackageName(this.manifest.getPackageName());
		sourceSinkManager.setResourcePackages(this.resources.getPackages());
		return sourceSinkManager;
	}

	/**
	 * Triggers the callgraph construction in Soot
	 */
	private void constructCallgraphInternal() {
		// If we are configured to use an existing callgraph, we may not replace
		// it. However, we must make sure that there really is one.
		if (config.getSootIntegrationMode() == SootIntegrationMode.UseExistingCallgraph) {
			if (!Scene.v().hasCallGraph())
				throw new RuntimeException("FlowDroid is configured to use an existing callgraph, but there is none");
			return;
		}

		// Do we need ICC instrumentation?
		if (config.getIccConfig().isIccEnabled()) {
			if (iccInstrumenter == null)
				iccInstrumenter = createIccInstrumenter();
			iccInstrumenter.onBeforeCallgraphConstruction();
		}

		// Run the preprocessors
		for (PreAnalysisHandler handler : this.preprocessors)
			handler.onBeforeCallgraphConstruction();

		// Make sure that we don't have any weird leftovers
		releaseCallgraph();

		// If we didn't create the Soot instance, we can't be sure what its callgraph
		// configuration is
		if (config.getSootIntegrationMode() == SootIntegrationMode.UseExistingInstance)
			configureCallgraph();

		// Construct the actual callgraph
		logger.info("Constructing the callgraph...");
		PackManager.v().getPack("cg").apply();

		// ICC instrumentation
		if (iccInstrumenter != null)
			iccInstrumenter.onAfterCallgraphConstruction();

		// Run the preprocessors
		for (PreAnalysisHandler handler : this.preprocessors)
			handler.onAfterCallgraphConstruction();

		// Make sure that we have a hierarchy
		Scene.v().getOrMakeFastHierarchy();
	}

	/**
	 * Creates the ICC instrumentation class
	 * 
	 * @return An instance of the class for instrumenting the app's code for
	 *         inter-component communication
	 */
	protected IccInstrumenter createIccInstrumenter() {
		IccInstrumenter iccInstrumenter;
		iccInstrumenter = new IccInstrumenter(config.getIccConfig().getIccModel(),
				entryPointCreator.getGeneratedMainMethod().getDeclaringClass(),
				entryPointCreator.getComponentToEntryPointInfo());
		return iccInstrumenter;
	}

	/**
	 * Calculates the set of callback methods declared in the XML resource files or
	 * the app's source code
	 * 
	 * @param lfp       The layout file parser to be used for analyzing UI controls
	 * @param component The Android component for which to compute the callbacks.
	 *                  Pass null to compute callbacks for all components.
	 * @throws IOException Thrown if a required configuration cannot be read
	 */
	private void calculateCallbackMethods(LayoutFileParser lfp, SootClass component) throws IOException {
		final CallbackConfiguration callbackConfig = config.getCallbackConfig();

		// Load the APK file
		if (config.getSootIntegrationMode().needsToBuildCallgraph())
			releaseCallgraph();

		// Make sure that we don't have any leftovers from previous runs
		PackManager.v().getPack("wjtp").remove("wjtp.lfp");
		PackManager.v().getPack("wjtp").remove("wjtp.ajc");

		// Get the classes for which to find callbacks
		Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

		// Collect the callback interfaces implemented in the app's
		// source code. Note that the filters should know all components to
		// filter out callbacks even if the respective component is only
		// analyzed later.
		AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
				? new DefaultCallbackAnalyzer(config, entryPointClasses, callbackFile)
				: new DefaultCallbackAnalyzer(config, entryPointClasses, callbackClasses);
		if (valueProvider != null)
			jimpleClass.setValueProvider(valueProvider);
		jimpleClass.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
		jimpleClass.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
		jimpleClass.addCallbackFilter(new UnreachableConstructorFilter());
		jimpleClass.collectCallbackMethods();

		// Find the user-defined sources in the layout XML files. This
		// only needs to be done once, but is a Soot phase.
		lfp.parseLayoutFile(config.getAnalysisFileConfig().getTargetAPKFile());

		// Watch the callback collection algorithm's memory consumption
		FlowDroidMemoryWatcher memoryWatcher = null;
		FlowDroidTimeoutWatcher timeoutWatcher = null;
		if (jimpleClass instanceof IMemoryBoundedSolver) {
			memoryWatcher = new FlowDroidMemoryWatcher(config.getMemoryThreshold());
			memoryWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);

			// Make sure that we don't spend too much time in the callback
			// analysis
			if (callbackConfig.getCallbackAnalysisTimeout() > 0) {
				timeoutWatcher = new FlowDroidTimeoutWatcher(callbackConfig.getCallbackAnalysisTimeout());
				timeoutWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);
				timeoutWatcher.start();
			}
		}

		try {
			int depthIdx = 0;
			boolean hasChanged = true;
			boolean isInitial = true;
			while (hasChanged) {
				hasChanged = false;

				// Check whether the solver has been aborted in the meantime
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled())
						break;
				}

				// Create the new iteration of the main method
				createMainMethod(component);

				// Since the gerenation of the main method can take some time,
				// we check again whether we need to stop.
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled())
						break;
				}

				if (!isInitial) {
					// Reset the callgraph
					releaseCallgraph();

					// We only want to parse the layout files once
					PackManager.v().getPack("wjtp").remove("wjtp.lfp");
				}
				isInitial = false;

				// Run the soot-based operations
				constructCallgraphInternal();
				if (!Scene.v().hasCallGraph())
					throw new RuntimeException("No callgraph in Scene even after creating one. That's very sad "
							+ "and should never happen.");
				PackManager.v().getPack("wjtp").apply();

				// Creating all callgraph takes time and memory. Check whether
				// the solver has been aborted in the meantime
				if (jimpleClass instanceof IMemoryBoundedSolver) {
					if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
						logger.warn("Aborted callback collection because of low memory");
						break;
					}
				}

				// Collect the results of the soot-based phases
				if (this.callbackMethods.putAll(jimpleClass.getCallbackMethods()))
					hasChanged = true;

				if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
					hasChanged = true;

				// Collect the XML-based callback methods
				if (collectXmlBasedCallbackMethods(lfp, jimpleClass))
					hasChanged = true;

				// Avoid callback overruns. If we are beyond the callback limit
				// for one entry point, we may not collect any further callbacks
				// for that entry point.
				if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
					for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt
							.hasNext();) {
						SootClass callbackComponent = componentIt.next();
						if (this.callbackMethods.get(callbackComponent).size() > callbackConfig
								.getMaxCallbacksPerComponent()) {
							componentIt.remove();
							jimpleClass.excludeEntryPoint(callbackComponent);
						}
					}
				}

				// Check depth limiting
				depthIdx++;
				if (callbackConfig.getMaxAnalysisCallbackDepth() > 0
						&& depthIdx >= callbackConfig.getMaxAnalysisCallbackDepth())
					break;

				// If we work with an existing callgraph, the callgraph never
				// changes and thus it doesn't make any sense to go multiple
				// rounds
				if (config.getSootIntegrationMode() == SootIntegrationMode.UseExistingCallgraph)
					break;
			}
		} catch (Exception ex) {
			logger.error("Could not calculate callback methods", ex);
			throw ex;
		} finally {
			// Shut down the watchers
			if (timeoutWatcher != null)
				timeoutWatcher.stop();
			if (memoryWatcher != null)
				memoryWatcher.close();
		}

		// Filter out callbacks that belong to fragments that are not used by
		// the host activity
		AlienFragmentFilter fragmentFilter = new AlienFragmentFilter(invertMap(fragmentClasses));
		fragmentFilter.reset();
		for (Iterator<Pair<SootClass, CallbackDefinition>> cbIt = this.callbackMethods.iterator(); cbIt.hasNext();) {
			Pair<SootClass, CallbackDefinition> pair = cbIt.next();

			// Check whether the filter accepts the given mapping
			if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod()))
				cbIt.remove();
			else if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod().getDeclaringClass())) {
				cbIt.remove();
			}
		}

		// Avoid callback overruns
		if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
			for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt.hasNext();) {
				SootClass callbackComponent = componentIt.next();
				if (this.callbackMethods.get(callbackComponent).size() > callbackConfig.getMaxCallbacksPerComponent())
					componentIt.remove();
			}
		}

		// Make sure that we don't retain any weird Soot phases
		PackManager.v().getPack("wjtp").remove("wjtp.lfp");
		PackManager.v().getPack("wjtp").remove("wjtp.ajc");

		// Warn the user if we had to abort the callback analysis early
		boolean abortedEarly = false;
		if (jimpleClass instanceof IMemoryBoundedSolver) {
			if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
				logger.warn("Callback analysis aborted early due to time or memory exhaustion");
				abortedEarly = true;
			}
		}
		if (!abortedEarly)
			logger.info("Callback analysis terminated normally");
	}

	/**
	 * Inverts the given {@link MultiMap}. The keys become values and vice versa
	 * 
	 * @param original The map to invert
	 * @return An inverted copy of the given map
	 */
	private <K, V> MultiMap<K, V> invertMap(MultiMap<V, K> original) {
		MultiMap<K, V> newTag = new HashMultiMap<>();
		for (V key : original.keySet())
			for (K value : original.get(key))
				newTag.put(value, key);
		return newTag;
	}

	/**
	 * Releases the callgraph and all intermediate objects associated with it
	 */
	protected void releaseCallgraph() {
		// If we are configured to use an existing callgraph, we may not release
		// it
		if (config.getSootIntegrationMode() == SootIntegrationMode.UseExistingCallgraph)
			return;

		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseReachableMethods();
		G.v().resetSpark();
	}

	/**
	 * Collects the XML-based callback methods, e.g., Button.onClick() declared in
	 * layout XML files
	 * 
	 * @param lfp         The layout file parser
	 * @param jimpleClass The analysis class that gives us a mapping between layout
	 *                    IDs and components
	 * @return True if at least one new callback method has been added, otherwise
	 *         false
	 */
	private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractCallbackAnalyzer jimpleClass) {
		SootMethod smViewOnClick = Scene.v()
				.grabMethod("<android.view.View$OnClickListener: void onClick(android.view.View)>");

		// Collect the XML-based callback methods
		boolean hasNewCallback = false;
		for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
			if (jimpleClass.isExcludedEntryPoint(callbackClass))
				continue;

			Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
			for (Integer classId : classIds) {
				AbstractResource resource = this.resources.findResource(classId);
				if (resource instanceof StringResource) {
					final String layoutFileName = ((StringResource) resource).getValue();

					// Add the callback methods for the given class
					Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
					if (callbackMethods != null) {
						for (String methodName : callbackMethods) {
							final String subSig = "void " + methodName + "(android.view.View)";

							// The callback may be declared directly in the
							// class or in one of the superclasses
							SootClass currentClass = callbackClass;
							while (true) {
								SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
								if (callbackMethod != null) {
									if (this.callbackMethods.put(callbackClass,
											new CallbackDefinition(callbackMethod, smViewOnClick, CallbackType.Widget)))
										hasNewCallback = true;
									break;
								}
								SootClass sclass = currentClass.getSuperclassUnsafe();
								if (sclass == null) {
									logger.error(String.format("Callback method %s not found in class %s", methodName,
											callbackClass.getName()));
									break;
								}
								currentClass = sclass;
							}
						}
					}

					// Add the fragments for this class
					Set<SootClass> fragments = lfp.getFragments().get(layoutFileName);
					if (fragments != null)
						for (SootClass fragment : fragments)
							if (fragmentClasses.put(callbackClass, fragment))
								hasNewCallback = true;

					// For user-defined views, we need to emulate their
					// callbacks
					Set<AndroidLayoutControl> controls = lfp.getUserControls().get(layoutFileName);
					if (controls != null) {
						for (AndroidLayoutControl lc : controls)
							if (!SystemClassHandler.isClassInSystemPackage(lc.getViewClass().getName()))
								registerCallbackMethodsForView(callbackClass, lc);
					}
				} else
					logger.error("Unexpected resource type for layout class");
			}
		}

		// Collect the fragments, merge the fragments created in the code with
		// those declared in Xml files
		if (fragmentClasses.putAll(jimpleClass.getFragmentClasses())) // Fragments
																		// declared
																		// in
																		// code
			hasNewCallback = true;

		return hasNewCallback;
	}

	/**
	 * Calculates the set of callback methods declared in the XML resource files or
	 * the app's source code. This method prefers performance over precision and
	 * scans the code including unreachable methods.
	 * 
	 * @param lfp        The layout file parser to be used for analyzing UI controls
	 * @param entryPoint The entry point for which to calculate the callbacks. Pass
	 *                   null to calculate callbacks for all entry points.
	 * @throws IOException Thrown if a required configuration cannot be read
	 */
	private void calculateCallbackMethodsFast(LayoutFileParser lfp, SootClass component) throws IOException {
		// Construct the current callgraph
		releaseCallgraph();
		createMainMethod(component);
		constructCallgraphInternal();

		// Get the classes for which to find callbacks
		Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

		// Collect the callback interfaces implemented in the app's
		// source code
		AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
				? new FastCallbackAnalyzer(config, entryPointClasses, callbackFile)
				: new FastCallbackAnalyzer(config, entryPointClasses, callbackClasses);
		if (valueProvider != null)
			jimpleClass.setValueProvider(valueProvider);
		jimpleClass.collectCallbackMethods();

		// Collect the results
		this.callbackMethods.putAll(jimpleClass.getCallbackMethods());
		this.entrypoints.addAll(jimpleClass.getDynamicManifestComponents());

		// Find the user-defined sources in the layout XML files. This
		// only needs to be done once, but is a Soot phase.
		lfp.parseLayoutFileDirect(config.getAnalysisFileConfig().getTargetAPKFile());

		// Collect the XML-based callback methods
		collectXmlBasedCallbackMethods(lfp, jimpleClass);

		// Construct the final callgraph
		releaseCallgraph();
		createMainMethod(component);
		constructCallgraphInternal();
	}

	/**
	 * Registers the callback methods in the given layout control so that they are
	 * included in the dummy main method
	 * 
	 * @param callbackClass The class with which to associate the layout callbacks
	 * @param lc            The layout control whose callbacks are to be associated
	 *                      with the given class
	 */
	private void registerCallbackMethodsForView(SootClass callbackClass, AndroidLayoutControl lc) {
		// Ignore system classes
		if (SystemClassHandler.isClassInSystemPackage(callbackClass.getName()))
			return;

		// Get common Android classes
		if (scView == null)
			scView = Scene.v().getSootClass("android.view.View");

		// Check whether the current class is actually a view
		if (!Scene.v().getOrMakeFastHierarchy().canStoreType(lc.getViewClass().getType(), scView.getType()))
			return;

		// There are also some classes that implement interesting callback
		// methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		SootClass sc = lc.getViewClass();
		Map<String, SootMethod> systemMethods = new HashMap<>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
			if (parentClass.getName().startsWith("android."))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.put(sm.getSubSignature(), sm);
		}

		// Scan for methods that overwrite parent class methods
		for (SootMethod sm : sc.getMethods()) {
			if (!sm.isConstructor()) {
				SootMethod parentMethod = systemMethods.get(sm.getSubSignature());
				if (parentMethod != null)
					// This is a real callback method
					this.callbackMethods.put(callbackClass,
							new CallbackDefinition(sm, parentMethod, CallbackType.Widget));
			}
		}
	}

	/**
	 * Creates the main method based on the current callback information, injects it
	 * into the Soot scene.
	 * 
	 * @param The class name of a component to create a main method containing only
	 *            that component, or null to create main method for all components
	 */
	private void createMainMethod(SootClass component) {
		// There is no need to create a main method if we don't want to generate
		// a callgraph
		if (config.getSootIntegrationMode() == SootIntegrationMode.UseExistingCallgraph)
			return;

		// Always update the entry point creator to reflect the newest set
		// of callback methods
		entryPointCreator = createEntryPointCreator(component);
		SootMethod dummyMainMethod = entryPointCreator.createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
		if (!dummyMainMethod.getDeclaringClass().isInScene())
			Scene.v().addClass(dummyMainMethod.getDeclaringClass());

		// addClass() declares the given class as a library class. We need to
		// fix this.
		dummyMainMethod.getDeclaringClass().setApplicationClass();
	}

	/**
	 * Gets the source/sink manager constructed for FlowDroid. Make sure to call
	 * constructCallgraph() first, or you will get a null result.
	 * 
	 * @return FlowDroid's source/sink manager
	 */
	public ISourceSinkManager getSourceSinkManager() {
		return sourceSinkManager;
	}

	/**
	 * Builds the classpath for this analysis
	 * 
	 * @return The classpath to be used for the taint analysis
	 */
	private String getClasspath() {
		final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
		final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();
		final String additionalClasspath = config.getAnalysisFileConfig().getAdditionalClasspath();

		String classpath = forceAndroidJar ? androidJar : Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		if (additionalClasspath != null && !additionalClasspath.isEmpty())
			classpath += File.pathSeparator + additionalClasspath;
		logger.debug("soot classpath: " + classpath);
		return classpath;
	}

	/**
	 * Initializes soot for running the soot-based phases of the application
	 * metadata analysis
	 */
	private void initializeSoot() {
		logger.info("Initializing Soot...");

		final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
		final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();

		// Clean up any old Soot instance we may have
		G.reset();

		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		if (config.getWriteOutputFiles())
			Options.v().set_output_format(Options.output_format_jimple);
		else
			Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		if (forceAndroidJar)
			Options.v().set_force_android_jar(androidJar);
		else
			Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		Options.v().set_keep_line_number(false);
		Options.v().set_keep_offset(false);
		Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
		Options.v().set_process_multiple_dex(config.getMergeDexFiles());
		Options.v().set_ignore_resolution_errors(true);

		// Set the Soot configuration options. Note that this will needs to be
		// done before we compute the classpath.
		if (sootConfig != null)
			sootConfig.setSootOptions(Options.v(), config);

		Options.v().set_soot_classpath(getClasspath());
		Main.v().autoSetOptions();
		configureCallgraph();

		// Load whatever we need
		logger.info("Loading dex files...");
		Scene.v().loadNecessaryClasses();

		// Make sure that we have valid Jimple bodies
		PackManager.v().getPack("wjpp").apply();

		// Patch the callgraph to support additional edges. We do this now,
		// because during callback discovery, the context-insensitive callgraph
		// algorithm would flood us with invalid edges.
		LibraryClassPatcher patcher = new LibraryClassPatcher();
		patcher.patchLibraries();
	}

	/**
	 * Configures the callgraph options for Soot according to FlowDroid's settings
	 */
	protected void configureCallgraph() {
		// Configure the callgraph algorithm
		switch (config.getCallgraphAlgorithm()) {
		case AutomaticSelection:
		case SPARK:
			Options.v().setPhaseOption("cg.spark", "on");
			break;
		case GEOM:
			Options.v().setPhaseOption("cg.spark", "on");
			AbstractInfoflow.setGeomPtaSpecificOptions();
			break;
		case CHA:
			Options.v().setPhaseOption("cg.cha", "on");
			break;
		case RTA:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "rta:true");
			Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
			break;
		case VTA:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "vta:true");
			break;
		default:
			throw new RuntimeException("Invalid callgraph algorithm");
		}
		if (config.getEnableReflection())
			Options.v().setPhaseOption("cg", "types-for-invoke:true");
	}

	/**
	 * Common interface for specialized versions of the {@link Infoflow} class that
	 * allows the data flow analysis to be run inside an existing Soot instance
	 * 
	 * @author Steven Arzt
	 *
	 */
	protected static interface IInPlaceInfoflow extends IInfoflow {

		public void runAnalysis(final ISourceSinkManager sourcesSinks, SootMethod entryPoint);

	}

	/**
	 * Specialized {@link Infoflow} class that allows the data flow analysis to be
	 * run inside an existing Soot instance
	 * 
	 * @author Steven Arzt
	 *
	 */
	protected static class InPlaceInfoflow extends Infoflow implements IInPlaceInfoflow {

		/**
		 * Creates a new instance of the Infoflow class for analyzing Android APK files.
		 * 
		 * @param androidPath                 If forceAndroidJar is false, this is the
		 *                                    base directory of the platform files in
		 *                                    the Android SDK. If forceAndroidJar is
		 *                                    true, this is the full path of a single
		 *                                    android.jar file.
		 * @param forceAndroidJar             True if a single platform JAR file shall
		 *                                    be forced, false if Soot shall pick the
		 *                                    appropriate platform version
		 * @param icfgFactory                 The interprocedural CFG to be used by the
		 *                                    InfoFlowProblem
		 * @param additionalEntryPointMethods Additional methods generated by the entry
		 *                                    point creator that are not directly entry
		 *                                    ypoints on their own
		 */
		public InPlaceInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory,
				Collection<SootMethod> additionalEntryPointMethods) {
			super(androidPath, forceAndroidJar, icfgFactory);
			this.additionalEntryPointMethods = additionalEntryPointMethods;
		}

		@Override
		public void runAnalysis(final ISourceSinkManager sourcesSinks, SootMethod entryPoint) {
			this.dummyMainMethod = entryPoint;
			super.runAnalysis(sourcesSinks);
		}

	}

	/**
	 * Constructs a callgraph only without running the actual data flow analysis. If
	 * you want to run a data flow analysis, do not call this method. Instead, call
	 * runInfoflow() directly, which will take care all necessary prerequisites.
	 */
	public void constructCallgraph() {
		boolean oldRunAnalysis = config.isTaintAnalysisEnabled();
		try {
			config.setTaintAnalysisEnabled(false);

			// If FlowDroid is configured to use an existing callgraph, we cannot create a
			// new one
			if (!config.getSootIntegrationMode().needsToBuildCallgraph())
				throw new RuntimeException("FlowDroid is configured to use an existing callgraph. Please "
						+ "change this option before trying to create a new callgraph.");

			// The runInfoflow method can take a null provider as long as we
			// don't attempt to run a data flow analysis.
			this.runInfoflow((ISourceSinkDefinitionProvider) null);
		} catch (RuntimeException ex) {
			logger.error("Could not construct callgraph", ex);
			throw ex;
		} finally {
			config.setTaintAnalysisEnabled(oldRunAnalysis);
		}
	}

	/**
	 * Runs the data flow analysis.
	 * 
	 * @param sources The methods that shall be considered as sources
	 * @param sinks   The methods that shall be considered as sinks
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults runInfoflow(Set<AndroidMethod> sources, Set<AndroidMethod> sinks)
			throws IOException, XmlPullParserException {
		final Set<SourceSinkDefinition> sourceDefs = new HashSet<>(sources.size());
		final Set<SourceSinkDefinition> sinkDefs = new HashSet<>(sinks.size());

		for (AndroidMethod am : sources)
			sourceDefs.add(new MethodSourceSinkDefinition(am));
		for (AndroidMethod am : sinks)
			sinkDefs.add(new MethodSourceSinkDefinition(am));

		ISourceSinkDefinitionProvider parser = new ISourceSinkDefinitionProvider() {

			@Override
			public Set<SourceSinkDefinition> getSources() {
				return sourceDefs;
			}

			@Override
			public Set<SourceSinkDefinition> getSinks() {
				return sinkDefs;
			}

			@Override
			public Set<SourceSinkDefinition> getAllMethods() {
				Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sourceDefs.size() + sinkDefs.size());
				sourcesSinks.addAll(sourceDefs);
				sourcesSinks.addAll(sinkDefs);
				return sourcesSinks;
			}

		};

		return runInfoflow(parser);
	}

	/**
	 * Runs the data flow analysis.
	 * 
	 * @param sourceSinkFile The full path and file name of the file containing the
	 *                       sources and sinks
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults runInfoflow(String sourceSinkFile) throws IOException, XmlPullParserException {
		if (sourceSinkFile != null && !sourceSinkFile.isEmpty())
			config.getAnalysisFileConfig().setSourceSinkFile(sourceSinkFile);

		return runInfoflow();
	}

	/**
	 * Runs the data flow analysis.
	 * 
	 * @throws IOException            Thrown if the given source/sink file could not
	 *                                be read.
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults runInfoflow() throws IOException, XmlPullParserException {
		// If we don't have a source/sink file by now, we cannot run the data
		// flow analysis
		String sourceSinkFile = config.getAnalysisFileConfig().getSourceSinkFile();
		if (sourceSinkFile == null || sourceSinkFile.isEmpty())
			throw new RuntimeException("No source/sink file specified for the data flow analysis");
		String fileExtension = sourceSinkFile.substring(sourceSinkFile.lastIndexOf("."));
		fileExtension = fileExtension.toLowerCase();

		ISourceSinkDefinitionProvider parser = null;
		try {
			if (fileExtension.equals(".xml")) {
				parser = XMLSourceSinkParser.fromFile(sourceSinkFile,
						new ConfigurationBasedCategoryFilter(config.getSourceSinkConfig()));
			} else if (fileExtension.equals(".txt"))
				parser = PermissionMethodParser.fromFile(sourceSinkFile);
			else if (fileExtension.equals(".rifl"))
				parser = new RIFLSourceSinkDefinitionProvider(sourceSinkFile);
			else
				throw new UnsupportedSourceSinkFormatException("The Inputfile isn't a .txt or .xml file.");
		} catch (SAXException ex) {
			throw new IOException("Could not read XML file", ex);
		}

		return runInfoflow(parser);
	}

	/**
	 * Runs the data flow analysis.
	 * 
	 * @param sourcesAndSinks The sources and sinks of the data flow analysis
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow(ISourceSinkDefinitionProvider sourcesAndSinks) {
		// Reset our object state
		this.collectedSources = config.getLogSourcesAndSinks() ? new HashSet<Stmt>() : null;
		this.collectedSinks = config.getLogSourcesAndSinks() ? new HashSet<Stmt>() : null;
		this.sourceSinkProvider = sourcesAndSinks;
		this.infoflow = null;

		// Perform some sanity checks on the configuration
		if (config.getSourceSinkConfig().getEnableLifecycleSources() && config.getIccConfig().isIccEnabled()) {
			logger.warn("ICC model specified, automatically disabling lifecycle sources");
			config.getSourceSinkConfig().setEnableLifecycleSources(false);
		}

		// Start a new Soot instance
		if (config.getSootIntegrationMode() == SootIntegrationMode.CreateNewInstace) {
			G.reset();
			initializeSoot();
		}

		// Perform basic app parsing
		try {
			parseAppResources();
		} catch (IOException | XmlPullParserException e) {
			logger.error("Callgraph construction failed", e);
			throw new RuntimeException("Callgraph construction failed", e);
		}

		MultiRunResultAggregator resultAggregator = new MultiRunResultAggregator();

		// We need at least one entry point
		if (entrypoints == null || entrypoints.isEmpty()) {
			logger.warn("No entry points");
			return null;
		}

		// In one-component-at-a-time, we do not have a single entry point
		// creator. For every entry point, run the data flow analysis.
		if (config.getOneComponentAtATime()) {
			List<SootClass> entrypointWorklist = new ArrayList<>(entrypoints);
			while (!entrypointWorklist.isEmpty()) {
				SootClass entrypoint = entrypointWorklist.remove(0);
				processEntryPoint(sourcesAndSinks, resultAggregator, entrypointWorklist.size(), entrypoint);
			}
		} else
			processEntryPoint(sourcesAndSinks, resultAggregator, -1, null);

		// Write the results to disk if requested
		serializeResults(resultAggregator.getAggregatedResults(), resultAggregator.getLastICFG());

		// We return the aggregated results
		this.infoflow = null;
		resultAggregator.clearLastResults();
		return resultAggregator.getAggregatedResults();
	}

	/**
	 * Runs the data flow analysis on the given entry point class
	 * 
	 * @param sourcesAndSinks  The sources and sinks on which to run the data flow
	 *                         analysis
	 * @param resultAggregator An object for aggregating the results from the
	 *                         individual data flow runs
	 * @param numEntryPoints   The total number of runs (for logging)
	 * @param entrypoint       The current entry point to analyze
	 */
	protected void processEntryPoint(ISourceSinkDefinitionProvider sourcesAndSinks,
			MultiRunResultAggregator resultAggregator, int numEntryPoints, SootClass entrypoint) {
		long beforeEntryPoint = System.nanoTime();

		// Get rid of leftovers from the last entry point
		resultAggregator.clearLastResults();

		// Perform basic app parsing
		long callbackDuration = System.nanoTime();
		try {
			if (config.getOneComponentAtATime())
				calculateCallbacks(sourcesAndSinks, entrypoint);
			else
				calculateCallbacks(sourcesAndSinks);
		} catch (IOException | XmlPullParserException e) {
			logger.error("Callgraph construction failed: " + e.getMessage(), e);
			throw new RuntimeException("Callgraph construction failed", e);
		}
		callbackDuration = Math.round((System.nanoTime() - callbackDuration) / 1E9);
		logger.info(
				String.format("Collecting callbacks and building a callgraph took %d seconds", (int) callbackDuration));

		final Set<SourceSinkDefinition> sources = getSources();
		final Set<SourceSinkDefinition> sinks = getSinks();
		final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();
		if (config.getOneComponentAtATime())
			logger.info("Running data flow analysis on {} (component {}/{}: {}) with {} sources and {} sinks...",
					apkFileLocation, (entrypoints.size() - numEntryPoints), entrypoints.size(), entrypoint,
					sources == null ? 0 : sources.size(), sinks == null ? 0 : sinks.size());
		else
			logger.info("Running data flow analysis on {} with {} sources and {} sinks...", apkFileLocation,
					sources == null ? 0 : sources.size(), sinks == null ? 0 : sinks.size());

		// Create a new entry point and compute the flows in it. If we
		// analyze all components together, we do not need a new callgraph,
		// but can reuse the one from the callback collection phase.
		if (config.getOneComponentAtATime() && config.getSootIntegrationMode().needsToBuildCallgraph()) {
			createMainMethod(entrypoint);
			constructCallgraphInternal();
		}

		// Create and run the data flow tracker
		infoflow = createInfoflow();
		infoflow.addResultsAvailableHandler(resultAggregator);
		infoflow.runAnalysis(sourceSinkManager, entryPointCreator.getGeneratedMainMethod());

		// Update the statistics
		if (config.getLogSourcesAndSinks() && infoflow.getCollectedSources() != null)
			this.collectedSources.addAll(infoflow.getCollectedSources());
		if (config.getLogSourcesAndSinks() && infoflow.getCollectedSinks() != null)
			this.collectedSinks.addAll(infoflow.getCollectedSinks());

		// Print out the found results
		{
			int resCount = resultAggregator.getLastResults() == null ? 0 : resultAggregator.getLastResults().size();
			if (config.getOneComponentAtATime())
				logger.info("Found {} leaks for component {}", resCount, entrypoint);
			else
				logger.info("Found {} leaks", resCount);
		}

		// Update the performance object with the real data
		{
			InfoflowResults lastResults = resultAggregator.getLastResults();
			if (lastResults != null) {
				InfoflowPerformanceData perfData = lastResults.getPerformanceData();
				perfData.setCallgraphConstructionSeconds((int) callbackDuration);
				perfData.setTotalRuntimeSeconds((int) Math.round((System.nanoTime() - beforeEntryPoint) / 1E9));
			}
		}

		// We don't need the computed callbacks anymore
		this.callbackMethods.clear();
		this.fragmentClasses.clear();

		// Notify our result handlers
		for (ResultsAvailableHandler handler : resultsAvailableHandlers)
			handler.onResultsAvailable(resultAggregator.getLastICFG(), resultAggregator.getLastResults());
	}

	/**
	 * Writes the given data flow results into the configured output file
	 * 
	 * @param results The data flow results to write out
	 * @param cfg     The control flow graph to use for writing out the results
	 */
	private void serializeResults(InfoflowResults results, IInfoflowCFG cfg) {
		String resultsFile = config.getAnalysisFileConfig().getOutputFile();
		if (resultsFile != null && !resultsFile.isEmpty()) {
			InfoflowResultsSerializer serializer = new InfoflowResultsSerializer(cfg, config);
			try {
				serializer.serialize(results, resultsFile);
			} catch (FileNotFoundException ex) {
				System.err.println("Could not write data flow results to file: " + ex.getMessage());
				ex.printStackTrace();
			} catch (XMLStreamException ex) {
				System.err.println("Could not write data flow results to file: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Instantiates and configures the data flow engine
	 * 
	 * @return A properly configured instance of the {@link Infoflow} class
	 */
	private IInPlaceInfoflow createInfoflow() {
		// Some sanity checks
		if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
			if (entryPointCreator == null)
				throw new RuntimeException("No entry point available");
			if (entryPointCreator.getComponentToEntryPointInfo() == null)
				throw new RuntimeException("No information about component entry points available");
		}

		// Get the component lifecycle methods
		Collection<SootMethod> lifecycleMethods = Collections.emptySet();
		if (entryPointCreator != null) {
			ComponentEntryPointCollection entryPoints = entryPointCreator.getComponentToEntryPointInfo();
			if (entryPoints != null)
				lifecycleMethods = entryPoints.getLifecycleMethods();
		}

		// Initialize and configure the data flow tracker
		IInPlaceInfoflow info = createInfoflowInternal(lifecycleMethods);
		if (ipcManager != null)
			info.setIPCManager(ipcManager);
		info.setConfig(config);
		info.setSootConfig(sootConfig);
		info.setTaintWrapper(taintWrapper);
		info.setTaintPropagationHandler(taintPropagationHandler);
		info.setBackwardsPropagationHandler(backwardsPropagationHandler);

		// We use a specialized memory manager that knows about Android
		info.setMemoryManagerFactory(new IMemoryManagerFactory() {

			@Override
			public IMemoryManager<Abstraction, Unit> getMemoryManager(boolean tracingEnabled,
					PathDataErasureMode erasePathData) {
				return new AndroidMemoryManager(tracingEnabled, erasePathData, entrypoints);
			}

		});
		info.setMemoryManagerFactory(null);

		// Inject additional post-processors
		info.setPostProcessors(Collections.singleton(new PostAnalysisHandler() {

			@Override
			public InfoflowResults onResultsAvailable(InfoflowResults results, IInfoflowCFG cfg) {
				// Purify the ICC results if requested
				final IccConfiguration iccConfig = config.getIccConfig();
				if (iccConfig.isIccResultsPurifyEnabled())
					results = IccResults.clean(cfg, results);

				return results;
			}

		}));

		return info;
	}

	/**
	 * Creates the data flow engine on which to run the analysis. Derived classes
	 * can override this method to use other data flow engines.
	 * 
	 * @param lifecycleMethods The set of Android lifecycle methods to consider
	 * @return The data flow engine
	 */
	protected IInPlaceInfoflow createInfoflowInternal(Collection<SootMethod> lifecycleMethods) {
		final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
		return new InPlaceInfoflow(androidJar, forceAndroidJar, cfgFactory, lifecycleMethods);
	}

	/**
	 * Creates the {@link AndroidEntryPointCreator} instance which will later create
	 * the dummy main method for the analysis
	 * 
	 * @param component The single component to include in the dummy main method.
	 *                  Pass null to include all components in the dummy main
	 *                  method.
	 * @return The {@link AndroidEntryPointCreator} responsible for generating the
	 *         dummy main method
	 */
	private AndroidEntryPointCreator createEntryPointCreator(SootClass component) {
		Set<SootClass> components = getComponentsToAnalyze(component);

		// If we we already have an entry point creator, we make sure to clean up our
		// leftovers from previous runs
		if (entryPointCreator == null)
			entryPointCreator = new AndroidEntryPointCreator(manifest, components);
		else {
			entryPointCreator.removeGeneratedMethods(false);
			entryPointCreator.reset();
		}

		MultiMap<SootClass, SootMethod> callbackMethodSigs = new HashMultiMap<>();
		if (component == null) {
			// Get all callbacks for all components
			for (SootClass sc : this.callbackMethods.keySet()) {
				Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
				if (callbackDefs != null)
					for (CallbackDefinition cd : callbackDefs)
						callbackMethodSigs.put(sc, cd.getTargetMethod());
			}
		} else {
			// Get the callbacks for the current component only
			for (SootClass sc : components) {
				Set<CallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
				if (callbackDefs != null)
					for (CallbackDefinition cd : callbackDefs)
						callbackMethodSigs.put(sc, cd.getTargetMethod());
			}
		}
		entryPointCreator.setCallbackFunctions(callbackMethodSigs);
		entryPointCreator.setFragments(fragmentClasses);
		entryPointCreator.setComponents(components);
		return entryPointCreator;
	}

	/**
	 * Gets the components to analyze. If the given component is not null, we assume
	 * that only this component and the application class (if any) shall be
	 * analyzed. Otherwise, all components are to be analyzed.
	 * 
	 * @param component A component class name to only analyze this class and the
	 *                  application class (if any), or null to analyze all classes.
	 * @return The set of classes to analyze
	 */
	private Set<SootClass> getComponentsToAnalyze(SootClass component) {
		if (component == null)
			return this.entrypoints;
		else {
			// We always analyze the application class together with each
			// component
			// as there might be interactions between the two
			Set<SootClass> components = new HashSet<>(2);
			components.add(component);

			String applicationName = manifest.getApplicationName();
			if (applicationName != null && !applicationName.isEmpty())
				components.add(Scene.v().getSootClassUnsafe(applicationName));
			return components;
		}
	}

	/**
	 * Gets the dummy main method that was used for the last callgraph construction.
	 * You need to run a data flow analysis or call constructCallgraph() first,
	 * otherwise you will get a null result.
	 * 
	 * @return The dummy main method
	 */
	public SootMethod getDummyMainMethod() {
		return entryPointCreator.getGeneratedMainMethod();
	}

	/**
	 * Gets the extra Soot configuration options to be used when running the
	 * analysis
	 * 
	 * @return The extra Soot configuration options to be used when running the
	 *         analysis, null if the defaults shall be used
	 */
	public IInfoflowConfig getSootConfig() {
		return this.sootConfig;
	}

	/**
	 * Sets the extra Soot configuration options to be used when running the
	 * analysis
	 * 
	 * @param config The extra Soot configuration options to be used when running
	 *               the analysis, null if the defaults shall be used
	 */
	public void setSootConfig(IInfoflowConfig config) {
		this.sootConfig = config;
	}

	/**
	 * Sets the factory class to be used for constructing interprocedural control
	 * flow graphs
	 * 
	 * @param factory The factory to be used. If null is passed, the default factory
	 *                is used.
	 */
	public void setIcfgFactory(BiDirICFGFactory factory) {
		this.cfgFactory = factory;
	}

	/**
	 * Gets the data flow configuration
	 * 
	 * @return The current data flow configuration
	 */
	public InfoflowAndroidConfiguration getConfig() {
		return this.config;
	}

	public void setCallbackFile(String callbackFile) {
		this.callbackFile = callbackFile;
	}

	/**
	 * Adds custom code to be executed before the taint propagation starts
	 * 
	 * @param preprocessor The callback to invoke before starting the taint
	 *                     propagation
	 */
	public void addPreprocessor(PreAnalysisHandler preprocessor) {
		this.preprocessors.add(preprocessor);
	}

	/**
	 * Adds a new handler that shall be executed when the results of the data flow
	 * analysis are available
	 * 
	 * @param handler The callback to invoke when the data flow results are
	 *                available
	 */
	public void addResultsAvailableHandler(ResultsAvailableHandler handler) {
		this.resultsAvailableHandlers.add(handler);
	}

	/**
	 * Clears the list of handlers that shall be executed when the data flow results
	 * are available
	 */
	public void clearResultsAvailableHandlers() {
		this.resultsAvailableHandlers.clear();
	}

	/**
	 * Aborts the data flow analysis. This is useful when the analysis controller is
	 * running in a different thread and the main thread (e.g., a GUI) wants to
	 * abort the analysis
	 */
	public void abortAnalysis() {
		if (infoflow != null)
			infoflow.abortAnalysis();
	}

	/**
	 * Sets the IPC manager for modeling inter-component and inter-application data
	 * flows
	 * 
	 * @param ipcManager The IPC manager to use for modeling inter-component and
	 *                   inter-application data flows
	 */
	public void setIpcManager(IIPCManager ipcManager) {
		this.ipcManager = ipcManager;
	}

	/**
	 * Sets the taint propagation handler, which will be notified when the data flow
	 * analysis processes an edge. Use this callback if you need a hook into the
	 * low-level data flow analysis.
	 * 
	 * @param taintPropagationHandler The propagation handler to register
	 */
	public void setTaintPropagationHandler(TaintPropagationHandler taintPropagationHandler) {
		this.taintPropagationHandler = taintPropagationHandler;
	}

	/**
	 * Sets the backwards propagation handler, which will be notified when the alias
	 * analysis processes an edge. Use this callback if you need a hook into the
	 * low-level alias analysis.
	 * 
	 * @param backwardsPropagationHandler The propagation handler to register
	 */
	public void setBackwardsPropagationHandler(TaintPropagationHandler backwardsPropagationHandler) {
		this.backwardsPropagationHandler = backwardsPropagationHandler;
	}

	/**
	 * Sets the value provider that extracts constants from the Jimple code
	 * 
	 * @param valueProvider The value provider for extracting constants from the
	 *                      code
	 */
	public void setValueProvider(IValueProvider valueProvider) {
		this.valueProvider = valueProvider;
	}

}
