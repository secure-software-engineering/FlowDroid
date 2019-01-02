package soot.jimple.infoflow.methodSummary.xml;

import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_ACCESSPATH;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_ACCESSPATHTYPES;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_BASETYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_FLOWTYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_PARAMTER_INDEX;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_FLOW;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_FLOWS;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SINK;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SOURCE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.VALUE_TRUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

public class XMLWriter {

	private final int FILE_FORMAT_VERSION = 101;

	public XMLWriter() {

	}

	/**
	 * Writes the given class summaries into files, one per class
	 * 
	 * @param file    The target directory in which to place the class summary files
	 * @param summary The class summaries to write out
	 * @throws FileNotFoundException Thrown if the target file could not be found or
	 *                               created
	 * @throws XMLStreamException    Thrown if the XML data could not be written
	 */
	public void write(File file, ClassSummaries summary) throws FileNotFoundException, XMLStreamException {
		for (String className : summary.getClasses()) {
			String fileName = file.getAbsolutePath() + File.separatorChar + className + ".xml";
			write(new File(fileName), summary.getClassSummaries(className).getMethodSummaries());
		}
	}

	/**
	 * Writes the given method summaries into the given XML file
	 * 
	 * @param file    The XML file in which to write the summaries
	 * @param summary The method summaries to be written out
	 * @throws FileNotFoundException Thrown if the target file could not be found or
	 *                               created
	 * @throws XMLStreamException    Thrown if the XML data could not be written
	 */
	public void write(File file, MethodSummaries summary) throws FileNotFoundException, XMLStreamException {
		// Do not write out empty summaries
		if (summary == null || summary.isEmpty())
			return;

		OutputStream out = new FileOutputStream(file);
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out);

		writer.writeStartDocument();
		writer.writeStartElement(XMLConstants.TREE_SUMMARY);
		writer.writeAttribute(XMLConstants.ATTRIBUTE_FORMAT_VERSION, FILE_FORMAT_VERSION + "");

		writer.writeStartElement(XMLConstants.TREE_METHODS);
		writeMethodFlows(summary, writer);
		writer.writeEndElement(); // end methods tree

		writer.writeStartElement(XMLConstants.TREE_GAPS);
		writeGaps(summary, writer);
		writer.writeEndElement(); // end gaps tree

		writer.writeEndDocument();
		writer.close();
	}

	private void writeGaps(MethodSummaries summary, XMLStreamWriter writer) throws XMLStreamException {
		for (GapDefinition gap : summary.getGaps().values()) {
			writer.writeStartElement(XMLConstants.TREE_GAP);
			writer.writeAttribute(XMLConstants.ATTRIBUTE_ID, gap.getID() + "");
			writer.writeAttribute(XMLConstants.ATTRIBUTE_METHOD_SIG, gap.getSignature());
			writer.writeEndElement(); // close gap
		}
	}

	private void writeMethodFlows(MethodSummaries summary, XMLStreamWriter writer) throws XMLStreamException {
		for (String methodSig : summary.getFlows().keySet()) {
			// write method sub tree
			writer.writeStartElement(XMLConstants.TREE_METHOD);
			writer.writeAttribute(XMLConstants.ATTRIBUTE_METHOD_SIG, methodSig);

			writer.writeStartElement(TREE_FLOWS);
			for (MethodFlow data : summary.getFlows().get(methodSig))
				if (!data.isCustom()) {
					writer.writeStartElement(TREE_FLOW);
					writer.writeAttribute(XMLConstants.ATTRIBUTE_IS_ALIAS, data.isAlias() + "");
					writeFlowSource(writer, data);
					writeFlowSink(writer, data);
					writer.writeEndElement(); // end flow
				}
			writer.writeEndElement(); // close flows
			writer.writeEndElement(); // close method
		}
	}

	private void writeFlowSink(XMLStreamWriter writer, MethodFlow data) throws XMLStreamException {
		writer.writeStartElement(TREE_SINK);
		writeAbstractFlowSinkSource(writer, data.sink(), data.methodSig());
		if (data.sink().taintSubFields())
			writer.writeAttribute(XMLConstants.ATTRIBUTE_TAINT_SUB_FIELDS, VALUE_TRUE);
		writer.writeEndElement();
	}

	private void writeFlowSource(XMLStreamWriter writer, MethodFlow data) throws XMLStreamException {
		writer.writeStartElement(TREE_SOURCE);
		writeAbstractFlowSinkSource(writer, data.source(), data.methodSig());
		writer.writeEndElement();

	}

	private void writeAbstractFlowSinkSource(XMLStreamWriter writer, AbstractFlowSinkSource currentFlow,
			String methodSig) throws XMLStreamException {
		writer.writeAttribute(ATTRIBUTE_FLOWTYPE, currentFlow.getType().toString());

		if (currentFlow.isField() || currentFlow.isReturn()) {
			// nothing we need to write in the xml file here (we write the
			// access path later)
		} else if (currentFlow.isParameter())
			writer.writeAttribute(ATTRIBUTE_PARAMTER_INDEX, currentFlow.getParameterIndex() + "");
		else if (currentFlow.isGapBaseObject()) {
			// nothing special to write
		} else
			throw new RuntimeException("Unsupported source or sink type " + currentFlow.getType());

		writer.writeAttribute(ATTRIBUTE_BASETYPE, currentFlow.getBaseType());
		if (currentFlow.hasAccessPath() && currentFlow.getAccessPath() != null) {
			writer.writeAttribute(ATTRIBUTE_ACCESSPATH, Arrays.toString(currentFlow.getAccessPath()));
			writer.writeAttribute(ATTRIBUTE_ACCESSPATHTYPES, Arrays.toString(currentFlow.getAccessPathTypes()));
		}
		if (currentFlow.getGap() != null)
			writer.writeAttribute(XMLConstants.ATTRIBUTE_GAP, currentFlow.getGap().getID() + "");
	}

}
