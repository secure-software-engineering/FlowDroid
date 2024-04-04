package soot.jimple.infoflow.methodSummary.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Configuration class for the data flow summary generator
 * 
 * @author Steven Arzt
 *
 */
public class SummaryGeneratorConfiguration extends InfoflowConfiguration {

	/**
	 * Defines under which circumstances the return value of a method shall be
	 * tainted.
	 */
	public static enum TaintCondition {
		TaintAlways, TaintOnImplicit, TaintNever
	}

	protected boolean loadFullJAR = false;
	protected boolean summarizeFullJAR = false;
	protected String androidPlatformDir = "";

	protected Set<String> excludes = null;
	protected boolean summarizeHashCodeEquals = false;
	protected Map<String, TaintCondition> defaultTaints = new HashMap<>();

	protected boolean validateResults = true;

	protected boolean applySummariesOnTheFly = true;
	protected Set<String> additionalSummaryDirectories;
	protected boolean useDefaultSummaries = true;

	protected long classSummaryTimeout = -1;
	private int repeatCount = 1;

	/**
	 * Creates a new instance of the SummaryGeneratorConfiguration class and
	 * initializes it with the default configuration options
	 */
	public SummaryGeneratorConfiguration() {
		// Set the default data flow configuration
		setEnableExceptionTracking(false);
		setStaticFieldTrackingMode(StaticFieldTrackingMode.None);
		setCodeEliminationMode(CodeEliminationMode.PropagateConstants);
		setIgnoreFlowsInSystemPackages(false);
		setStopAfterFirstFlow(false);
		setEnableArraySizeTainting(false);
		setExcludeSootLibraryClasses(false);
		setWriteOutputFiles(false);

		getPathConfiguration().setPathBuildingAlgorithm(PathBuildingAlgorithm.None);
		getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);

