package soot.jimple.infoflow.android.source.parsers.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.android.data.CategoryDefinition.CATEGORY;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Parses informations from the new Dataformat (XML) with the help of SAX.
 * Returns only a Set of Android Method when calling the function parse. For the
 * AccessPath the class SaxHandler is used.
 * 
 * @author Anna-Katharina Wickert
 * @author Joern Tillmans
 * @author Steven Arzt
 */

public class XMLSourceSinkParser implements ISourceSinkDefinitionProvider {

	private final static Logger logger = LoggerFactory.getLogger(XMLSourceSinkParser.class);

	/**
	 * Filter interface for excluding certain categories from the source/sink
	 * set
	 * 
	 * @author Steven Arzt
	 *
	 */
	public interface ICategoryFilter {

		/**
		 * Checks whether methods from the given category shall be parsed or not
		 * 
		 * @param category
		 *            The category in which the source or sink is defined
		 * @return True if the given category shall be parsed, otherwise false
		 */
		public boolean acceptsCategory(CategoryDefinition category);

		/**
		 * Filters sources and sinks by category
		 * 
		 * @param category
		 *            The category in which the source or sink is defined
		 * @param sourceSinkType
		 *            Specifies whether the category is used for sources or
		 *            sinks
		 * @return The acceptable use of the given category. This is a reduction
		 *         from the given type. If the requested type, for example, is
		 *         "Both", this method may return "Source", if the category
		 *         shall only be used for sources, but not for sinks.
		 */
		public SourceSinkType filter(CategoryDefinition category, SourceSinkType sourceSinkType);

	}

	/**
	 * Handler class for the XML parser
	 * 
	 * @author Anna-Katharina Wickert
	 * @author Joern Tillmans
	 * @author Steven Arzt
	 *
	 */
	private class SAXHandler extends DefaultHandler {

		// Holding temporary values for handling with SAX
		private String methodSignature = null;
		private String fieldSignature = null;

		private CategoryDefinition category;
		private boolean isSource, isSink;
		private List<String> pathElements;
		private List<String> pathElementTypes;
		private int paramIndex;
		private List<String> paramTypes = new ArrayList<String>();
		private CallType callType;

		private String accessPathParentElement = "";
		private String description = "";

		private Set<AccessPathTuple> baseAPs = new HashSet<>();
		private List<Set<AccessPathTuple>> paramAPs = new ArrayList<>();
		private Set<AccessPathTuple> returnAPs = new HashSet<>();

