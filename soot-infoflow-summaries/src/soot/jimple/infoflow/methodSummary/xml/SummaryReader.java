package soot.jimple.infoflow.methodSummary.xml;

import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import soot.jimple.infoflow.methodSummary.data.sourceSink.ConstraintType;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowClear;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.*;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

public class SummaryReader extends AbstractXMLReader {

	// XML stuff incl. Verification against XSD
	private static final String XSD_FILE_PATH = "schema/ClassSummary.xsd";

	private boolean validateSummariesOnRead = false;

	private enum State {
		summary, hierarchy, intf, methods, method, flows, flow, clear, gaps, gap
	}

	/**
	 * Reads a summary xml and places the new summaries into the given data object.
	 * This method closes the reader.
	 * 
	 * @param reader    The reader from which to read the method summaries
	 * @param summaries The data object in which to place the summaries
	 * @throws XMLStreamException Thrown in case of a syntax error in the input file
	 * @throws IOException Thrown if the reader could not be read
	 */
	public void read(Reader reader, ClassMethodSummaries summaries)
			throws XMLStreamException, SummaryXMLException, IOException {
		XMLStreamReader xmlreader = null;
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			xmlreader = factory.createXMLStreamReader(reader);
			final MethodSummaries summary = summaries.getMethodSummaries();

			Map<String, String> sourceAttributes = new HashMap<String, String>();
			Map<String, String> sinkAttributes = new HashMap<String, String>();
			Map<String, String> clearAttributes = new HashMap<String, String>();

			String currentMethod = "";
			int currentID = -1;
			IsAliasType isAlias = IsAliasType.FALSE;
			Boolean typeChecking = null;
			Boolean ignoreTypes = null;
			Boolean cutSubfields = null;

			State state = State.summary;
			while (xmlreader.hasNext()) {
				// Read the next tag
				xmlreader.next();
				if (!xmlreader.hasName())
					continue;

				final String localName = xmlreader.getLocalName();
				if (localName.equals(XMLConstants.TREE_SUMMARY) && xmlreader.isStartElement()) {
					String isInterface = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IS_INTERFACE);

					// If the string is empty, it's unknown, and neither true nor false, so we must
					// not call setInterface()!
					if (isInterface != null && !isInterface.isEmpty())
						summaries.setInterface(isInterface.equals(XMLConstants.VALUE_TRUE));

					String isExclusive = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IS_EXCLUSIVE);
					if (isExclusive != null && !isExclusive.isEmpty())
						summaries.setExclusiveForClass(isExclusive.equals(XMLConstants.VALUE_TRUE));
				} else if (localName.equals(XMLConstants.TREE_METHODS) && xmlreader.isStartElement()) {
					if (state == State.summary)
						state = State.methods;
					else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_METHOD) && xmlreader.isStartElement()) {
					if (state == State.methods) {
						currentMethod = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_METHOD_SIG);

						// Some summaries are full signatures instead of subsignatures. We fix this on
						// the fly.
						if (currentMethod.contains(":"))
							currentMethod = currentMethod.substring(currentMethod.indexOf(": ") + 2);

						String sIsExcluded = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IS_EXCLUDED);
						if (sIsExcluded != null && sIsExcluded.equals(XMLConstants.VALUE_TRUE))
							summary.addExcludedMethod(currentMethod);
						state = State.method;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_METHOD) && xmlreader.isEndElement()) {
					if (state == State.method)
						state = State.methods;
					else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_FLOW) && xmlreader.isStartElement()) {
					if (state == State.method) {
						sourceAttributes.clear();
						sinkAttributes.clear();
						state = State.flow;
						String sAlias = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IS_ALIAS);
						isAlias = (sAlias != null && sAlias.equals(XMLConstants.VALUE_TRUE)) ? IsAliasType.TRUE : IsAliasType.FALSE;

						String sTypeChecking = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_TYPE_CHECKING);
						if (sTypeChecking != null && !sTypeChecking.isEmpty())
							typeChecking = sTypeChecking.equals(XMLConstants.VALUE_TRUE);

						String sCutSubfields = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_CUT_SUBFIELDS);
						if (sCutSubfields != null && !sCutSubfields.isEmpty())
							cutSubfields = sCutSubfields.equals(XMLConstants.VALUE_TRUE);

						String sIgnoreTypes = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_IGNORE_TYPES);
						if (sIgnoreTypes != null && !sIgnoreTypes.isEmpty())
							ignoreTypes = sIgnoreTypes.equals(XMLConstants.VALUE_TRUE);

					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_CLEAR) && xmlreader.isStartElement()) {
					if (state == State.method) {
						clearAttributes.clear();
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							clearAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
						state = State.clear;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_SOURCE) && xmlreader.isStartElement()) {
					if (state == State.flow) {
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							sourceAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_SINK) && xmlreader.isStartElement()) {
					if (state == State.flow) {
						for (int i = 0; i < xmlreader.getAttributeCount(); i++)
							sinkAttributes.put(xmlreader.getAttributeLocalName(i), xmlreader.getAttributeValue(i));
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_FLOW) && xmlreader.isEndElement()) {
					if (state == State.flow) {
						state = State.method;
						MethodFlow flow = new MethodFlow(currentMethod, createSource(summary, sourceAttributes),
								createSink(summary, sinkAttributes), isAlias, typeChecking, ignoreTypes, cutSubfields, null, false, false);
						summary.addFlow(flow);

						isAlias = IsAliasType.FALSE;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(TREE_CLEAR) && xmlreader.isEndElement()) {
					if (state == State.clear) {
						state = State.method;
						String ppString = clearAttributes.get(ATTRIBUTE_PREVENT_PROPAGATION);
						boolean preventProp = ppString == null || ppString.isEmpty() || ppString.equals(VALUE_TRUE);
						MethodClear clear = new MethodClear(currentMethod, createClear(summary, clearAttributes), null, preventProp);
						summary.addClear(clear);
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_METHODS) && xmlreader.isEndElement()) {
					if (state == State.methods)
						state = State.summary;
					else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_GAPS) && xmlreader.isStartElement()) {
					if (state == State.summary)
						state = State.gaps;
					else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_GAPS) && xmlreader.isEndElement()) {
					if (state == State.gaps)
						state = State.summary;
					else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_GAP) && xmlreader.isStartElement()) {
					if (state == State.gaps) {
						currentMethod = getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_METHOD_SIG);
						currentID = Integer.valueOf(getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_ID));
						summary.getOrCreateGap(currentID, currentMethod);
						state = State.gap;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_GAP) && xmlreader.isEndElement()) {
					if (state == State.gap) {
						state = State.gaps;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_HIERARCHY) && xmlreader.isStartElement()) {
					if (state == State.summary) {
						summaries.setSuperClass(getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_SUPERCLASS));
						state = State.hierarchy;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_HIERARCHY) && xmlreader.isEndElement()) {
					if (state == State.hierarchy) {
						state = State.summary;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_INTERFACE) && xmlreader.isStartElement()) {
					if (state == State.hierarchy) {
						summaries.addInterface(getAttributeByName(xmlreader, XMLConstants.ATTRIBUTE_NAME));
						state = State.intf;
					} else
						throw new SummaryXMLException();
				} else if (localName.equals(XMLConstants.TREE_INTERFACE) && xmlreader.isEndElement()) {
					if (state == State.intf) {
						state = State.hierarchy;
					} else
						throw new SummaryXMLException();
				}
			}

			// Validate the summary to make sure that we didn't read in any
			// bogus stuff
			if (validateSummariesOnRead)
				summary.validate();
		} finally {
			if (xmlreader != null)
				xmlreader.close();
		}
	}

	/**
	 * Reads a summary xml file and merges the summaries that are saved in that file
	 * into the given data object
	 * 
	 * @param fileName  The file from which to read the method summaries
	 * @param summaries The data object in which to place the summaries
	 * @throws XMLStreamException Thrown in case of a syntax error in the input file
	 * @throws IOException Thrown if the file could not be read
	 */

	public void read(File fileName, ClassMethodSummaries summaries)
			throws XMLStreamException, SummaryXMLException, IOException {
		if (validateSummariesOnRead) {
			try (FileReader rdr = new FileReader(fileName)) {
				if (!verifyXML(rdr, XSD_FILE_PATH)) {
					throw new RuntimeException("The XML-File isn't valid");
				}
			}
		}

		read(new FileReader(fileName), summaries);
	}

	/**
	 * Creates a new source data object from the given XML attributes
	 * 
	 * @param summary    The method summary for which to create the new flow source
	 * @param attributes The XML attributes for the source
	 * @return The newly created source data object
	 * @throws SummaryXMLException
	 */
	private FlowSource createSource(MethodSummaries summary, Map<String, String> attributes)
			throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowSource(SourceSinkType.Field, getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isParameter(attributes)) {
			return new FlowSource(SourceSinkType.Parameter, parameterIdx(attributes), getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isGapBaseObject(attributes)) {
			return new FlowSource(SourceSinkType.GapBaseObject, getBaseType(attributes),
					getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isReturn(attributes)) {
			GapDefinition gap = getGapDefinition(attributes, summary);
			if (gap == null)
				throw new SummaryXMLException(
						"Return values can only be " + "sources if they have a gap specification");

			return new FlowSource(SourceSinkType.Return, getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		}
		throw new SummaryXMLException("Invalid flow source definition");
	}

	/**
	 * Creates a new sink data object from the given XML attributes
	 * 
	 * @param summary    The method summary for which to create the new flow source
	 * @param attributes The XML attributes for the sink
	 * @return The newly created sink data object
	 * @throws SummaryXMLException
	 */
	private FlowSink createSink(MethodSummaries summary, Map<String, String> attributes) throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowSink(SourceSinkType.Field, getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					taintSubFields(attributes), getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isParameter(attributes)) {
			return new FlowSink(SourceSinkType.Parameter, parameterIdx(attributes), getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					taintSubFields(attributes), getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isReturn(attributes)) {
			return new FlowSink(SourceSinkType.Return, getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					taintSubFields(attributes), getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		} else if (isGapBaseObject(attributes)) {
			return new FlowSink(SourceSinkType.GapBaseObject, -1, getBaseType(attributes), false,
					getGapDefinition(attributes, summary), isMatchStrict(attributes), ConstraintType.FALSE);
		}
		throw new SummaryXMLException();
	}

	/**
	 * Creates a new taint kill data object from the given XML attributes
	 * 
	 * @param summary    The method summary for which to create the new flow source
	 * @param attributes The XML attributes for the source
	 * @return The newly created source data object
	 * @throws SummaryXMLException
	 */
	private FlowClear createClear(MethodSummaries summary, Map<String, String> attributes) throws SummaryXMLException {
		if (isField(attributes)) {
			return new FlowClear(SourceSinkType.Field, getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					getGapDefinition(attributes, summary), ConstraintType.FALSE);
		} else if (isParameter(attributes)) {
			return new FlowClear(SourceSinkType.Parameter, parameterIdx(attributes), getBaseType(attributes),
					new AccessPathFragment(getAccessPath(attributes), getAccessPathTypes(attributes)),
					getGapDefinition(attributes, summary), ConstraintType.FALSE);
		} else if (isGapBaseObject(attributes)) {
			return new FlowClear(SourceSinkType.GapBaseObject, getBaseType(attributes),
					getGapDefinition(attributes, summary), ConstraintType.FALSE);
		}
		throw new SummaryXMLException("Invalid flow clear definition");
	}

	private boolean isReturn(Map<String, String> attributes) {
		if (attributes != null) {
			String attr = attributes.get(ATTRIBUTE_FLOWTYPE);
			return attr != null && attr.equals(SourceSinkType.Return.toString());
		}
		return false;
	}

	private boolean isField(Map<String, String> attributes) {
		if (attributes != null) {
			String attr = attributes.get(ATTRIBUTE_FLOWTYPE);
			return attr != null && attr.equals(SourceSinkType.Field.toString());
		}
		return false;
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

	private int parameterIdx(Map<String, String> attributes) {
		String strIdx = attributes.get(ATTRIBUTE_PARAMETER_INDEX);
		if (strIdx == null || strIdx.isEmpty())
			throw new RuntimeException("Parameter index not specified");
		if (strIdx.equals("*"))
			return FlowSource.ANY_PARAMETER;
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
	 * @param validateSummariesOnRead True if summaries shall be validated after
	 *                                they are read from disk, otherwise false
	 */
	public void setValidateSummariesOnRead(boolean validateSummariesOnRead) {
		this.validateSummariesOnRead = validateSummariesOnRead;
	}

}
