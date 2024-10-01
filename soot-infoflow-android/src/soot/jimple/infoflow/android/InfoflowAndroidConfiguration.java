package soot.jimple.infoflow.android;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.data.CategoryDefinition;

/**
 * Configuration class for the Android-specific data flow analysis
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowAndroidConfiguration extends InfoflowConfiguration {

	/**
	 * The configuration for the various files required for the data flow analysis
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class AnalysisFileConfiguration {

		private File targetAPKFile;
		private File sourceSinkFile;
		private File androidPlatformDir;
		private String additionalClasspath = "";
		private String outputFile = "";

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param fileConfig The other configuration object
		 */
		public void merge(AnalysisFileConfiguration fileConfig) {
			this.targetAPKFile = fileConfig.targetAPKFile;
			this.sourceSinkFile = fileConfig.sourceSinkFile;
			this.androidPlatformDir = fileConfig.androidPlatformDir;
			this.additionalClasspath = fileConfig.additionalClasspath;
			this.outputFile = fileConfig.outputFile;
		}

		/**
		 * Checks whether this configuration is valid, i.e., whether there are no
		 * inconsistencies and all necessary data is filled in
		 * 
		 * @return True if this configuration is complete and valid, otherwise false
		 */
		public boolean validate() {
			return targetAPKFile != null && targetAPKFile.exists() && sourceSinkFile != null && sourceSinkFile.exists()
					&& androidPlatformDir != null && androidPlatformDir.exists();
		}

		/**
		 * Gets the target APK file on which the data flow analysis shall be conducted
		 * 
		 * @return The target APK file on which the data flow analysis shall be
		 *         conducted
		 */
		public File getTargetAPKFile() {
			return targetAPKFile;
		}

		/**
		 * Sets the target APK file on which the data flow analysis shall be conducted
		 * 
		 * @param targetAPKFile The target APK file on which the data flow analysis
		 *                      shall be conducted
		 */
		public void setTargetAPKFile(File targetAPKFile) {
			this.targetAPKFile = targetAPKFile;
		}

		/**
		 * Gets the directory in which the Android platform JARs are located
		 * 
		 * @return The directory in which the Android platform JARs are located
		 */
		public File getAndroidPlatformDir() {
			return androidPlatformDir;
		}

		/**
		 * Sets the directory in which the Android platform JARs are located
		 * 
		 * @param androidPlatformDir The directory in which the Android platform JARs
		 *                           are located
		 */
		public void setAndroidPlatformDir(File androidPlatformDir) {
			this.androidPlatformDir = androidPlatformDir;
		}

		/**
		 * Gets the source and sink file
		 * 
		 * @return The source and sink file
		 */
		public File getSourceSinkFile() {
			return sourceSinkFile;
		}

		/**
		 * Sets the source and sink file
		 * 
		 * @param sourceSinkFile The source and sink file
		 */
		public void setSourceSinkFile(File sourceSinkFile) {
			this.sourceSinkFile = sourceSinkFile;
		}

		/**
		 * Gets the additional libraries that are required on the analysis classpath.
		 * FlowDroid will automatically include the target APK file and the Android
		 * platform JAR file, these need not be specified separately.
		 * 
		 * @return The additional libraries that are required on the analysis classpath
		 */
		public String getAdditionalClasspath() {
			return additionalClasspath;
		}

		/**
		 * Gets the additional libraries that are required on the analysis classpath.
		 * FlowDroid will automatically include the target APK file and the Android
		 * platform JAR file, these need not be specified separately.
		 * 
		 * @param additionalClasspath The additional libraries that are required on the
		 *                            analysis classpath
		 */
		public void setAdditionalClasspath(String additionalClasspath) {
			this.additionalClasspath = additionalClasspath;
		}

		/**
		 * Gets the file into which the results of the data flow analysis shall be
		 * written
		 * 
		 * @return The target file into which the results of the data flow analysis
		 *         shall be written
		 */
		public String getOutputFile() {
			return outputFile;
		}

		/**
		 * Sets the file into which the results of the data flow analysis shall be
		 * written
		 * 
		 * @param outputFile The target file into which the results of the data flow
		 *                   analysis shall be written
		 */
		public void setOutputFile(String outputFile) {
			this.outputFile = outputFile;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((additionalClasspath == null) ? 0 : additionalClasspath.hashCode());
			result = prime * result + ((androidPlatformDir == null) ? 0 : androidPlatformDir.hashCode());
			result = prime * result + ((outputFile == null) ? 0 : outputFile.hashCode());
			result = prime * result + ((sourceSinkFile == null) ? 0 : sourceSinkFile.hashCode());
			result = prime * result + ((targetAPKFile == null) ? 0 : targetAPKFile.hashCode());
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
			AnalysisFileConfiguration other = (AnalysisFileConfiguration) obj;
			if (additionalClasspath == null) {
				if (other.additionalClasspath != null)
					return false;
			} else if (!additionalClasspath.equals(other.additionalClasspath))
				return false;
			if (androidPlatformDir == null) {
				if (other.androidPlatformDir != null)
					return false;
			} else if (!androidPlatformDir.equals(other.androidPlatformDir))
				return false;
			if (outputFile == null) {
				if (other.outputFile != null)
					return false;
			} else if (!outputFile.equals(other.outputFile))
				return false;
			if (sourceSinkFile == null) {
				if (other.sourceSinkFile != null)
					return false;
			} else if (!sourceSinkFile.equals(other.sourceSinkFile))
				return false;
			if (targetAPKFile == null) {
				if (other.targetAPKFile != null)
					return false;
			} else if (!targetAPKFile.equals(other.targetAPKFile))
				return false;
			return true;
		}

	}

	/**
	 * The configuration for the inter-component data flow analysis
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class IccConfiguration {

		private boolean iccEnabled = false;
		private String iccModel = null;
		private boolean iccResultsPurify = true;

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param iccConfig The other configuration object
		 */
		public void merge(IccConfiguration iccConfig) {
			this.iccEnabled = iccConfig.iccEnabled;
			this.iccModel = iccConfig.iccModel;
			this.iccResultsPurify = iccConfig.iccResultsPurify;
		}

		public String getIccModel() {
			return iccModel;
		}

		public void setIccModel(String iccModel) {
			this.iccModel = iccModel;
		}

		/**
		 * Gets whether inter-component data flow tracking is enabled or not
		 * 
		 * @return True if inter-component data flow tracking is enabled, otherwise
		 *         false
		 */
		public boolean isIccEnabled() {
			return this.iccModel != null && !this.iccModel.isEmpty();
		}

		/**
		 * Gets whether the ICC results shall be purified after the data flow
		 * computation. Purification means that flows inside components are dropped if
		 * the same flow is also part of an inter-component flow.
		 * 
		 * @return True if the ICC results shall be purified, otherwise false. Note that
		 *         this method also returns false if ICC processing is disabled.
		 */
		public boolean isIccResultsPurifyEnabled() {
			return isIccEnabled() && iccResultsPurify;
		}

		/**
		 * Sets whether the ICC results shall be purified after the data flow
		 * computation. Purification means that flows inside components are dropped if
		 * the same flow is also part of an inter-component flow.
		 * 
		 * @param iccResultsPurify
		 */
		public void setIccResultsPurify(boolean iccResultsPurify) {
			this.iccResultsPurify = iccResultsPurify;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (iccEnabled ? 1231 : 1237);
			result = prime * result + ((iccModel == null) ? 0 : iccModel.hashCode());
			result = prime * result + (iccResultsPurify ? 1231 : 1237);
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
			IccConfiguration other = (IccConfiguration) obj;
			if (iccEnabled != other.iccEnabled)
				return false;
			if (iccModel == null) {
				if (other.iccModel != null)
					return false;
			} else if (!iccModel.equals(other.iccModel))
				return false;
			if (iccResultsPurify != other.iccResultsPurify)
				return false;
			return true;
		}

	}

	/**
	 * The configuration for analyzing callbacks in Android apps
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class CallbackConfiguration {

		private boolean enableCallbacks = true;
		private CallbackAnalyzer callbackAnalyzer = CallbackAnalyzer.Default;
		private boolean filterThreadCallbacks = true;
		private int maxCallbacksPerComponent = 100;
		private int callbackAnalysisTimeout = 0;
		private int maxCallbackAnalysisDepth = -1;
		private boolean serializeCallbacks = false;
		private String callbacksFile = "";

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param cbConfig The other configuration object
		 */
		public void merge(CallbackConfiguration cbConfig) {
			this.enableCallbacks = cbConfig.enableCallbacks;
			this.callbackAnalyzer = cbConfig.callbackAnalyzer;
			this.filterThreadCallbacks = cbConfig.filterThreadCallbacks;
			this.maxCallbacksPerComponent = cbConfig.maxCallbacksPerComponent;
			this.callbackAnalysisTimeout = cbConfig.callbackAnalysisTimeout;
			this.maxCallbackAnalysisDepth = cbConfig.maxCallbackAnalysisDepth;
			this.serializeCallbacks = cbConfig.serializeCallbacks;
			this.callbacksFile = cbConfig.callbacksFile;
		}

		/**
		 * Sets whether the taint analysis shall consider callbacks
		 * 
		 * @param enableCallbacks True if taints shall be tracked through callbacks,
		 *                        otherwise false
		 */
		public void setEnableCallbacks(boolean enableCallbacks) {
			this.enableCallbacks = enableCallbacks;
		}

		/**
		 * Gets whether the taint analysis shall consider callbacks
		 * 
		 * @return True if taints shall be tracked through callbacks, otherwise false
		 */
		public boolean getEnableCallbacks() {
			return this.enableCallbacks;
		}

		/**
		 * Sets the callback analyzer to be used in preparation for the taint analysis
		 * 
		 * @param callbackAnalyzer The callback analyzer to be used
		 */
		public void setCallbackAnalyzer(CallbackAnalyzer callbackAnalyzer) {
			this.callbackAnalyzer = callbackAnalyzer;
		}

		/**
		 * Gets the callback analyzer that is being used in preparation for the taint
		 * analysis
		 * 
		 * @return The callback analyzer being used
		 */
		public CallbackAnalyzer getCallbackAnalyzer() {
			return this.callbackAnalyzer;
		}

		/**
		 * Sets whether the callback analysis algorithm should follow paths that contain
		 * threads. If this option is disabled, callbacks only registered in threads
		 * will be missed. If it is enabled, context-insensitive callgraph algorithms
		 * can lead to a high number of false positives for the callback analyzer.
		 * 
		 * @param filterThreadCallbacks True to discover callbacks registered in
		 *                              threads, otherwise false
		 */
		public void setFilterThreadCallbacks(boolean filterThreadCallbacks) {
			this.filterThreadCallbacks = filterThreadCallbacks;
		}

		/**
		 * Gets whether the callback analysis algorithm should follow paths that contain
		 * threads. If this option is disabled, callbacks only registered in threads
		 * will be missed. If it is enabled, context-insensitive callgraph algorithms
		 * can lead to a high number of false positives for the callback analyzer.
		 * 
		 * @return True to discover callbacks registered in threads, otherwise false
		 */
		public boolean getFilterThreadCallbacks() {
			return this.filterThreadCallbacks;
		}

		/**
		 * Gets the maximum number of callbacks per component. If the callback collector
		 * finds more callbacks than this number for one given component, the analysis
		 * will assume that precision has degraded too much and will analyze this
		 * component without callbacks.
		 * 
		 * @return The maximum number of callbacks per component
		 */
		public int getMaxCallbacksPerComponent() {
			return this.maxCallbacksPerComponent;
		}

		/**
		 * Sets the maximum number of callbacks per component. If the callback collector
		 * finds more callbacks than this number for one given component, the analysis
		 * will assume that precision has degraded too much and will analyze this
		 * component without callbacks.
		 * 
		 * @param maxCallbacksPerComponent The maximum number of callbacks per component
		 */
		public void setMaxCallbacksPerComponent(int maxCallbacksPerComponent) {
			this.maxCallbacksPerComponent = maxCallbacksPerComponent;
		}

		/**
		 * Gets the timeout in seconds after which the callback analysis shall be
		 * stopped. After the timeout, the data flow analysis will continue with those
		 * callbacks that have been found so far.
		 * 
		 * @return The callback analysis timeout in seconds
		 */
		public int getCallbackAnalysisTimeout() {
			return this.callbackAnalysisTimeout;
		}

		/**
		 * Sets the timeout in seconds after which the callback analysis shall be
		 * stopped. After the timeout, the data flow analysis will continue with those
		 * callbacks that have been found so far.
		 * 
		 * @param callbackAnalysisTimeout The callback analysis timeout in seconds
		 */
		public void setCallbackAnalysisTimeout(int callbackAnalysisTimeout) {
			this.callbackAnalysisTimeout = callbackAnalysisTimeout;
		}

		/**
		 * Gets the maximum depth up to which the callback analyzer shall look into
		 * chains of callbacks registering other callbacks. A value equal to or smaller
		 * than zero indicates an infinite maximum depth.
		 * 
		 * @return The maximum depth up to which to look into callback registration
		 *         chains.
		 */
		public int getMaxAnalysisCallbackDepth() {
			return this.maxCallbackAnalysisDepth;
		}

		/**
		 * Sets the maximum depth up to which the callback analyzer shall look into
		 * chains of callbacks registering other callbacks. A value equal to or smaller
		 * than zero indicates an infinite maximum depth.
		 * 
		 * @param maxCallbackAnalysisDepth The maximum depth up to which to look into
		 *                                 callback registration chains.
		 */
		public void setMaxAnalysisCallbackDepth(int maxCallbackAnalysisDepth) {
			this.maxCallbackAnalysisDepth = maxCallbackAnalysisDepth;
		}

		/**
		 * Gets whether the collected callbacks shall be serialized into a file
		 * 
		 * @return True to serialize the collected callbacks into a file, false
		 *         otherwise
		 */
		public boolean isSerializeCallbacks() {
			return serializeCallbacks;
		}

		/**
		 * Sets whether the collected callbacks shall be serialized into a file
		 * 
		 * @param serializeCallbacks True to serialize the collected callbacks into a
		 *                           file, false otherwise
		 */
		public void setSerializeCallbacks(boolean serializeCallbacks) {
			this.serializeCallbacks = serializeCallbacks;
		}

		/**
		 * Gets the full path and file name of the file to which the collected callback
		 * shall be written, or from which they shall be read, respectively
		 * 
		 * @return The file for the collected callbacks
		 */
		public String getCallbacksFile() {
			return callbacksFile;
		}

		/**
		 * Sets the full path and file name of the file to which the collected callback
		 * shall be written, or from which they shall be read, respectively
		 * 
		 * @param callbacksFile The file for the collected callbacks
		 */
		public void setCallbacksFile(String callbacksFile) {
			this.callbacksFile = callbacksFile;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + callbackAnalysisTimeout;
			result = prime * result + ((callbackAnalyzer == null) ? 0 : callbackAnalyzer.hashCode());
			result = prime * result + ((callbacksFile == null) ? 0 : callbacksFile.hashCode());
			result = prime * result + (enableCallbacks ? 1231 : 1237);
			result = prime * result + (filterThreadCallbacks ? 1231 : 1237);
			result = prime * result + maxCallbackAnalysisDepth;
			result = prime * result + maxCallbacksPerComponent;
			result = prime * result + (serializeCallbacks ? 1231 : 1237);
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
			CallbackConfiguration other = (CallbackConfiguration) obj;
			if (callbackAnalysisTimeout != other.callbackAnalysisTimeout)
				return false;
			if (callbackAnalyzer != other.callbackAnalyzer)
				return false;
			if (callbacksFile == null) {
				if (other.callbacksFile != null)
					return false;
			} else if (!callbacksFile.equals(other.callbacksFile))
				return false;
			if (enableCallbacks != other.enableCallbacks)
				return false;
			if (filterThreadCallbacks != other.filterThreadCallbacks)
				return false;
			if (maxCallbackAnalysisDepth != other.maxCallbackAnalysisDepth)
				return false;
			if (maxCallbacksPerComponent != other.maxCallbacksPerComponent)
				return false;
			if (serializeCallbacks != other.serializeCallbacks)
				return false;
			return true;
		}

	}

	/**
	 * The configuration for the source and sink manager
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class SourceSinkConfiguration extends InfoflowConfiguration.SourceSinkConfiguration {

		private Map<CategoryDefinition, CategoryMode> sourceCategories = new HashMap<>();
		private Map<CategoryDefinition, CategoryMode> sinkCategories = new HashMap<>();

		public void merge(SourceSinkConfiguration ssConfig) {
			super.merge(ssConfig);

			this.sourceCategories.putAll(ssConfig.sourceCategories);
			this.sinkCategories.putAll(ssConfig.sinkCategories);
		}

		/**
		 * Gets the explicitly-configured source categories
		 * 
		 * @return The set of source categories for which an explicit configuration has
		 *         been specified
		 */
		public Set<CategoryDefinition> getSourceCategories() {
			return sourceCategories.keySet();
		}

		/**
		 * Gets the explicitly-configured sink categories
		 * 
		 * @return The set of sink categories for which an explicit configuration has
		 *         been specified
		 */
		public Set<CategoryDefinition> getSinkCategories() {
			return sinkCategories.keySet();
		}

		/**
		 * Gets all source categories defined in the configuration together with their
		 * respective modes (included or excluded)
		 * 
		 * @return The source categories defined in the configuration along with their
		 *         respective modes
		 */
		public Map<CategoryDefinition, CategoryMode> getSourceCategoriesAndModes() {
			return this.sourceCategories;
		}

		/**
		 * Gets all sink categories defined in the configuration together with their
		 * respective modes (included or excluded)
		 * 
		 * @return The sink categories defined in the configuration along with their
		 *         respective modes
		 */
		public Map<CategoryDefinition, CategoryMode> getSinkCategoriesAndModes() {
			return this.sinkCategories;
		}

		/**
		 * Adds a source category definition to this configuration
		 * 
		 * @param category The category definition
		 * @param mode     The mode that defines whether this category shall be included
		 *                 or excluded
		 */
		public void addSourceCategory(CategoryDefinition category, CategoryMode mode) {
			this.sourceCategories.put(category, mode);
		}

		/**
		 * Adds a sink category definition to this configuration
		 * 
		 * @param category The category definition
		 * @param mode     The mode that defines whether this category shall be included
		 *                 or excluded
		 */
		public void addSinkCategory(CategoryDefinition category, CategoryMode mode) {
			this.sinkCategories.put(category, mode);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * super.hashCode();
			result = prime * result + ((sinkCategories == null) ? 0 : sinkCategories.hashCode());
			result = prime * result + ((sourceCategories == null) ? 0 : sourceCategories.hashCode());
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
			SourceSinkConfiguration other = (SourceSinkConfiguration) obj;
			if (!super.equals(obj))
				return false;
			if (sinkCategories == null) {
				if (other.sinkCategories != null)
					return false;
			} else if (!sinkCategories.equals(other.sinkCategories))
				return false;
			if (sourceCategories == null) {
				if (other.sourceCategories != null)
					return false;
			} else if (!sourceCategories.equals(other.sourceCategories))
				return false;

			return true;
		}

	}

	/**
	 * Enumeration containing the supported callback analyzers
	 */
	public static enum CallbackAnalyzer {
		/**
		 * The highly-precise default analyzer
		 */
		Default,
		/**
		 * An analyzer that favors performance over precision
		 */
		Fast
	}

	private boolean oneComponentAtATime = false;

	private final CallbackConfiguration callbackConfig = new CallbackConfiguration();
	private final SourceSinkConfiguration sourceSinkConfig = new SourceSinkConfiguration();
	private final IccConfiguration iccConfig = new IccConfiguration();
	private final AnalysisFileConfiguration analysisFileConfig = new AnalysisFileConfiguration();

	private boolean mergeDexFiles = true;

	private boolean performConstantPropagation;
	private static boolean createActivityEntryMethods = true;

	public InfoflowAndroidConfiguration() {
		// We need to adapt some of the defaults. Most people don't care about
		// this stuff, but want a faster analysis.
		this.setEnableArraySizeTainting(false);
		this.setInspectSources(false);
		this.setInspectSinks(false);
		this.setIgnoreFlowsInSystemPackages(true);
		this.setExcludeSootLibraryClasses(true);
	}

	@Override
	public void merge(InfoflowConfiguration config) {
		super.merge(config);
		if (config instanceof InfoflowAndroidConfiguration) {
			InfoflowAndroidConfiguration androidConfig = (InfoflowAndroidConfiguration) config;
			this.oneComponentAtATime = androidConfig.oneComponentAtATime;

			this.callbackConfig.merge(androidConfig.callbackConfig);
			this.sourceSinkConfig.merge(androidConfig.sourceSinkConfig);
			this.iccConfig.merge(androidConfig.iccConfig);
			this.analysisFileConfig.merge(androidConfig.analysisFileConfig);

			this.mergeDexFiles = androidConfig.mergeDexFiles;
			this.createActivityEntryMethods = androidConfig.createActivityEntryMethods;
		}
	}

	/**
	 * Gets the configuration that defines how callbacks shall be handled
	 * 
	 * @return The configuration of the callback analyzer
	 */
	public CallbackConfiguration getCallbackConfig() {
		return callbackConfig;
	}

	/**
	 * Gets the configuration of the source/sink manager
	 * 
	 * @return The configuration of the source/sink manager
	 */
	public SourceSinkConfiguration getSourceSinkConfig() {
		return sourceSinkConfig;
	}

	/**
	 * Gets the configuration for the inter-component data flow analysis
	 * 
	 * @return The configuration for the inter-component data flow analysis
	 */
	public IccConfiguration getIccConfig() {
		return this.iccConfig;
	}

	/**
	 * Gets the configuration for the input files (target APK, Android platform
	 * directory, etc.)
	 * 
	 * @return The input file configuration
	 */
	public AnalysisFileConfiguration getAnalysisFileConfig() {
		return analysisFileConfig;
	}

	/**
	 * Sets whether FlowDroid shall analyze one component at a time instead of
	 * generating one big dummy main method containing all components
	 * 
	 * @param oneComponentAtATime True if FlowDroid shall analyze one component at a
	 *                            time, otherwise false
	 */
	public void setOneComponentAtATime(boolean oneComponentAtATime) {
		this.oneComponentAtATime = oneComponentAtATime;
	}

	/**
	 * Gets whether FlowDroid shall analyze one component at a time instead of
	 * generating one biug dummy main method containing all components
	 * 
	 * @return True if FlowDroid shall analyze one component at a time, otherwise
	 *         false
	 */
	public boolean getOneComponentAtATime() {
		return this.oneComponentAtATime;
	}

	/**
	 * Gets whether FlowDroid shall merge all dex files in the APK to get a full
	 * picture of the app
	 * 
	 * @return True if FlowDroid shall merge all dex files in the APK, otherwise
	 *         false
	 */
	public boolean getMergeDexFiles() {
		return this.mergeDexFiles;
	}

	/**
	 * Sets whether FlowDroid shall merge all dex files in the APK to get a full
	 * picture of the app
	 * 
	 * @param mergeDexFiles True if FlowDroid shall merge all dex files in the APK,
	 *                      otherwise false
	 */
	public void setMergeDexFiles(boolean mergeDexFiles) {
		this.mergeDexFiles = mergeDexFiles;
	}

	/**
	 * Gets if Flowdroid should create new Methods when creating the Activity Entry
	 * point
	 * 
	 * @return true/false
	 */
	public static boolean getCreateActivityEntryMethods() {
		return createActivityEntryMethods;
	}

	/**
	 * Sets if Flow Flowdroid should create new Methods when creating the Activity
	 * Entry point
	 * 
	 * @param createActivityEntryMethods boolean that is true if Methods should be
	 *                                   created
	 */
	public static void setCreateActivityEntryMethods(boolean createActivityEntryMethods) {
		InfoflowAndroidConfiguration.createActivityEntryMethods = createActivityEntryMethods;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((analysisFileConfig == null) ? 0 : analysisFileConfig.hashCode());
		result = prime * result + ((callbackConfig == null) ? 0 : callbackConfig.hashCode());
		result = prime * result + ((iccConfig == null) ? 0 : iccConfig.hashCode());
		result = prime * result + (mergeDexFiles ? 1231 : 1237);
		result = prime * result + (oneComponentAtATime ? 1231 : 1237);
		result = prime * result + ((sourceSinkConfig == null) ? 0 : sourceSinkConfig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InfoflowAndroidConfiguration other = (InfoflowAndroidConfiguration) obj;
		if (analysisFileConfig == null) {
			if (other.analysisFileConfig != null)
				return false;
		} else if (!analysisFileConfig.equals(other.analysisFileConfig))
			return false;
		if (callbackConfig == null) {
			if (other.callbackConfig != null)
				return false;
		} else if (!callbackConfig.equals(other.callbackConfig))
			return false;
		if (iccConfig == null) {
			if (other.iccConfig != null)
				return false;
		} else if (!iccConfig.equals(other.iccConfig))
			return false;
		if (mergeDexFiles != other.mergeDexFiles)
			return false;
		if (oneComponentAtATime != other.oneComponentAtATime)
			return false;
		if (sourceSinkConfig == null) {
			if (other.sourceSinkConfig != null)
				return false;
		} else if (!sourceSinkConfig.equals(other.sourceSinkConfig))
			return false;
		return true;
	}

	/**
	 * Returns whether FlowDroid should perform a simple constant propagation prior
	 * to the data flow analysis.
	 * 
	 * @return whether to perform constant propagation
	 */
	public boolean getPerformConstantPropagation() {
		return performConstantPropagation;
	}

	/**
	 * Sets whether FlowDroid should perform a simple constant propagation prior to
	 * the data flow analysis.
	 * 
	 * @param value true if constants should be propagated
	 */
	public void setPerformConstantPropagation(boolean value) {
		performConstantPropagation = value;
	}
}