		/**
		 * Event Handler for the starting element for SAX. Possible start
		 * elements for filling AndroidMethod objects with the new data format:
		 * - method: Setting parsingvalues to false or null and get and set the
		 * signature and category, - accessPath: To get the information whether
		 * the AndroidMethod is a sink or source, - and the other elements
		 * doesn't care for creating the AndroidMethod object. At these element
		 * we will look, if calling the method getAccessPath (using an new SAX
		 * Handler)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			String qNameLower = qName.toLowerCase();
			switch (qNameLower) {
			case XMLConstants.CATEGORY_TAG:
				String strSysCategory = attributes.getValue(XMLConstants.ID_ATTRIBUTE).trim();
				String strCustomCategory = attributes.getValue(XMLConstants.CUSTOM_ID_ATTRIBUTE);
				if (strCustomCategory != null && !strCustomCategory.isEmpty())
					strCustomCategory = strCustomCategory.trim();
				String strCustomDescription = attributes.getValue(XMLConstants.DESCRIPTION_ATTRIBUTE);
				if (strCustomDescription != null && !strCustomDescription.isEmpty())
					strCustomDescription = strCustomDescription.trim();

				category = getOrMakeCategory(CATEGORY.valueOf(strSysCategory), strCustomCategory, strCustomDescription);

				// Check for excluded categories
				if (categoryFilter != null && !categoryFilter.acceptsCategory(category)) {
					category = null;
				}
				break;

			case XMLConstants.FIELD_TAG:
				if (category != null && attributes != null) {
					fieldSignature = parseSignature(attributes);
					accessPathParentElement = XMLConstants.BASE_TAG;
				}
				break;

			case XMLConstants.METHOD_TAG:
				if (category != null && attributes != null) {
					methodSignature = parseSignature(attributes);

					// Get the call type
					callType = CallType.MethodCall;
					String strCallType = attributes.getValue(XMLConstants.CALL_TYPE);
					if (strCallType != null && !strCallType.isEmpty()) {
						strCallType = strCallType.trim();
						if (strCallType.equalsIgnoreCase("MethodCall"))
							callType = CallType.MethodCall;
						else if (strCallType.equalsIgnoreCase("Callback"))
							callType = CallType.Callback;
					}
				}
				break;

			case XMLConstants.ACCESSPATH_TAG:
				if ((methodSignature != null && !methodSignature.isEmpty())
						|| (fieldSignature != null && !fieldSignature.isEmpty())) {
					pathElements = new ArrayList<>();
					pathElementTypes = new ArrayList<>();

					if (attributes != null) {
						String tempStr = attributes.getValue(XMLConstants.IS_SOURCE_ATTRIBUTE);
						if (tempStr != null && !tempStr.isEmpty())
							isSource = tempStr.equalsIgnoreCase(XMLConstants.TRUE);

						tempStr = attributes.getValue(XMLConstants.IS_SINK_ATTRIBUTE);
						if (tempStr != null && !tempStr.isEmpty())
							isSink = tempStr.equalsIgnoreCase(XMLConstants.TRUE);

						description = attributes.getValue(XMLConstants.DESCRIPTION_ATTRIBUTE);
					}
				}
				break;

			case XMLConstants.BASE_TAG:
				accessPathParentElement = qNameLower;
				break;

			case XMLConstants.RETURN_TAG:
				accessPathParentElement = qNameLower;
				break;

			case XMLConstants.PARAM_TAG:
				if (methodSignature != null && attributes != null) {
					String tempStr = attributes.getValue(XMLConstants.INDEX_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty())
						paramIndex = Integer.parseInt(tempStr);

					tempStr = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty())
						paramTypes.add(tempStr.trim());
				}
				accessPathParentElement = qNameLower;
				break;

			case XMLConstants.PATHELEMENT_TAG:
				if (methodSignature != null && attributes != null) {
					String tempStr = attributes.getValue(XMLConstants.FIELD_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty()) {
						pathElements.add(tempStr);
					}

					tempStr = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty()) {
						pathElementTypes.add(tempStr);
					}
				}
				break;
			}
		}

		/**
		 * Reads the method or field signature from the given attribute map
		 * 
		 * @param attributes
		 *            The attribute map from which to read the signature
		 * @return The signature that identifies a Soot method or field
		 */
		private String parseSignature(Attributes attributes) {
			String signature = attributes.getValue(XMLConstants.SIGNATURE_ATTRIBUTE).trim();

			// If the user did not specify the brackets, we add them on
			// the fly
			if (signature != null && !signature.isEmpty())
				if (!signature.startsWith("<"))
					signature = "<" + signature + ">";
			return signature;
		}

		/**
		 * PathElement is the only element having values inside -> nothing to do
		 * here. Doesn't care at the current state of parsing.
		 **/
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
		}

