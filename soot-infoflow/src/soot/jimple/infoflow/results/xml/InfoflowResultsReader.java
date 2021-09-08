package soot.jimple.infoflow.results.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import soot.jimple.infoflow.results.InfoflowPerformanceData;

/**
 * Class for reading data flow results back into data objects
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowResultsReader {

	private enum State {
		init, dataFlowResults, results, result, fields, field, sources, source, sink, taintPath, pathElement,
		accessPath, performanceData, performanceEntry
	}

	/**
	 * Reads XML file containing data flow results into a data object
	 * 
	 * @param fileName The file from which to read the data flows
	 * @return The data flow result object read from the given file
	 * @return XMLStreamException Thrown in case of a syntax error in the input file
	 * @throws IOException Thrown if the file could not be read
	 */
	public SerializedInfoflowResults readResults(String fileName) throws XMLStreamException, IOException {
		SerializedInfoflowResults results = new SerializedInfoflowResults();
		InfoflowPerformanceData perfData = null;

		XMLStreamReader reader = null;
		try (InputStream in = new FileInputStream(fileName)) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			reader = factory.createXMLStreamReader(in);

			String statement = null;
			String method = null;
			String apValue = null;
			String apValueType = null;
			boolean apTaintSubFields = false;
			List<String> apFields = new ArrayList<>();
			List<String> apTypes = new ArrayList<>();
			SerializedAccessPath ap = null;
			SerializedSinkInfo sink = null;
			SerializedSourceInfo source = null;
			List<SerializedPathElement> pathElements = new ArrayList<>();

			Stack<State> stateStack = new Stack<>();
			stateStack.push(State.init);

			while (reader.hasNext()) {
				// Read the next tag
				reader.next();
				if (!reader.hasName())
					continue;

				if (reader.getLocalName().equals(XmlConstants.Tags.root) && reader.isStartElement()
						&& stateStack.peek() == State.init) {
					stateStack.push(State.dataFlowResults);

					// Load the attributes of the root node
					results.setFileFormatVersion(
							int2Str(getAttributeByName(reader, XmlConstants.Attributes.fileFormatVersion)));
				} else if (reader.getLocalName().equals(XmlConstants.Tags.results) && reader.isStartElement()
						&& stateStack.peek() == State.dataFlowResults) {
					stateStack.push(State.results);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.result) && reader.isStartElement()
						&& stateStack.peek() == State.results) {
					stateStack.push(State.result);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.sink) && reader.isStartElement()
						&& stateStack.peek() == State.result) {
					stateStack.push(State.sink);

					// Read the attributes
					statement = getAttributeByName(reader, XmlConstants.Attributes.statement);
					method = getAttributeByName(reader, XmlConstants.Attributes.method);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.accessPath) && reader.isStartElement()) {
					stateStack.push(State.accessPath);

					// Read the attributes
					apValue = getAttributeByName(reader, XmlConstants.Attributes.value);
					apValueType = getAttributeByName(reader, XmlConstants.Attributes.type);
					apTaintSubFields = getAttributeByName(reader, XmlConstants.Attributes.taintSubFields)
							.equals(XmlConstants.Values.TRUE);

					// Clear the fields
					apFields.clear();
					apTypes.clear();
				} else if (reader.getLocalName().equals(XmlConstants.Tags.fields) && reader.isStartElement()
						&& stateStack.peek() == State.accessPath) {
					stateStack.push(State.fields);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.field) && reader.isStartElement()
						&& stateStack.peek() == State.fields) {
					stateStack.push(State.field);

					// Read the attributes
					String value = getAttributeByName(reader, XmlConstants.Attributes.value);
					String type = getAttributeByName(reader, XmlConstants.Attributes.type);
					if (value != null && !value.isEmpty() && type != null && !type.isEmpty()) {
						apFields.add(value);
						apTypes.add(value);
					}
				} else if (reader.getLocalName().equals(XmlConstants.Tags.sources) && reader.isStartElement()
						&& stateStack.peek() == State.result) {
					stateStack.push(State.sources);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.source) && reader.isStartElement()
						&& stateStack.peek() == State.sources) {
					stateStack.push(State.source);

					// Read the attributes
					statement = getAttributeByName(reader, XmlConstants.Attributes.statement);
					method = getAttributeByName(reader, XmlConstants.Attributes.method);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.taintPath) && reader.isStartElement()
						&& stateStack.peek() == State.source) {
					stateStack.push(State.taintPath);

					// Clear the old state
					pathElements.clear();
				} else if (reader.getLocalName().equals(XmlConstants.Tags.pathElement) && reader.isStartElement()
						&& stateStack.peek() == State.source) {
					stateStack.push(State.taintPath);

					// Read the attributes
					statement = getAttributeByName(reader, XmlConstants.Attributes.statement);
					method = getAttributeByName(reader, XmlConstants.Attributes.method);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.performanceData) && reader.isStartElement()
						&& stateStack.peek() == State.dataFlowResults) {
					stateStack.push(State.performanceData);
				} else if (reader.getLocalName().equals(XmlConstants.Tags.performanceEntry) && reader.isStartElement()
						&& stateStack.peek() == State.performanceData) {
					stateStack.push(State.performanceEntry);

					// We need a performance data object
					if (perfData == null)
						perfData = results.getOrCreatePerformanceData();

					// Read the attributes
					String perfName = getAttributeByName(reader, XmlConstants.Attributes.name);
					String perfValue = getAttributeByName(reader, XmlConstants.Attributes.value);

					switch (perfName) {
					case XmlConstants.Values.PERF_CALLGRAPH_SECONDS:
						perfData.setCallgraphConstructionSeconds(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_PATH_RECONSTRUCTION_SECONDS:
						perfData.setPathReconstructionSeconds(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_TAINT_PROPAGATION_SECONDS:
						perfData.setTaintPropagationSeconds(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_TOTAL_RUNTIME_SECONDS:
						perfData.setTotalRuntimeSeconds(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_MAX_MEMORY_CONSUMPTION:
						perfData.setMaxMemoryConsumption(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_SOURCE_COUNT:
						perfData.setSourceCount(Integer.parseInt(perfValue));
						break;
					case XmlConstants.Values.PERF_SINK_COUNT:
						perfData.setSinkCount(Integer.parseInt(perfValue));
						break;
					}
				} else if (reader.isEndElement()) {
					stateStack.pop();

					if (reader.getLocalName().equals(XmlConstants.Tags.accessPath)) {
						ap = new SerializedAccessPath(apValue, apValueType, apTaintSubFields,
								apFields.toArray(new String[apFields.size()]),
								apTypes.toArray(new String[apTypes.size()]));
					} else if (reader.getLocalName().equals(XmlConstants.Tags.sink)) {
						sink = new SerializedSinkInfo(ap, statement, method);
					} else if (reader.getLocalName().equals(XmlConstants.Tags.source)) {
						source = new SerializedSourceInfo(ap, statement, method, pathElements);
						results.addResult(source, sink);
					} else if (reader.getLocalName().equals(XmlConstants.Tags.pathElement)) {
						pathElements.add(new SerializedPathElement(ap, statement, method));
					}
				}
			}

			return results;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Converts the given string into an integer
	 * 
	 * @param value The string value to convert
	 * @return The given value as an integer if the string was not null or empty,
	 *         otherwise -1
	 */
	private int int2Str(String value) {
		if (value == null || value.isEmpty())
			return -1;
		return Integer.valueOf(value);
	}

	/**
	 * Gets the value of the XML attribute with the specified id
	 * 
	 * @param reader The reader from which to get the XML data
	 * @param id     The attribute id for which to get the data
	 * @return The data of the given attribute if it exists, otherwise an empty
	 *         string
	 */
	private String getAttributeByName(XMLStreamReader reader, String id) {
		for (int i = 0; i < reader.getAttributeCount(); i++)
			if (reader.getAttributeLocalName(i).equals(id))
				return reader.getAttributeValue(i);
		return "";
	}

}
