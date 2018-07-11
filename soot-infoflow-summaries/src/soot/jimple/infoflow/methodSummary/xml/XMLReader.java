package soot.jimple.infoflow.methodSummary.xml;

import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_BASETYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_FLOWTYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_MATCH_STRICT;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_PARAMTER_INDEX;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_TAINT_SUB_FIELDS;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_CLEAR;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_FLOW;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_METHOD;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SINK;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SOURCE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.VALUE_TRUE;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowClear;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodClear;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.util.ResourceUtils;

public class XMLReader {

	// XML stuff incl. Verification against XSD
	private static final String XSD_FILE_PATH = "schema/ClassSummary.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	private boolean validateSummariesOnRead = false;

	private enum State {
		summary, methods, method, flow, clear, gaps, gap
	}

	/**
	 * Reads a summary xml and returns the MethodSummaries. This method closes the
	 * reader.
	 * 
	 * @param reader
	 *            The reader from which to read the method summaries
	 * @return The summary data object read from the given reader
	 * @return XMLStreamException Thrown in case of a syntax error in the input file
	 * @throws IOException
	 *             Thrown if the reader could not be read
	 */
	public MethodSummaries read(Reader reader) throws XMLStreamException, SummaryXMLException, IOException {
		MethodSummaries summary = new MethodSummaries();

		XMLStreamReader xmlreader = null;
		try {
			xmlreader = XMLInputFactory.newInstance().createXMLStreamReader(reader);

			Map<String, String> sourceAttributes = new HashMap<String, String>();
			Map<String, String> sinkAttributes = new HashMap<String, String>();
			Map<String, String> clearAttributes = new HashMap<String, String>();

			String currentMethod = "";
			int currentID = -1;
			boolean isAlias = false;
			boolean typeChecking = true;
			boolean cutSubfields = false;

			State state = State.summary;
			while (xmlreader.hasNext()) {
				// Read the next tag
				xmlreader.next();
				if (!xmlreader.hasName())
					continue;

				if (xmlreader.getLocalName().equals(XMLConstants.TREE_METHODS) && xmlreader.isStartElement()) {
					if (state == State.summary)
						state = State.methods;
					else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_METHOD) && xmlreader.isStartElement()) {
					if (state == State.methods) {
						currentMethod = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_METHOD_SIG);
						state = State.method;
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_METHOD) && xmlreader.isEndElement()) {
					if (state == State.method)
						state = State.methods;
					else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_FLOW) && xmlreader.isStartElement()) {
					if (state == State.method) {
						sourceAttributes.clear();
						sinkAttributes.clear();
						state = State.flow;
						String sAlias = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IS_ALIAS);
						isAlias = sAlias != null && sAlias.equals(XMLConstants.VALUE_TRUE);
						String sTypeChecking = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_TYPE_CHECKING);
						if (sTypeChecking != null)
							typeChecking = sTypeChecking.equals(XMLConstants.VALUE_TRUE);
						String sCutSubfields = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_CUT_SUBFIELDS);
						if (sCutSubfields != null)
							cutSubfields = sTypeChecking.equals(XMLConstants.VALUE_TRUE);
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_CLEAR) && xmlreader.isStartElement()) {
					if (state == State.method) {
						clearAttributes.clear();
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							clearAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
						state = State.clear;
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_SOURCE) && xmlreader.isStartElement()) {
					if (state == State.flow) {
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							sourceAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_SINK) && xmlreader.isStartElement()) {
					if (state == State.flow) {
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							sinkAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_FLOW) && xmlreader.isEndElement()) {
					if (state == State.flow) {
						state = State.method;
						MethodFlow flow = new MethodFlow(currentMethod, createSource(summary, sourceAttributes),
								createSink(summary, sinkAttributes), isAlias, typeChecking, cutSubfields);
						summary.addFlow(flow);

						isAlias = false;
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(TREE_CLEAR) && xmlreader.isEndElement()) {
					if (state == State.clear) {
						state = State.method;
						MethodClear clear = new MethodClear(currentMethod, createClear(summary, clearAttributes));
						summary.addClear(clear);
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(XMLConstants.TREE_METHODS) && xmlreader.isEndElement()) {
					if (state == State.methods)
						state = State.summary;
					else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(XMLConstants.TREE_GAPS) && xmlreader.isStartElement()) {
					if (state == State.summary)
						state = State.gaps;
					else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(XMLConstants.TREE_GAPS) && xmlreader.isEndElement()) {
					if (state == State.gaps)
						state = State.summary;
					else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(XMLConstants.TREE_GAP) && xmlreader.isStartElement()) {
					if (state == State.gaps) {
						currentMethod = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_METHOD_SIG);
						currentID = Integer.valueOf(getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_ID));
						summary.getOrCreateGap(currentID, currentMethod);
						state = State.gap;
					} else
						throw new SummaryXMLException();
				} else if (xmlreader.getLocalName().equals(XMLConstants.TREE_GAP) && xmlreader.isEndElement()) {
					if (state == State.gap) {
						state = State.gaps;
					} else
						throw new SummaryXMLException();
				}
			}

			// Validate the summary to make sure that we didn't read in any
			// bogus
			// stuff
			if (validateSummariesOnRead)
				summary.validate();

			return summary;
		} finally {
			if (xmlreader != null)
				xmlreader.close();
		}
	}

	/**
	 * Reads a summary xml file and returns the MethodSummaries which are saved in
	 * that file
	 * 
	 * @param fileName
	 *            The file from which to read the method summaries
	 * @return The summary data object read from the given file
	 * @return XMLStreamException Thrown in case of a syntax error in the input file
	 * @throws IOException
	 *             Thrown if the file could not be read
	 */

	public MethodSummaries read(File fileName) throws XMLStreamException, SummaryXMLException, IOException {
		FileReader rdr = null;
		try {
			rdr = new FileReader(fileName);
			if (!verifyXML(rdr)) {
				throw new RuntimeException("The XML-File isn't valid");
			}
		} finally {
			if (rdr != null)
				rdr.close();
		}

		return read(new FileReader(fileName));
	}

	/**
	 * Gets the value of the XML attribute with the specified id
	 * 
	 * @param reader
	 *            The reader from which to get the XML data
	 * @param id
	 *            The attribute id for which to get the data
	 * @return The data of the given attribute if it exists, otherwise an empty
	 *         string
	 */
	private String getAttributeByName(XMLStreamReader reader, String id) {
		for (int i = 0; i < reader.getAttributeCount(); i++)
			if (reader.getAttributeLocalName(i).equals(id))
				return reader.getAttributeValue(i);
		return "";
	}

	/**
	 * Creates a new source data object from the given XML attributes
	 * 
	 * @param summary
	 *            The method summary for which to create the new flow source
	 * @param attributes
	 *            The XML attributes for the source
	 * @return The newly created source data object
	 * @throws SummaryXMLException
	 */
	private FlowSource createSource(MethodSummaries summary, Map<String, String> attributes)
			throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowSource(SourceSinkType.Field, getBaseType(attributes), getAccessPath(attributes),
					getAccessPathTypes(attributes), getGapDefinition(attributes, summary), isMatchStrict(attributes));
		} else if (isParameter(attributes)) {
			return new FlowSource(SourceSinkType.Parameter, paramterIdx(attributes), getBaseType(attributes),
					getAccessPath(attributes), getAccessPathTypes(attributes), getGapDefinition(attributes, summary),
					isMatchStrict(attributes));
		} else if (isGapBaseObject(attributes)) {
			return new FlowSource(SourceSinkType.GapBaseObject, getBaseType(attributes),
					getGapDefinition(attributes, summary), isMatchStrict(attributes));
		} else if (isReturn(attributes)) {
			GapDefinition gap = getGapDefinition(attributes, summary);
			if (gap == null)
				throw new SummaryXMLException(
						"Return values can only be " + "sources if they have a gap specification");

			return new FlowSource(SourceSinkType.Return, getBaseType(attributes), getAccessPath(attributes),
					getAccessPathTypes(attributes), getGapDefinition(attributes, summary), isMatchStrict(attributes));
		}
		throw new SummaryXMLException("Invalid flow source definition");
	}

	/**
	 * Creates a new sink data object from the given XML attributes
	 * 
	 * @param summary
	 *            The method summary for which to create the new flow source
	 * @param attributes
	 *            The XML attributes for the sink
	 * @return The newly created sink data object
	 * @throws SummaryXMLException
	 */
	private FlowSink createSink(MethodSummaries summary, Map<String, String> attributes) throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowSink(SourceSinkType.Field, getBaseType(attributes), getAccessPath(attributes),
					getAccessPathTypes(attributes), taintSubFields(attributes), getGapDefinition(attributes, summary),
					isMatchStrict(attributes));
		} else if (isParameter(attributes)) {
			return new FlowSink(SourceSinkType.Parameter, paramterIdx(attributes), getBaseType(attributes),
					getAccessPath(attributes), getAccessPathTypes(attributes), taintSubFields(attributes),
					getGapDefinition(attributes, summary), isMatchStrict(attributes));
		} else if (isReturn(attributes)) {
			return new FlowSink(SourceSinkType.Return, getBaseType(attributes), getAccessPath(attributes),
					getAccessPathTypes(attributes), taintSubFields(attributes), getGapDefinition(attributes, summary),
					isMatchStrict(attributes));
		} else if (isGapBaseObject(attributes)) {
			return new FlowSink(SourceSinkType.GapBaseObject, -1, getBaseType(attributes), false,
					getGapDefinition(attributes, summary), isMatchStrict(attributes));
		}
		throw new SummaryXMLException();
	}

	/**
	 * Creates a new taint kill data object from the given XML attributes
	 * 
	 * @param summary
	 *            The method summary for which to create the new flow source
	 * @param attributes
	 *            The XML attributes for the source
	 * @return The newly created source data object
	 * @throws SummaryXMLException
	 */
	private FlowClear createClear(MethodSummaries summary, Map<String, String> attributes) throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowClear(SourceSinkType.Field, getBaseType(attributes), getAccessPath(attributes),
					getAccessPathTypes(attributes), getGapDefinition(attributes, summary));
		} else if (isParameter(attributes)) {
			return new FlowClear(SourceSinkType.Parameter, paramterIdx(attributes), getBaseType(attributes),
					getAccessPath(attributes), getAccessPathTypes(attributes), getGapDefinition(attributes, summary));
		} else if (isGapBaseObject(attributes)) {
			return new FlowClear(SourceSinkType.GapBaseObject, getBaseType(attributes),
					getGapDefinition(attributes, summary));
		}
		throw new SummaryXMLException("Invalid flow clear definition");
	}

	private boolean isReturn(Map<String, String> attributes) {
		return attributes != null && attributes.get(ATTRIBUTE_FLOWTYPE).equals(SourceSinkType.Return.toString());
	}

	private boolean isField(Map<String, String> attributes) {
		return attributes != null && attributes.get(ATTRIBUTE_FLOWTYPE).equals(SourceSinkType.Field.toString());
	}

	private String[] getAccessPath(Map<String, String> attributes) {
		String ap = attributes.get(XMLConstants.ATTRIBUTE_ACCESSPATH);
		if (ap != null) {
			if (ap.length() > 3) {
				String[] res = ap.substring(1, ap.length() - 1).split(",");
				for (int i = 0; i < res.length; i++) {
					String curElement = res[i].trim();

					// We don't require the XML file to contain Soot's signature
					// brackets
					if (!curElement.startsWith("<"))
						curElement = "<" + curElement;
					if (!curElement.endsWith(">"))
						curElement = curElement + ">";

					res[i] = curElement;
				}
				return res;
			}
		}
		return null;
	}

	private String[] getAccessPathTypes(Map<String, String> attributes) {
		String ap = attributes.get(XMLConstants.ATTRIBUTE_ACCESSPATHTYPES);
		if (ap != null) {
			if (ap.length() > 3) {
				String[] res = ap.substring(1, ap.length() - 1).split(",");
				for (int i = 0; i < res.length; i++)
					res[i] = res[i].trim();
				return res;
			}
		}
		return null;
	}

	private boolean isMatchStrict(Map<String, String> attributes) {
		String str = attributes.get(ATTRIBUTE_MATCH_STRICT);
		if (str != null && !str.isEmpty())
			return Boolean.valueOf(str);
		return false;
	}

	private boolean isParameter(Map<String, String> attributes) {
		return attributes.get(ATTRIBUTE_FLOWTYPE).equals(SourceSinkType.Parameter.toString());
	}

	private boolean isGapBaseObject(Map<String, String> attributes) {
		return attributes != null && attributes.get(ATTRIBUTE_FLOWTYPE).equals(SourceSinkType.GapBaseObject.toString());
	}

	private int paramterIdx(Map<String, String> attributes) {
		String strIdx = attributes.get(ATTRIBUTE_PARAMTER_INDEX);
		if (strIdx == null || strIdx.isEmpty())
			throw new RuntimeException("Parameter index not specified");
		return Integer.parseInt(strIdx);
	}

	private String getBaseType(Map<String, String> attributes) {
		return attributes.get(ATTRIBUTE_BASETYPE);
	}

	private boolean taintSubFields(Map<String, String> attributes) {
		String val = attributes.get(ATTRIBUTE_TAINT_SUB_FIELDS);
		return val != null && val.equals(VALUE_TRUE);
	}

	private GapDefinition getGapDefinition(Map<String, String> attributes, MethodSummaries summary) {
		String id = attributes.get(XMLConstants.ATTRIBUTE_GAP);
		if (id == null || id.isEmpty())
			return null;

		// Do we already have a suitable gap definition?
		GapDefinition gap = summary.getGap(Integer.parseInt(id));
		if (gap != null)
			return gap;

		// We have not read in this gap definition yet and need to create a stub
		// for the time being.
		return summary.createTemporaryGap(Integer.parseInt(id));
	}

	/**
	 * Sets whether summaries shall be validated after they are read from disk
	 * 
	 * @param validateSummariesOnRead
	 *            True if summaries shall be validated after they are read from
	 *            disk, otherwise false
	 */
	public void setValidateSummariesOnRead(boolean validateSummariesOnRead) {
		this.validateSummariesOnRead = validateSummariesOnRead;
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data
	 * format.
	 * 
	 * @param is
	 *            The stream from which to read the XML data
	 * @return true = valid XML false = invalid XML
	 * @throws IOException
	 */
	private static boolean verifyXML(FileReader reader) throws IOException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		StreamSource xsdFile = new StreamSource(ResourceUtils.getResourceStream(XSD_FILE_PATH));
		StreamSource xmlFile = new StreamSource(reader);
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

}
