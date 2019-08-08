package soot.jimple.infoflow.android.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.InfoflowConfiguration.SolverConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.AnalysisFileConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackSourceMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CategoryMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.IccConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.LayoutMatchingMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SourceSinkConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SourceSinkFilterMode;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.android.data.CategoryDefinition.CATEGORY;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;

/**
 * Parser class for reading the FlowDroid configuration from an XML file
 * 
 * @author Steven Arzt
 *
 */
public class XMLConfigurationParser {

	// XML stuff incl. Verification against XSD
	private static final String XSD_FILE_PATH = "schema/FlowDroidConfiguration.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	private final InputStream xmlStream;

	/**
	 * Enumeration containing all XML elements in the schema
	 * 
	 * @author Steven Arzt
	 *
	 */
	private enum XMLSection {
		CONFIGURATION, INPUT_FILES, SOURCES, SINKS, ANDROID_CONFIGURATION, ICC_CONFIGURATION, DATA_FLOW_CONFIGURATION,

		DUMMY
	}

	/**
	 * Handler class for the XML parser
	 * 
	 * @author Steven Arzt
	 *
	 */
	private class SAXHandler extends DefaultHandler {

		private final InfoflowAndroidConfiguration config;
		private String currentElement = "";
		private Stack<XMLSection> parseStack = new Stack<>();
		private boolean enableIccTracking = false;

		public SAXHandler(InfoflowAndroidConfiguration config) {
			this.config = config;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			XMLSection stackElement = parseStack.isEmpty() ? null : parseStack.peek();

			if (parseStack.isEmpty()) {
				if (qName.equals(XMLConstants.TAG_ROOT_ELEMENT))
					parseStack.push(XMLSection.CONFIGURATION);
			} else if (stackElement == XMLSection.CONFIGURATION) {
				if (qName.equals(XMLConstants.TAG_INPUT_FILES))
					parseStack.push(XMLSection.INPUT_FILES);
				else if (qName.equals(XMLConstants.TAG_SOURCE_SPEC)) {
					parseStack.push(XMLSection.SOURCES);

					String strDefaultMode = attributes.getValue(XMLConstants.ATTR_DEFAULT_MODE);
					if (strDefaultMode != null && !strDefaultMode.isEmpty())
						config.getSourceSinkConfig().setSourceFilterMode(SourceSinkFilterMode.valueOf(strDefaultMode));
				} else if (qName.equals(XMLConstants.TAG_SINK_SPEC)) {
					parseStack.push(XMLSection.SINKS);

					String strDefaultMode = attributes.getValue(XMLConstants.ATTR_DEFAULT_MODE);
					if (strDefaultMode != null && !strDefaultMode.isEmpty())
						config.getSourceSinkConfig().setSinkFilterMode(SourceSinkFilterMode.valueOf(strDefaultMode));
				} else if (qName.equals(XMLConstants.TAG_ANDROID_CONFIGURATION))
					parseStack.push(XMLSection.ANDROID_CONFIGURATION);
				else if (qName.equals(XMLConstants.TAG_ICC_CONFIGURATION))
					parseStack.push(XMLSection.ICC_CONFIGURATION);
				else if (qName.equals(XMLConstants.TAG_DATA_FLOW_CONFIGURATION))
					parseStack.push(XMLSection.DATA_FLOW_CONFIGURATION);
			} else if (stackElement == XMLSection.SOURCES) {
				if (qName.equals(XMLConstants.TAG_CATEGORY)) {
					String strId = attributes.getValue(XMLConstants.ATTR_ID);
					String strCustomId = attributes.getValue(XMLConstants.ATTR_CUSTOM_ID);
					String strMode = attributes.getValue(XMLConstants.ATTR_MODE);

					CategoryDefinition catDef = new CategoryDefinition(CATEGORY.valueOf(strId), strCustomId);
					config.getSourceSinkConfig().addSourceCategory(catDef, CategoryMode.valueOf(strMode));
				}
			} else if (stackElement == XMLSection.SINKS) {
				if (qName.equals(XMLConstants.TAG_CATEGORY)) {
					String strId = attributes.getValue(XMLConstants.ATTR_ID);
					String strCustomId = attributes.getValue(XMLConstants.ATTR_CUSTOM_ID);
					String strMode = attributes.getValue(XMLConstants.ATTR_MODE);

					CategoryDefinition catDef = new CategoryDefinition(CATEGORY.valueOf(strId), strCustomId);
					config.getSourceSinkConfig().addSinkCategory(catDef, CategoryMode.valueOf(strMode));
				}
			}

			if (stackElement == XMLSection.INPUT_FILES || stackElement == XMLSection.ANDROID_CONFIGURATION
					|| stackElement == XMLSection.ICC_CONFIGURATION
					|| stackElement == XMLSection.DATA_FLOW_CONFIGURATION || stackElement == XMLSection.SINKS
					|| stackElement == XMLSection.SOURCES)
				currentElement = qName;
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);

