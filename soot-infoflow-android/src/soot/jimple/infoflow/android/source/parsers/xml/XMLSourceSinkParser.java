package soot.jimple.infoflow.android.source.parsers.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.data.AbstractMethodAndClass;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.IAccessPathBasedSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;
import soot.util.HashMultiMap;

/**
 * Parses informations from the new Dataformat (XML) with the help of SAX.
 * Returns only a Set of Android Method when calling the function parse. For the
 * AccessPath the class SaxHandler is used.
 * 
 * @author Anna-Katharina Wickert
 * @author Joern Tillmans
 * @author Steven Arzt
 * @author Niklas Vogel
 */

public class XMLSourceSinkParser extends AbstractXMLSourceSinkParser implements ISourceSinkDefinitionProvider {

	private final static Logger logger = LoggerFactory.getLogger(XMLSourceSinkParser.class);

	// XML stuff incl. Verification against XSD
	protected static final String XSD_FILE_PATH = "schema/SourcesAndSinks.xsd";
	protected static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static XMLSourceSinkParser fromFile(String fileName) throws IOException {
		return fromFile(fileName, null);
	}

	public static XMLSourceSinkParser fromFile(File file) throws IOException {
		return fromFile(file, null);
	}

	public static XMLSourceSinkParser fromFile(String fileName, ICategoryFilter categoryFilter) throws IOException {
		logger.info(String.format("Loading sources and sinks from %s...", fileName));
		try (InputStream is = getStream(fileName)) {
			verifyXML(is);
		}

		try (InputStream inputStream = getStream(fileName)) {
			return fromStream(inputStream, categoryFilter);
		}
	}

	public static XMLSourceSinkParser fromFile(File file, ICategoryFilter categoryFilter) throws IOException {
		logger.info(String.format("Loading sources and sinks from %s...", file.getAbsolutePath()));
		try (InputStream is = new FileInputStream(file)) {
			verifyXML(is);
		}

		try (InputStream inputStream = new FileInputStream(file)) {
			return fromStream(inputStream, categoryFilter);
		}
	}

	protected static InputStream getStream(String fileName) throws IOException {
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
		XMLSourceSinkParser pmp = new XMLSourceSinkParser(categoryFilter);
		pmp.parseInputStream(inputStream);
		return pmp;
	}

	/**
	 * Builds the lists of sources and sinks from the data that we have parsed
	 */

	/**
	 * Creates a new instance of the {@link XMLSourceSinkParser} class
	 * 
	 * @param filter A filter for excluding certain categories of sources and sinks
	 */
	protected XMLSourceSinkParser(ICategoryFilter categoryFilter) {
		this.sourcesAndSinks = new HashMultiMap<>();
		this.categoryFilter = categoryFilter;
	}

	/**
	 * Builds the lists of sources and sinks from the data that we have parsed
	 */
	@Override
	protected void buildSourceSinkLists() {
		for (ISourceSinkDefinition def : sourcesAndSinks.values()) {
			ISourceSinkDefinition sourceDef = def.getSourceOnlyDefinition();
			if (sourceDef != null && !sourceDef.isEmpty()) {
				if (sourceDef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition methodSrc = (MethodSourceSinkDefinition) sourceDef;
					if (methodSrc.getMethod() instanceof AndroidMethod) {
						AndroidMethod am = (AndroidMethod) methodSrc.getMethod();
						am.setSourceSinkType(am.getSourceSinkType().addType(SourceSinkType.Source));
					}
				}
				sources.add(sourceDef);
			}

			ISourceSinkDefinition sinkDef = def.getSinkOnlyDefinition();
			if (sinkDef != null && !sinkDef.isEmpty()) {
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
	 * @param fileName of the XML
	 * @throws IOException
	 */
	private static void verifyXML(InputStream inp) throws IOException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);

		// Read the schema
		StreamSource xsdFile = new StreamSource(
				ResourceUtils.getResourceStream(InfoflowConfiguration.getBaseDirectory() + XSD_FILE_PATH));

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

	/**
	 * Factory method for {@link MethodSourceSinkDefinition} instances
	 * 
	 * @param method    The method that is to be defined as a source or sink
	 * @param baseAPs   The access paths rooted in the base object that shall be
	 *                  considered as sources or sinks
	 * @param paramAPs  The access paths rooted in parameters that shall be
	 *                  considered as sources or sinks. The index in the set
	 *                  corresponds to the index of the formal parameter to which
	 *                  the respective set of access paths belongs.
	 * @param returnAPs The access paths rooted in the return object that shall be
	 *                  considered as sources or sinks
	 * @param callType  The type of call (normal call, callback, etc.)
	 * @return The newly created {@link MethodSourceSinkDefinition} instance
	 */
	@Override
	protected ISourceSinkDefinition createMethodSourceSinkDefinition(AbstractMethodAndClass method,
			Set<AccessPathTuple> baseAPs, Set<AccessPathTuple>[] paramAPs, Set<AccessPathTuple> returnAPs,
			CallType callType, ISourceSinkCategory category) {
		if (method instanceof AndroidMethod) {
			AndroidMethod amethod = (AndroidMethod) method;
			return new MethodSourceSinkDefinition(amethod, baseAPs, paramAPs, returnAPs, callType, category);
		}
		return null;
	}

	protected ISourceSinkDefinition createMethodSourceSinkDefinition(AbstractMethodAndClass method,
			Set<AccessPathTuple> baseAPs, Set<AccessPathTuple>[] paramAPs, Set<AccessPathTuple> returnAPs,
			CallType callType, ISourceSinkCategory category, Set<SourceSinkCondition> conditions) {
		ISourceSinkDefinition ssdef = createMethodSourceSinkDefinition(method, baseAPs, paramAPs, returnAPs, callType,
				category);
		if (ssdef != null)
			ssdef.setConditions(conditions);
		return ssdef;
	}

	/**
	 * Factory method for {@link FieldSourceSinkDefinition} instances
	 * 
	 * @param signature The signature of the target field
	 * @param baseAPs   The access paths that shall be considered as sources or
	 *                  sinks
	 * @return The newly created {@link FieldSourceSinkDefinition} instance
	 */
	@Override
	protected IAccessPathBasedSourceSinkDefinition createFieldSourceSinkDefinition(String signature,
			Set<AccessPathTuple> baseAPs) {
		return new FieldSourceSinkDefinition(signature, baseAPs);
	}

	@Override
	protected IAccessPathBasedSourceSinkDefinition createFieldSourceSinkDefinition(String signature,
			Set<AccessPathTuple> baseAPs, Set<SourceSinkCondition> conditions) {
		IAccessPathBasedSourceSinkDefinition ssdef = createFieldSourceSinkDefinition(signature, baseAPs);
		ssdef.setConditions(conditions);
		return ssdef;
	}

	/**
	 * Run parse method on given parser
	 * 
	 * @param parser The parser which should be used to parse
	 * @param stream The XML text input stream which should be parsed
	 */
	@Override
	protected void runParse(SAXParser parser, InputStream stream) {
		try {
			parser.parse(stream, new SAXHandler(this.categoryFilter));
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
	}

}