		/**
		 * EventHandler for the End of an element. -> Putting the values into
		 * the objects. For additional information: startElement description.
		 * Starting with the innerst elements and switching up to the outer
		 * elements
		 * 
		 * - pathElement -> means field sensitive, adding SootFields
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String qNameLower = qName.toLowerCase();
			switch (qNameLower) {

			case XMLConstants.CATEGORY_TAG:
				category = null;
				break;

			case XMLConstants.METHOD_TAG:
				if (methodSignature == null)
					break;

				// Check whether we have data
				if (!baseAPs.isEmpty() || !paramAPs.isEmpty() || !returnAPs.isEmpty()) {
					AndroidMethod tempMeth = AndroidMethod.createFromSignature(methodSignature);

					@SuppressWarnings("unchecked")
					SourceSinkDefinition ssd = new MethodSourceSinkDefinition(tempMeth, baseAPs,
							paramAPs.toArray(new Set[paramAPs.size()]), returnAPs, callType);
					ssd.setCategory(category);
					addSourceSinkDefinition(methodSignature, ssd);
				}

				// Start a new method and discard our old data
				methodSignature = null;
				fieldSignature = null;
				baseAPs = new HashSet<>();
				paramAPs = new ArrayList<>();
				returnAPs = new HashSet<>();
				break;

			case XMLConstants.FIELD_TAG:
				// Create the field source
				if (!baseAPs.isEmpty()) {
					SourceSinkDefinition ssd = new FieldSourceSinkDefinition(fieldSignature, baseAPs);
					addSourceSinkDefinition(fieldSignature, ssd);
				}

				// Start a new field and discard our old data
				methodSignature = null;
				fieldSignature = null;
				break;

			case XMLConstants.ACCESSPATH_TAG:
				// Record the access path for the current element
				if (isSource || isSink) {
					// Clean up the path elements
					if (pathElements != null && pathElements.isEmpty() && pathElementTypes != null
							&& pathElementTypes.isEmpty()) {
						pathElements = null;
						pathElementTypes = null;
					}

					// Sanity check
					if (pathElements != null && pathElementTypes != null
							&& pathElements.size() != pathElementTypes.size())
						throw new RuntimeException(
								String.format("Length mismatch between path elements (%d) and their types (%d)",
										pathElements.size(), pathElementTypes.size()));
					if (pathElements == null || pathElements.isEmpty())
						if (pathElementTypes != null && !pathElementTypes.isEmpty())
							throw new RuntimeException("Got types for path elements, but no elements (i.e., fields)");

					// Filter the sources and sinks
					SourceSinkType sstype = SourceSinkType.fromFlags(isSink, isSource);
					if (categoryFilter != null)
						sstype = categoryFilter.filter(category, sstype);

					if (sstype != SourceSinkType.Neither) {
						AccessPathTuple apt = AccessPathTuple.fromPathElements(pathElements, pathElementTypes, sstype);
						apt = apt.simplify();

						// Optional description
						if (description != null && !description.isEmpty())
							apt.setDescription(description);

						switch (accessPathParentElement) {
						case XMLConstants.BASE_TAG:
							baseAPs.add(apt);
							break;
						case XMLConstants.RETURN_TAG:
							returnAPs.add(apt);
							break;
						case XMLConstants.PARAM_TAG:
							while (paramAPs.size() <= paramIndex)
								paramAPs.add(new HashSet<AccessPathTuple>());
							paramAPs.get(paramIndex).add(apt);
						}
					}
				}

				pathElements = null;
				pathElementTypes = null;

				isSource = false;
				isSink = false;
				pathElements = null;
				pathElementTypes = null;
				break;

			case XMLConstants.BASE_TAG:
				accessPathParentElement = "";
				break;

			case XMLConstants.RETURN_TAG:
				accessPathParentElement = "";
				break;

			case XMLConstants.PARAM_TAG:
				accessPathParentElement = "";
				paramIndex = -1;
				paramTypes.clear();
				break;

			case XMLConstants.PATHELEMENT_TAG:
				break;
			}
		}

		/**
		 * Adds a new source or sink definition
		 * 
		 * @param signature
		 *            The signature of the method or field that is considered a
		 *            source or a sink
		 * @param ssd
		 *            The source or sink definition
		 */
		private void addSourceSinkDefinition(String signature, SourceSinkDefinition ssd) {
			if (sourcesAndSinks.containsKey(signature))
				sourcesAndSinks.get(signature).merge(ssd);
			else
				sourcesAndSinks.put(signature, ssd);
		}

	}

	private Map<String, SourceSinkDefinition> sourcesAndSinks;

	private Set<SourceSinkDefinition> sources = new HashSet<>();
	private Set<SourceSinkDefinition> sinks = new HashSet<>();
	private ICategoryFilter categoryFilter = null;

	private final Map<CategoryDefinition, CategoryDefinition> categories = new HashMap<>();

	// XML stuff incl. Verification against XSD
	private static final String XSD_FILE_PATH = "schema/SourcesAndSinks.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static XMLSourceSinkParser fromFile(String fileName) throws IOException {
		return fromFile(fileName, null);
	}

	public static XMLSourceSinkParser fromFile(String fileName, ICategoryFilter categoryFilter) throws IOException {
		logger.info(String.format("Loading sources and sinks from %s...", fileName));
		verifyXML(getStream(fileName));

		InputStream inputStream = getStream(fileName);
		try {
			return fromStream(inputStream, categoryFilter);
		} finally {
			inputStream.close();
		}
	}

	private static InputStream getStream(String fileName) throws IOException {
		File f = new File(fileName);
		if (f.exists())
			return new FileInputStream(f);

		return ResourceUtils.getResourceStream(fileName);
	}

	public static XMLSourceSinkParser fromStream(InputStream inputStream) throws IOException {
		return fromStream(inputStream, null);
	}

	public static XMLSourceSinkParser fromStream(InputStream inputStream, ICategoryFilter categoryFilter)
			throws IOException {
		XMLSourceSinkParser pmp = new XMLSourceSinkParser(inputStream, categoryFilter);
		return pmp;
	}

	@Override
	public Set<SourceSinkDefinition> getSources() {
		return sources;
	}

	@Override
	public Set<SourceSinkDefinition> getSinks() {
		return sinks;
	}

	/**
	 * gets the category for the given values. If this is the first time such a
	 * category is requested, the respective definition is created. Otherwise,
	 * the existing one is returned.
	 * 
	 * @param systemCategory
	 *            The system-defined category name
	 * @param customCategory
	 *            The user-defined category name
	 * @param customDescription
	 *            The human-readable description for the custom category
	 * @return The category definition object for the given category names
	 */
	private CategoryDefinition getOrMakeCategory(CATEGORY systemCategory, String customCategory,
			String customDescription) {
		// For the key in the map, we ignore the description. We do not want to
		// have the
		// same category id just with different descriptions.
		CategoryDefinition keyDef = new CategoryDefinition(systemCategory, customCategory);

		CategoryDefinition newDef = new CategoryDefinition(systemCategory, customCategory, customDescription);
		CategoryDefinition existingDef = categories.putIfAbsent(keyDef, newDef);
		return existingDef == null ? newDef : existingDef;
	}

	/**
	 * Creates a new instance of the {@link XMLSourceSinkParser} class and reads
	 * the source and sink definitions from the given stream
	 * 
	 * @param stream
	 *            The stream from which to read the sources and sinks
	 * @param filter
	 *            A filter for excluding certain categories of sources and sinks
	 */
	private XMLSourceSinkParser(InputStream stream, ICategoryFilter categoryFilter) {
		// Parse the data
		this.sourcesAndSinks = new HashMap<>();
		this.categoryFilter = categoryFilter;
		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			SAXParser parser = pf.newSAXParser();
			parser.parse(stream, new SAXHandler());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Build the source and sink lists
		for (SourceSinkDefinition def : sourcesAndSinks.values()) {
			SourceSinkDefinition sourceDef = def.getSourceOnlyDefinition();
			if (!sourceDef.isEmpty()) {
				if (sourceDef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition methodSrc = (MethodSourceSinkDefinition) sourceDef;
					if (methodSrc.getMethod() instanceof AndroidMethod) {
						AndroidMethod am = (AndroidMethod) methodSrc.getMethod();
						am.setSourceSinkType(am.getSourceSinkType().addType(SourceSinkType.Source));
					}
				}
				sources.add(sourceDef);
			}

			SourceSinkDefinition sinkDef = def.getSinkOnlyDefinition();
			if (!sinkDef.isEmpty()) {
				if (sourceDef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition methodSink = (MethodSourceSinkDefinition) sourceDef;
					if (methodSink.getMethod() instanceof AndroidMethod) {
						AndroidMethod am = (AndroidMethod) methodSink.getMethod();
						am.setSourceSinkType(am.getSourceSinkType().addType(SourceSinkType.Sink));
					}
				}
				sinks.add(sinkDef);
			}
		}
		logger.info(String.format("Loaded %d sources and %d sinks from the XML file", sources.size(), sinks.size()));
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data
	 * format.
	 * 
	 * @param fileName
	 *            of the XML
	 * @throws IOException
	 */
	private static void verifyXML(InputStream inp) throws IOException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);

		// Read the schema
		StreamSource xsdFile = new StreamSource(ResourceUtils.getResourceStream(XSD_FILE_PATH));

		StreamSource xmlFile = new StreamSource(inp);
		try {
			Schema schema = sf.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			try {
				validator.validate(xmlFile);
			} catch (IOException e) {
				throw new IOException("File isn't  valid against the xsd", e);
			}
		} catch (SAXException e) {
			throw new IOException("File isn't  valid against the xsd", e);
		} finally {
			xsdFile.getInputStream().close();
			xmlFile.getInputStream().close();
		}
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sources.size() + sinks.size());
		sourcesSinks.addAll(sources);
		sourcesSinks.addAll(sinks);
		return sourcesSinks;
	}

}
