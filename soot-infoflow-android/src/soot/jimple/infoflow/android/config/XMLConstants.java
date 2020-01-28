package soot.jimple.infoflow.android.config;

/**
 * Constants for the tags and attributes in the XML configuration file
 * 
 * @author Steven Arzt
 *
 */
class XMLConstants {

	public static final String TAG_ROOT_ELEMENT = "configuration";
	public static final String TAG_INPUT_FILES = "inputFiles";

	public static final String TAG_TARGET_APK_FILE = "targetAPK";
	public static final String TAG_SOURCE_SINK_FILE = "sourceSinkFile";
	public static final String TAG_ANDROID_PLATFORM_DIR = "androidPlatform";
	public static final String TAG_OUTPUT_FILE = "outputFile";

	public static final String TAG_SOURCE_SPEC = "sources";
	public static final String TAG_SINK_SPEC = "sinks";

	public static final String TAG_CATEGORY = "category";

	public static final String TAG_ANDROID_CONFIGURATION = "androidConfiguration";
	public static final String TAG_LAYOUT_MATCHING_MODE = "layoutMatchingMode";

	public static final String TAG_ENABLE_CALLBACKS = "enableCallbacks";
	public static final String TAG_FILTER_THREAD_CALLBACKS = "filterThreadCallbacks";
	public static final String TAG_MAX_CALLBACKS_PER_COMPONENT = "maxCallbacksPerComponent";
	public static final String TAG_MAX_CALLBACK_DEPTH = "maxCallbackDepth";

	public static final String TAG_MERGE_DEX_FILES = "mergeDexFiles";
	public static final String TAG_CALLBACK_SOURCE_MODE = "callbackSourceMode";
	public static final String TAG_CALLBACK_ANALYSIS_TIMEOUT = "callbackAnalysisTimeout";

	public static final String TAG_ICC_CONFIGURATION = "iccConfiguration";
	public static final String TAG_ENABLE_ICC_TRACKING = "enableICCTracking";
	public static final String TAG_MODEL_FILE = "modelFile";
	public static final String TAG_PURIFY_RESULTS = "purifyResults";

	public static final String TAG_DATA_FLOW_CONFIGURATION = "dataFlowConfiguration";
	public static final String TAG_MAX_JOIN_POINT_ABSTRACTIONS = "maxJoinPointAbstractions";
	public static final String TAG_MAX_CALLEES_PER_CALL_SITE = "maxCalleesPerCallSite";
	public static final String TAG_IMPLICIT_FLOW_MODE = "implicitFlowMode";
	public static final String TAG_STATIC_FIELD_TRACKING_MODE = "staticFieldTrackingMode";
	public static final String TAG_ENABLE_EXCEPTIONS = "enableExceptions";
	public static final String TAG_ENABLE_ARRAYS = "enableArrays";
	public static final String TAG_ENABLE_REFLECTION = "enableReflection";
	public static final String TAG_ENABLE_LINENUMBERS = "enableLineNumbers";
	public static final String TAG_ENABLE_ORIGINALNAMES = "enableOriginalNames";
	public static final String TAG_FLOW_SENSITIVE_ALIASING = "flowSensitiveAliasing";
	public static final String TAG_LOG_SOURCES_AND_SINKS = "logSourcesAndSinks";
	public static final String TAG_ENABLE_ARRAY_SIZE_TAINTING = "enableArraySizeTainting";
	public static final String TAG_PATH_RECONSTRUCTION_MODE = "pathReconstructionMode";
	public static final String TAG_PATH_AGNOSTIC_RESULTS = "pathAgnosticResults";
	public static final String TAG_MAX_CALLSTACK_SIZE = "maxCallStackSize";
	public static final String TAG_MAX_PATH_LENGTH = "maxPathLength";
	public static final String TAG_MAX_PATHS_PER_ABSTRACTION = "maxPathsPerAbstraction";
	public static final String TAG_DATA_FLOW_TIMEOUT = "dataFlowTimeout";
	public static final String TAG_PATH_RECONSTRUCTION_TIMEOUT = "pathReconstructionTimeout";
	public static final String TAG_PATH_RECONSTRUCTION_BATCH_SIZE = "pathReconstructionBatchSize";
	public static final String TAG_WRITE_OUTPUT_FILES = "writeOutputFiles";

	public static final String ATTR_DEFAULT_MODE = "defaultMode";
	public static final String ATTR_ID = "id";
	public static final String ATTR_CUSTOM_ID = "customId";
	public static final String ATTR_MODE = "mode";

}