		setStandardDefaultTaints();
	}

	@Override
	public void merge(InfoflowConfiguration config) {
		super.merge(config);
		if (config instanceof SummaryGeneratorConfiguration) {
			SummaryGeneratorConfiguration summaryConfig = (SummaryGeneratorConfiguration) config;
			this.androidPlatformDir = summaryConfig.androidPlatformDir;
			this.loadFullJAR = summaryConfig.loadFullJAR;
			this.summarizeFullJAR = summaryConfig.summarizeFullJAR;

			this.excludes = summaryConfig.excludes == null || summaryConfig.excludes.isEmpty() ? null
					: new HashSet<>(summaryConfig.excludes);
			this.summarizeHashCodeEquals = summaryConfig.summarizeHashCodeEquals;
			this.defaultTaints = summaryConfig.defaultTaints == null || summaryConfig.defaultTaints.isEmpty() ? null
					: new HashMap<>(summaryConfig.defaultTaints);

			this.validateResults = summaryConfig.validateResults;
			this.repeatCount = summaryConfig.repeatCount;
			this.applySummariesOnTheFly = summaryConfig.applySummariesOnTheFly;
			this.useDefaultSummaries = summaryConfig.useDefaultSummaries;

			{
				Set<String> otherAdditionalDirs = summaryConfig.additionalSummaryDirectories;
				this.additionalSummaryDirectories = otherAdditionalDirs == null || otherAdditionalDirs.isEmpty() ? null
						: new HashSet<>(otherAdditionalDirs);
			}

			this.classSummaryTimeout = summaryConfig.classSummaryTimeout;
		}
	}

	/**
	 * Sets the directory in which the Android platform JARs are located. This
	 * option must be set when generating summariers for classes inside an APK file.
	 * 
	 * @param androidPlatformDir The directory in which the Android platform JARs
	 *                           are located
	 */
	public void setAndroidPlatformDir(String androidPlatformDir) {
		this.androidPlatformDir = androidPlatformDir;
	}

	/**
	 * Gets the directory in which the Android platform JARs are located. This
	 * option must be set when generating summariers for classes inside an APK file.
	 * 
	 * @return The directory in which the Android platform JARs are located
	 */
	public String getAndroidPlatformDir() {
		return androidPlatformDir;
	}

	/**
	 * Sets whether the target JAR file shall be loaded fully before the analysis
	 * starts. More precisely, this instructs StubDroid to not only explicitly load
	 * the target classes, but put the whole target JAR into Soot's process
	 * directory. This is, for instance, useful when analyzing all classes derived
	 * from a certain superclass.
	 * 
	 * @param loadFullJAR True if the target JAR file shall be fully loaded before
	 *                    performing the analysis, otherwise false.
	 */
	public void setLoadFullJAR(boolean loadFullJAR) {
		this.loadFullJAR = loadFullJAR;
	}

	/**
	 * Gets whether the target JAR file shall be loaded fully before the analysis
	 * starts. More precisely, this instructs StubDroid to not only explicitly load
	 * the target classes, but put the whole target JAR into Soot's process
	 * directory. This is, for instance, useful when analyzing all classes derived
	 * from a certain superclass.
	 * 
	 * @return True if the target JAR file shall be fully loaded before performing
	 *         the analysis, otherwise false.
	 */
	public boolean getLoadFullJAR() {
		return this.loadFullJAR;
	}

	/**
	 * Sets whether all classes from the target JAR file shall be summarized.
	 * 
	 * @param summarizeFullJAR True if all classes from the target JAR file shall be
	 *                         summarized, otherwise false.
	 */
	public void setSummarizeFullJAR(boolean summarizeFullJAR) {
		this.summarizeFullJAR = summarizeFullJAR;
	}

	/**
	 * Gets whether all classes from the target JAR file shall be summarized.
	 * 
	 * @return True if all classes from the target JAR file shall be summarized,
	 *         otherwise false.
	 */
	public boolean getSummarizeFullJAR() {
		return this.summarizeFullJAR;
	}

	/**
	 * Sets the set of classes to be excluded from the analysis. Use pkg.* to
	 * exclude all classes in package "pkg"
	 * 
	 * @param excludes The set of classes and packages to be excluded
	 */
	public void setExcludes(Set<String> excludes) {
		this.excludes = excludes;
	}

	/**
	 * Gets the set of classes to be excluded from the analysis.
	 * 
	 * @return The set of classes and packages to be excluded
	 */
	public Set<String> getExcludes() {
		return this.excludes;
	}

	/**
	 * Gets whether hashCode() and equals() methods and other methods with default
	 * taints should also be summarized
	 * 
	 * @return True if hashCode() and equals() methods and other methods with
	 *         default taints shall be summarized, false otherwise
	 */
	public boolean getSummarizeHashCodeEquals() {
		return summarizeHashCodeEquals;
	}

	/**
	 * Sets whether hashCode() and equals() methods and other methods with default
	 * taints should also be summarized
	 * 
	 * @param summarizeHashCodeEquals True if hashCode() and equals() methods and
	 *                                other methods with default taints shall be
	 *                                summarized, false otherwise
	 */
	public void setSummarizeHashCodeEquals(boolean summarizeHashCodeEquals) {
		this.summarizeHashCodeEquals = summarizeHashCodeEquals;
	}

	/**
	 * Gets the map of methods with default taint conditions. When
	 * summarizeHashCodeEquals is true, the return value of the methods are tainted
	 * according to the given taint conditions instead of analyzing the methods.
	 * 
	 * @return the default taint conditions
	 */
	public Map<String, TaintCondition> getDefaultTaints() {
		return defaultTaints;
	}

	/**
	 * Sets the map of methods with default taint conditions. When
	 * summarizeHashCodeEquals is true, the return value of the methods are tainted
	 * according to the given taint conditions instead of analyzing the methods.
	 * 
	 * @param defaultTaints The map with methods and taint conditions that shall be
	 *                      used
	 */
	public void setDefaultTaints(Map<String, TaintCondition> defaultTaints) {
		this.defaultTaints = defaultTaints;
	}

	/**
	 * Adds a method and a corresponding taint condition to the map. When
	 * summarizeHashCodeEquals is true, the return value of the method is tainted
	 * according to the given taint condition instead of analyzing the method.
	 * 
	 * @param method         The method for which the taint condition shall be used
	 * @param taintCondition The taint condition that describes, when the return
	 *                       value shall be tainted
	 */
	public void addDefaultTaint(String method, TaintCondition taintCondition) {
		this.defaultTaints.put(method, taintCondition);
	}

	/**
	 * Adds hashCode(), equals(), compare() and compareTo() to the map of methods
	 * with taint conditions.
	 */
	public void setStandardDefaultTaints() {
		defaultTaints.put("int hashCode()", TaintCondition.TaintAlways);
		defaultTaints.put("boolean equals(java.lang.Object)", TaintCondition.TaintOnImplicit);
		defaultTaints.put("int compare(java.lang.Object,java.lang.Object)", TaintCondition.TaintOnImplicit);
		defaultTaints.put("int compareTo(java.lang.Object)", TaintCondition.TaintOnImplicit);
	}

	/**
	 * Sets the number of time the analysis of every class shall be repeated. This
	 * is useful for measurements and evaluations.
	 * 
	 * @param repeatCount The number of time the analysis of every class shall be
	 *                    repeated
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * Gets the number of time the analysis of every class shall be repeated. This
	 * is useful for measurements and evaluations.
	 * 
	 * @return The number of time the analysis of every class shall be repeated
	 */
	public int getRepeatCount() {
		return this.repeatCount;
	}

	/**
	 * Sets whether the computed data flows shall be validated
	 * 
	 * @param validateResults True if the computed data flows shall be validated,
	 *                        otherwise false
	 */
	public void setValidateResults(boolean validateResults) {
		this.validateResults = validateResults;
	}

	/**
	 * Gets whether the computed data flows shall be validated
	 * 
	 * @return True if the computed data flows shall be validated, otherwise false
	 */
	public boolean getValidateResults() {
		return this.validateResults;
	}

	/**
	 * Gets whether the summary generator shall apply the generated summaries on the
	 * fly. With this option enabled, the classes to be processed will be sorted
	 * according to their dependencies.When processing the second class, it will
	 * apply the summary of the first class.
	 * 
	 * @return True if the generated summaries shall be applied on the fly, false
	 *         otherwise
	 */
	public boolean getApplySummariesOnTheFly() {
		return applySummariesOnTheFly;
	}

	/**
	 * Sets whether the summary generator shall apply the generated summaries on the
	 * fly. With this option enabled, the classes to be processed will be sorted
	 * according to their dependencies.When processing the second class, it will
	 * apply the summary of the first class.
	 * 
	 * @param applySummariesOnTheFly True if the generated summaries shall be
	 *                               applied on the fly, false otherwise
	 */
	public void setApplySummariesOnTheFly(boolean applySummariesOnTheFly) {
		this.applySummariesOnTheFly = applySummariesOnTheFly;
	}

	/**
	 * Gets the directories in which the summary generator shall look for existing
	 * summaries to integrate
	 * 
	 * @return The directories from which existing summaries shall be loaded
	 */
	public Set<String> getAdditionalSummaryDirectories() {
		return additionalSummaryDirectories;
	}

	/**
	 * Sets whether the default summaries shall be applied while generating more
	 * summaries
	 * 
	 * @param useDefaultSummaries True to use the default summaries for the JDK and
	 *                            well-known libraries when generating new
	 *                            summaries, false to only rely on the
	 *                            implementation (and potentially user-defined
	 *                            additional summary directories)
	 */
	public void setUseDefaultSummaries(boolean useDefaultSummaries) {
		this.useDefaultSummaries = useDefaultSummaries;
	}

	/**
	 * Gets whether the default summaries shall be applied while generating more
	 * summaries
	 * 
	 * @return True to use the default summaries for the JDK and well-known
	 *         libraries when generating new summaries, false to only rely on the
	 *         implementation (and potentially user-defined additional summary
	 *         directories)
	 */
	public boolean isUseDefaultSummaries() {
		return useDefaultSummaries;
	}

	/**
	 * Adds an additional directory in which the summary generator shall look for
	 * existing summaries to speed up the generation of new ones
	 * 
	 * @param directory The directory from which to load the summaries
	 */
	public void addAdditionalSummaryDirectory(String directory) {
		if (additionalSummaryDirectories == null)
			additionalSummaryDirectories = new HashSet<>();
		additionalSummaryDirectories.add(directory);
	}

	/**
	 * Gets the timeout after which StubDroid shall abort generating summaries for a
	 * single class
	 * 
	 * @return The timeout, in seconds, after which StubDroid shall abort generating
	 *         summaries for a single class
	 */
	public long getClassSummaryTimeout() {
		return classSummaryTimeout;
	}

	/**
	 * Sets the timeout after which StubDroid shall abort generating summaries for a
	 * single class
	 * 
	 * @param classSummaryTimeout The timeout, in seconds, after which StubDroid
	 *                            shall abort generating summaries for a single
	 *                            class
	 */
	public void setClassSummaryTimeout(long classSummaryTimeout) {
		this.classSummaryTimeout = classSummaryTimeout;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((additionalSummaryDirectories == null) ? 0 : additionalSummaryDirectories.hashCode());
		result = prime * result + ((androidPlatformDir == null) ? 0 : androidPlatformDir.hashCode());
		result = prime * result + (applySummariesOnTheFly ? 1231 : 1237);
		result = prime * result + (int) (classSummaryTimeout ^ (classSummaryTimeout >>> 32));
		result = prime * result + ((excludes == null) ? 0 : excludes.hashCode());
		result = prime * result + (loadFullJAR ? 1231 : 1237);
		result = prime * result + (summarizeFullJAR ? 1231 : 1237);
		result = prime * result + repeatCount;
		result = prime * result + (summarizeHashCodeEquals ? 1231 : 1237);
		result = prime * result + ((defaultTaints == null) ? 0 : defaultTaints.hashCode());
		result = prime * result + (useDefaultSummaries ? 1231 : 1237);
		result = prime * result + (validateResults ? 1231 : 1237);
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
		SummaryGeneratorConfiguration other = (SummaryGeneratorConfiguration) obj;
		if (additionalSummaryDirectories == null) {
			if (other.additionalSummaryDirectories != null)
				return false;
		} else if (!additionalSummaryDirectories.equals(other.additionalSummaryDirectories))
			return false;
		if (androidPlatformDir == null) {
			if (other.androidPlatformDir != null)
				return false;
		} else if (!androidPlatformDir.equals(other.androidPlatformDir))
			return false;
		if (applySummariesOnTheFly != other.applySummariesOnTheFly)
			return false;
		if (classSummaryTimeout != other.classSummaryTimeout)
			return false;
		if (excludes == null) {
			if (other.excludes != null)
				return false;
		} else if (!excludes.equals(other.excludes))
			return false;
		if (loadFullJAR != other.loadFullJAR)
			return false;
		if (summarizeFullJAR != other.summarizeFullJAR)
			return false;
		if (repeatCount != other.repeatCount)
			return false;
		if (summarizeHashCodeEquals != other.summarizeHashCodeEquals)
			return false;
		if (defaultTaints == null) {
			if (other.defaultTaints != null)
				return false;
		} else if (!defaultTaints.equals(other.defaultTaints))
			return false;
		if (useDefaultSummaries != other.useDefaultSummaries)
			return false;
		if (validateResults != other.validateResults)
			return false;
		return true;
	}

}