			if (currentElement == null || currentElement.isEmpty())
				parseStack.pop();
			currentElement = "";
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);

			final String data = new String(ch, start, length);
			if (!parseStack.isEmpty()) {
				if (parseStack.peek() == XMLSection.INPUT_FILES) {
					AnalysisFileConfiguration fileConfig = config.getAnalysisFileConfig();

					if (currentElement.equals(XMLConstants.TAG_TARGET_APK_FILE))
						fileConfig.setTargetAPKFile(data);
					else if (currentElement.equals(XMLConstants.TAG_SOURCE_SINK_FILE))
						fileConfig.setSourceSinkFile(data);
					else if (currentElement.equals(XMLConstants.TAG_ANDROID_PLATFORM_DIR))
						fileConfig.setAndroidPlatformDir(data);
					else if (currentElement.equals(XMLConstants.TAG_OUTPUT_FILE))
						fileConfig.setOutputFile(data);
				} else if (parseStack.peek() == XMLSection.ANDROID_CONFIGURATION) {
					CallbackConfiguration callbackConfig = config.getCallbackConfig();
					SourceSinkConfiguration sourceSinkConfig = config.getSourceSinkConfig();

					if (currentElement.equals(XMLConstants.TAG_ENABLE_CALLBACKS))
						callbackConfig.setEnableCallbacks(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_FILTER_THREAD_CALLBACKS))
						callbackConfig.setFilterThreadCallbacks(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_CALLBACKS_PER_COMPONENT))
						callbackConfig.setMaxCallbacksPerComponent(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_CALLBACK_DEPTH))
						callbackConfig.setMaxAnalysisCallbackDepth(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_LAYOUT_MATCHING_MODE))
						sourceSinkConfig.setLayoutMatchingMode(LayoutMatchingMode.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MERGE_DEX_FILES))
						config.setMergeDexFiles(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_CALLBACK_SOURCE_MODE))
						sourceSinkConfig.setCallbackSourceMode(CallbackSourceMode.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_CALLBACK_ANALYSIS_TIMEOUT))
						callbackConfig.setCallbackAnalysisTimeout(Integer.valueOf(data));
				} else if (parseStack.peek() == XMLSection.ICC_CONFIGURATION) {
					IccConfiguration iccConfig = config.getIccConfig();

					if (currentElement.equals(XMLConstants.TAG_ENABLE_ICC_TRACKING))
						enableIccTracking = Boolean.valueOf(data);
					else if (currentElement.equals(XMLConstants.TAG_MODEL_FILE))
						iccConfig.setIccModel(data);
					else if (currentElement.equals(XMLConstants.TAG_PURIFY_RESULTS))
						iccConfig.setIccResultsPurify(Boolean.valueOf(data));
				} else if (parseStack.peek() == XMLSection.DATA_FLOW_CONFIGURATION) {
					PathConfiguration pathConfig = config.getPathConfiguration();
					SolverConfiguration solverConfig = config.getSolverConfiguration();

					if (currentElement.equals(XMLConstants.TAG_MAX_JOIN_POINT_ABSTRACTIONS))
						solverConfig.setMaxJoinPointAbstractions(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_CALLEES_PER_CALL_SITE))
						solverConfig.setMaxCalleesPerCallSite(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_IMPLICIT_FLOW_MODE))
						config.setImplicitFlowMode(ImplicitFlowMode.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_STATIC_FIELD_TRACKING_MODE))
						config.setStaticFieldTrackingMode(StaticFieldTrackingMode.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_ENABLE_EXCEPTIONS))
						config.setEnableExceptionTracking(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_ENABLE_ARRAYS))
						config.setEnableArrayTracking(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_ENABLE_REFLECTION))
						config.setEnableReflection(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_FLOW_SENSITIVE_ALIASING))
						config.setFlowSensitiveAliasing(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_LOG_SOURCES_AND_SINKS))
						config.setLogSourcesAndSinks(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_ENABLE_ARRAY_SIZE_TAINTING))
						config.setEnableArraySizeTainting(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_PATH_RECONSTRUCTION_MODE))
						pathConfig.setPathReconstructionMode(PathReconstructionMode.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_PATH_AGNOSTIC_RESULTS))
						InfoflowConfiguration.setPathAgnosticResults(Boolean.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_CALLSTACK_SIZE))
						pathConfig.setMaxCallStackSize(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_PATH_LENGTH))
						pathConfig.setMaxPathLength(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_MAX_PATHS_PER_ABSTRACTION))
						pathConfig.setMaxPathsPerAbstraction(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_DATA_FLOW_TIMEOUT))
						config.setDataFlowTimeout(Long.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_PATH_RECONSTRUCTION_TIMEOUT))
						pathConfig.setPathReconstructionTimeout(Long.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_PATH_RECONSTRUCTION_TIMEOUT))
						pathConfig.setPathReconstructionTimeout(Long.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_PATH_RECONSTRUCTION_BATCH_SIZE))
						pathConfig.setPathReconstructionBatchSize(Integer.valueOf(data));
					else if (currentElement.equals(XMLConstants.TAG_WRITE_OUTPUT_FILES))
						config.setWriteOutputFiles(Boolean.valueOf(data));
				}
			}
		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();

			if (!enableIccTracking)
				config.getIccConfig().setIccModel(null);
		}

	}

	/**
	 * Creates a new {@link XMLConfigurationParser} from an XML configuration file
	 * 
	 * @param fileName The full path and file name of the configuration file to read
	 * @return The parser that was initialized with the given XML file
	 * @throws IOException Thrown if the given file could not be read
	 */
	public static XMLConfigurationParser fromFile(String fileName) throws IOException {
		if (!verifyXML(fileName)) {
			throw new RuntimeException("The XML-File isn't valid");
		}
		FileInputStream inputStream = new FileInputStream(fileName);
		return fromStream(inputStream);
	}

	/**
	 * Creates a new {@link XMLConfigurationParser} from an input stream
	 * 
	 * @param inputStream The stream that contains the XML data to read
	 * @return The parser that was initialized with the given XML file
	 * @throws IOException Thrown if the given file could not be read
	 */
	public static XMLConfigurationParser fromStream(InputStream inputStream) throws IOException {
		XMLConfigurationParser pmp = new XMLConfigurationParser(inputStream);
		return pmp;
	}

	/**
	 * Creates a new instance of the {@link XMLConfigurationParser} class
	 * 
	 * @param stream The stream from which to read the XML data
	 */
	private XMLConfigurationParser(InputStream stream) {
		this.xmlStream = stream;
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data
	 * format.
	 * 
	 * @param fileName of the XML
	 * @return true = valid XML false = invalid XML
	 * @throws IOException
	 */
	private static boolean verifyXML(String fileName) throws IOException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		StreamSource xsdFile = new StreamSource(ResourceUtils.getResourceStream(XSD_FILE_PATH));
		StreamSource xmlFile = new StreamSource(new File(fileName));
		boolean validXML = false;
		try {
			Schema schema = sf.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			try {
				validator.validate(xmlFile);
				validXML = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!validXML) {
				new IOException("File isn't  valid against the xsd");
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} finally {
			xsdFile.getInputStream().close();
			// When using a file, this may be null
			if (xmlFile.getInputStream() != null)
				xmlFile.getInputStream().close();
		}
		return validXML;
	}

	/**
	 * Parses the configuration file and fills the data into the given object
	 * 
	 * @param config The configuration object to fill with the data read from the
	 *               XML file
	 */
	public void parse(InfoflowAndroidConfiguration config) {
		// Parse the data
		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			SAXParser parser = pf.newSAXParser();
			parser.parse(xmlStream, new SAXHandler(config));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the stream on which this parser operates
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		xmlStream.close();
	}

}
