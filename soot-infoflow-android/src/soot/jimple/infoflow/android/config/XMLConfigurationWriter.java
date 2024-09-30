package soot.jimple.infoflow.android.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soot.jimple.infoflow.InfoflowConfiguration.CategoryMode;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.SolverConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.SourceSinkFilterMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.IccConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SourceSinkConfiguration;
import soot.jimple.infoflow.android.data.CategoryDefinition;

/**
 * Writer class for serializing a FlowDroid configuration into an XML file
 * 
 * @author Steven Arzt
 *
 */
public class XMLConfigurationWriter {

	private final InfoflowAndroidConfiguration config;

	/**
	 * Creates a new instance of the {@link XMLConfigurationWriter} class
	 * 
	 * @param config The FlowDroid configuration to write out
	 */
	public XMLConfigurationWriter(InfoflowAndroidConfiguration config) {
		this.config = config;
	}

	/**
	 * Serializes the FlowDroid configuration into an XML string
	 * 
	 * @return The XML data representing the FlowDroid configuration
	 */
	public String write() {
		try {
			// Create a new document
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

			// Create the root document
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement(XMLConstants.TAG_ROOT_ELEMENT);
			document.appendChild(rootElement);

			writeAnalysisFileConfig(document, rootElement);
			writeSourceSinkConfig(document, rootElement);
			writeAndroidConfig(document, rootElement);
			writeIccConfig(document, rootElement);
			writeDataFlowConfig(document, rootElement);

			// Write it out
			StringWriter stringWriter = new StringWriter();

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stringWriter);
			transformer.transform(source, result);
			return stringWriter.toString();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException(ex);
		} catch (TransformerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Writes out the general data flow configuration
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 */
	private void writeDataFlowConfig(Document document, Element parentElement) {
		Element dataFlowConfigTag = document.createElement(XMLConstants.TAG_DATA_FLOW_CONFIGURATION);
		parentElement.appendChild(dataFlowConfigTag);

		PathConfiguration pathConfig = config.getPathConfiguration();
		SolverConfiguration solverConfig = config.getSolverConfiguration();

		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_MAX_JOIN_POINT_ABSTRACTIONS,
				Integer.toString(solverConfig.getMaxJoinPointAbstractions()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_MAX_CALLEES_PER_CALL_SITE,
				Integer.toString(solverConfig.getMaxCalleesPerCallSite()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_IMPLICIT_FLOW_MODE,
				config.getImplicitFlowMode().toString());
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_STATIC_FIELD_TRACKING_MODE,
				config.getStaticFieldTrackingMode().toString());
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_ENABLE_EXCEPTIONS,
				Boolean.toString(config.getEnableExceptionTracking()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_ENABLE_ARRAYS,
				Boolean.toString(config.getEnableArrayTracking()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_ENABLE_REFLECTION,
				Boolean.toString(config.getEnableReflection()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_FLOW_SENSITIVE_ALIASING,
				Boolean.toString(config.getFlowSensitiveAliasing()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_LOG_SOURCES_AND_SINKS,
				Boolean.toString(config.getLogSourcesAndSinks()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_ENABLE_ARRAY_SIZE_TAINTING,
				Boolean.toString(config.getEnableArraySizeTainting()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_PATH_RECONSTRUCTION_MODE,
				pathConfig.getPathReconstructionMode().toString());
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_PATH_AGNOSTIC_RESULTS,
				Boolean.toString(config.getPathAgnosticResults()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_MAX_CALLSTACK_SIZE,
				Integer.toString(pathConfig.getMaxCallStackSize()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_MAX_PATH_LENGTH,
				Integer.toString(pathConfig.getMaxPathLength()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_MAX_PATHS_PER_ABSTRACTION,
				Integer.toString(pathConfig.getMaxPathsPerAbstraction()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_DATA_FLOW_TIMEOUT,
				Long.toString(config.getDataFlowTimeout()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_PATH_RECONSTRUCTION_TIMEOUT,
				Long.toString(pathConfig.getPathReconstructionTimeout()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_PATH_RECONSTRUCTION_BATCH_SIZE,
				Integer.toString(pathConfig.getPathReconstructionBatchSize()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_WRITE_OUTPUT_FILES,
				Boolean.toString(config.getWriteOutputFiles()));
		appendSimpleTag(document, dataFlowConfigTag, XMLConstants.TAG_PATH_RECONSTRUCTION_TOTAL_TIME,
				Long.toString(pathConfig.getPathReconstructionTotalTime()));
	}

	/**
	 * Writes out the configuration specific to inter-component data flow analysis
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 */
	private void writeIccConfig(Document document, Element parentElement) {
		Element iccConfigTag = document.createElement(XMLConstants.TAG_ICC_CONFIGURATION);
		parentElement.appendChild(iccConfigTag);

		IccConfiguration iccConfig = config.getIccConfig();

		appendSimpleTag(document, iccConfigTag, XMLConstants.TAG_ENABLE_ICC_TRACKING,
				Boolean.toString(iccConfig.isIccEnabled()));
		appendSimpleTag(document, iccConfigTag, XMLConstants.TAG_MODEL_FILE, iccConfig.getIccModel());
		appendSimpleTag(document, iccConfigTag, XMLConstants.TAG_PURIFY_RESULTS,
				Boolean.toString(iccConfig.isIccResultsPurifyEnabled()));
	}

	/**
	 * Writes out the Android-specific configuration for the data flow analysis
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 */
	private void writeAndroidConfig(Document document, Element parentElement) {
		Element androidConfigTag = document.createElement(XMLConstants.TAG_ANDROID_CONFIGURATION);
		parentElement.appendChild(androidConfigTag);

		CallbackConfiguration callbackConfig = config.getCallbackConfig();
		SourceSinkConfiguration sourceSinkConfig = config.getSourceSinkConfig();

		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_ENABLE_CALLBACKS,
				Boolean.toString(callbackConfig.getEnableCallbacks()));
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_FILTER_THREAD_CALLBACKS,
				Boolean.toString(callbackConfig.getFilterThreadCallbacks()));
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_MAX_CALLBACKS_PER_COMPONENT,
				Integer.toString(callbackConfig.getMaxCallbacksPerComponent()));
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_MAX_CALLBACK_DEPTH,
				Integer.toString(callbackConfig.getMaxAnalysisCallbackDepth()));
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_LAYOUT_MATCHING_MODE,
				sourceSinkConfig.getLayoutMatchingMode().toString());
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_MERGE_DEX_FILES,
				Boolean.toString(config.getMergeDexFiles()));
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_CALLBACK_SOURCE_MODE,
				sourceSinkConfig.getCallbackSourceMode().toString());
		appendSimpleTag(document, androidConfigTag, XMLConstants.TAG_CALLBACK_ANALYSIS_TIMEOUT,
				Integer.toString(callbackConfig.getCallbackAnalysisTimeout()));
	}

	/**
	 * Writes out the source/sink configuration for the data flow analysis
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 */
	private void writeSourceSinkConfig(Document document, Element parentElement) {
		Element sourceSpecTag = document.createElement(XMLConstants.TAG_SOURCE_SPEC);
		parentElement.appendChild(sourceSpecTag);

		// Write the default mode (include/exclude)
		sourceSpecTag.setAttribute(XMLConstants.ATTR_DEFAULT_MODE,
				config.getSourceSinkConfig().getSourceFilterMode().toString());

		// Write the individually-configured categories
		writeCategoryConfig(document, sourceSpecTag, config.getSourceSinkConfig().getSourceCategoriesAndModes());

		Element sinkSpecTag = document.createElement(XMLConstants.TAG_SINK_SPEC);
		parentElement.appendChild(sinkSpecTag);

		// Write the default mode (include/exclude)
		sinkSpecTag.setAttribute(XMLConstants.ATTR_DEFAULT_MODE,
				config.getSourceSinkConfig().getSinkFilterMode().toString());

		// Write the individually-configured categories
		writeCategoryConfig(document, sinkSpecTag, config.getSourceSinkConfig().getSinkCategoriesAndModes());
	}

	/**
	 * Writes out the source / sink categories explicitly configured for the data
	 * flow analysis
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 * @param categorySpec  The category specification to write out
	 */
	private void writeCategoryConfig(Document document, Element parentElement,
			Map<CategoryDefinition, CategoryMode> categorySpec) {
		for (CategoryDefinition def : categorySpec.keySet()) {
			// Is the mode for this category different from the default mode?
			if (config.getSourceSinkConfig().getSourceFilterMode() == SourceSinkFilterMode.UseAllButExcluded
					&& categorySpec.get(def) == CategoryMode.Include)
				continue;
			if (config.getSourceSinkConfig().getSourceFilterMode() == SourceSinkFilterMode.UseOnlyIncluded
					&& categorySpec.get(def) == CategoryMode.Exclude)
				continue;

			// Create a new <category /> element
			Element categoryTag = document.createElement(XMLConstants.TAG_CATEGORY);
			parentElement.appendChild(categoryTag);

			// Write out the specification data
			categoryTag.setAttribute(XMLConstants.ATTR_ID, def.getCategoryId().toString());
			categoryTag.setAttribute(XMLConstants.ATTR_MODE, categorySpec.get(def).toString());
		}
	}

	/**
	 * Writes out the configuration on the files required for the data flow analysis
	 * 
	 * @param document      The XML document into which to write the configuration
	 * @param parentElement The root element under which to place the configuration
	 */
	private void writeAnalysisFileConfig(Document document, Element parentElement) {
		Element inputFileTag = document.createElement(XMLConstants.TAG_INPUT_FILES);
		parentElement.appendChild(inputFileTag);

		appendSimpleTag(document, inputFileTag, XMLConstants.TAG_TARGET_APK_FILE,
				config.getAnalysisFileConfig().getTargetAPKFile().getAbsolutePath());
		appendSimpleTag(document, inputFileTag, XMLConstants.TAG_SOURCE_SINK_FILE,
				config.getAnalysisFileConfig().getSourceSinkFile().getAbsolutePath());
		appendSimpleTag(document, inputFileTag, XMLConstants.TAG_ANDROID_PLATFORM_DIR,
				config.getAnalysisFileConfig().getAndroidPlatformDir().getAbsolutePath());
		appendSimpleTag(document, inputFileTag, XMLConstants.TAG_OUTPUT_FILE,
				config.getAnalysisFileConfig().getOutputFile());
	}

	/**
	 * Creates a new element with a simple string as contents
	 * 
	 * @param document      The XML document into which to write the new element
	 * @param parentElement The parent element under which to place the new element
	 * @param tagName       The name of the new element
	 */
	private void appendSimpleTag(Document document, Element parentElement, String tagName, String contents) {
		Element newElement = document.createElement(tagName);
		parentElement.appendChild(newElement);
		newElement.setTextContent(contents);
	}

	/**
	 * Writes the FlowDroid configuration into a file
	 * 
	 * @param fileName The full path and file name of the target file
	 * @throws IOException Thrown if there is an error while writing the file
	 */
	public void write(String fileName) throws IOException {
		String xmlData = write();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fileName);
			pw.write(xmlData);
		} finally {
			if (pw != null)
				pw.close();
		}
	}

}
