package soot.jimple.infoflow.methodSummary.xml;

import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_ACCESSPATH;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_ACCESSPATHTYPES;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_BASETYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_FLOWTYPE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.ATTRIBUTE_PARAMETER_INDEX;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_FLOW;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_FLOWS;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SINK;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.TREE_SOURCE;
import static soot.jimple.infoflow.methodSummary.xml.XMLConstants.VALUE_TRUE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

public class SummaryWriter {

	private final int FILE_FORMAT_VERSION = 103;

	public SummaryWriter() {

	}

	/**
	 * Writes the given class summaries into files, one per class
	 * 
	 * @param file    The target directory in which to place the class summary files
	 * @param summary The class summaries to write out
	 * @throws IOException        Thrown if the target file could not be found or
	 *                            created
	 * @throws XMLStreamException Thrown if the XML data could not be written
	 */
	public void write(File file, ClassSummaries summary) throws IOException, XMLStreamException {
		for (String className : summary.getClasses()) {
			String fileName = file.getAbsolutePath() + File.separatorChar + className + ".xml";
			write(new File(fileName), summary.getClassSummaries(className));
		}
	}

	/**
	 * Writes the given method summaries into the given XML file
	 * 
	 * @param file    The XML file in which to write the summaries
	 * @param summary The method summaries to be written out
	 * @throws XMLStreamException Thrown if the XML data could not be written
	 * @throws IOException        Thrown if the target file could not be written
	 */
	public void write(File file, ClassMethodSummaries summary) throws XMLStreamException, IOException {
		// Do not write out empty summaries
		if (summary.isEmpty())
			return;

		try (OutputStream out = new FileOutputStream(file)) {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = factory.createXMLStreamWriter(out);

			writer.writeStartDocument();
			writer.writeStartElement(XMLConstants.TREE_SUMMARY);
			writer.writeAttribute(XMLConstants.ATTRIBUTE_FORMAT_VERSION, FILE_FORMAT_VERSION + "");
			if (summary.hasInterfaceInfo())
				writer.writeAttribute(XMLConstants.ATTRIBUTE_IS_INTERFACE,
						summary.isInterface() ? XMLConstants.VALUE_TRUE : XMLConstants.VALUE_FALSE);

			MethodSummaries methodSummaries = summary.getMethodSummaries();

			if (summary.hasInterfaces()) {
				writer.writeStartElement(XMLConstants.TREE_HIERARCHY);
				{
					String superClass = summary.getSuperClass();
					if (superClass != null && !superClass.isEmpty())
						writer.writeAttribute(XMLConstants.ATTRIBUTE_SUPERCLASS, superClass);
				}
				writeInterfaces(summary, writer);
				writer.writeEndElement(); // end hierarchy tree
			}

			writer.writeStartElement(XMLConstants.TREE_METHODS);
			writeMethodFlows(methodSummaries, writer);
			writer.writeEndElement(); // end methods tree

			if (methodSummaries.hasGaps()) {
				writer.writeStartElement(XMLConstants.TREE_GAPS);
				writeGaps(methodSummaries, writer);
				writer.writeEndElement(); // end gaps tree
			}

			writer.writeEndDocument();
			writer.close();
		}
	}

	private void writeInterfaces(ClassMethodSummaries summary, XMLStreamWriter writer) throws XMLStreamException {
		for (String intf : summary.getInterfaces()) {
			writer.writeStartElement(XMLConstants.TREE_INTERFACE);
			writer.writeAttribute(XMLConstants.ATTRIBUTE_NAME, intf);
			writer.writeEndElement(); // close interface
		}
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
		List<String> sortedMethods = new ArrayList<>(summary.getFlows().keySet());
		sortedMethods.sort(new SortMethodsByNameComparator());

		for (String methodSig : sortedMethods) {
			// write method sub tree
			writer.writeStartElement(XMLConstants.TREE_METHOD);
			writer.writeAttribute(XMLConstants.ATTRIBUTE_METHOD_SIG, methodSig);

			if (summary != null && summary.hasFlows()) {
				writer.writeStartElement(TREE_FLOWS);
				List<MethodFlow> sortedMethodFlows = new ArrayList<>(summary.getFlows().get(methodSig));
				sortedMethodFlows.sort(new SortFlowsComparator());

				for (MethodFlow data : sortedMethodFlows)
					if (!data.isCustom()) {
						writer.writeStartElement(TREE_FLOW);
						writer.writeAttribute(XMLConstants.ATTRIBUTE_IS_ALIAS, data.isAlias() + "");
						writeFlowSource(writer, data);
						writeFlowSink(writer, data);
						writer.writeEndElement(); // end flow
					}
				writer.writeEndElement(); // close flows
			}
			writer.writeEndElement(); // close method
		}
	}

	/**
	 * Comparator for sorting method signatures according to the method name
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class SortMethodsByNameComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			String name1 = SootMethodRepresentationParser.v().getMethodNameFromSubSignature(o1);
			String name2 = SootMethodRepresentationParser.v().getMethodNameFromSubSignature(o2);
			return name1.compareTo(name2);
		}

	}

	/**
	 * Comparator for sorting flows inside a method according to type and parameter
	 * index
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class SortFlowsComparator implements Comparator<MethodFlow> {

		@Override
		public int compare(MethodFlow o1, MethodFlow o2) {
			if (o1.source().isParameter() && !o2.source().isParameter())
				return -1;
			if (o1.source().isParameter() && o2.source().isParameter())
				return o1.source().getParameterIndex() - o2.source().getParameterIndex();
			return 0;
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
			writer.writeAttribute(ATTRIBUTE_PARAMETER_INDEX, currentFlow.getParameterIndex() + "");
		else if (currentFlow.isGapBaseObject()) {
			// nothing special to write
		} else
			throw new RuntimeException("Unsupported source or sink type " + currentFlow.getType());

		writer.writeAttribute(ATTRIBUTE_BASETYPE, currentFlow.getBaseType());
		if (currentFlow.hasAccessPath()) {
			final AccessPathFragment accessPath = currentFlow.getAccessPath();
			if (accessPath != null && !accessPath.isEmpty()) {
				writer.writeAttribute(ATTRIBUTE_ACCESSPATH, Arrays.toString(accessPath.getFields()));
				writer.writeAttribute(ATTRIBUTE_ACCESSPATHTYPES, Arrays.toString(accessPath.getFieldTypes()));
			}
		}
		if (currentFlow.getGap() != null)
			writer.writeAttribute(XMLConstants.ATTRIBUTE_GAP, currentFlow.getGap().getID() + "");
	}

}
