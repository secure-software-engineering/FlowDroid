/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.rifl;

import java.io.StringWriter;

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

import soot.jimple.infoflow.rifl.RIFLDocument.Assignable;
import soot.jimple.infoflow.rifl.RIFLDocument.Category;
import soot.jimple.infoflow.rifl.RIFLDocument.DomainAssignment;
import soot.jimple.infoflow.rifl.RIFLDocument.DomainSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.FlowPair;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaFieldSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaParameterSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaReturnValueSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkSpec;

/**
 * Class for writing out RIFL-compliant data flow policies
 *
 * @author Steven Arzt
 */
public class RIFLWriter {

	private final RIFLDocument document;

	/**
	 * Creates a new instance of the {@link RIFLWriter} class
	 * 
	 * @param document
	 *            The document to write out
	 */
	public RIFLWriter(RIFLDocument document) {
		this.document = document;
	}

	public String write() {
		try {
			// Create a new document
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement(RIFLConstants.RIFL_SPEC_TAG);
			document.appendChild(rootElement);

			writeInterfaceSpec(document, rootElement);
			writeDomains(document, rootElement);
			writeDomainAssignment(document, rootElement);
			writeFlowPolicy(document, rootElement);

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
	 * Writes out the interface specification component of the RIFL document
	 * 
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeInterfaceSpec(Document document, Element rootElement) {
		Element attackerIO = document.createElement(RIFLConstants.INTERFACE_SPEC_TAG);
		rootElement.appendChild(attackerIO);

		for (Assignable assign : this.document.getInterfaceSpec().getSourcesSinks()) {
			writeAssignable(assign, document, attackerIO);
		}
	}

	/**
	 * Writes out an assignable element the RIFL document
	 * 
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeAssignable(Assignable assign, Document document, Element rootElement) {
		Element attackerIO = document.createElement(RIFLConstants.ASSIGNABLE_TAG);
		rootElement.appendChild(attackerIO);

		attackerIO.setAttribute(RIFLConstants.HANDLE_ATTRIBUTE, assign.getHandle());
		writeSourceSinkSpec(assign.getElement(), document, attackerIO);
	}

	/**
	 * Writes out a source/sink specification object
	 * 
	 * @param spec
	 *            The source/sink specification to write out
	 * @param document
	 *            The document in which to write the source/sink specification
	 * @param parentElement
	 *            The parent element in the DOM tree. This must be <source> or
	 *            <sink>
	 */
	private void writeSourceSinkSpec(SourceSinkSpec spec, Document document, Element parentElement) {
		Element containerElement = null;
		switch (spec.getType()) {
		case Source:
			containerElement = document.createElement(RIFLConstants.SOURCE_TAG);
			break;
		case Sink:
			containerElement = document.createElement(RIFLConstants.SINK_TAG);
			break;
		case Category:
			containerElement = document.createElement(RIFLConstants.CATEGORY_TAG);
			break;
		default:
			throw new RuntimeException("Invalid source/sink type");
		}
		parentElement.appendChild(containerElement);

		if (spec instanceof JavaParameterSpec)
			writeJavaParameterSpec((JavaParameterSpec) spec, document, containerElement);
		else if (spec instanceof JavaFieldSpec)
			writeJavaFieldSpec((JavaFieldSpec) spec, document, containerElement);
		else if (spec instanceof JavaReturnValueSpec)
			writeJavaReturnValueSpec((JavaReturnValueSpec) spec, document, containerElement);
		else if (spec instanceof Category)
			writeCategory((Category) spec, document, containerElement);
		else
			throw new RuntimeException("Unsupported source or sink specification type");
	}

	/**
	 * Writes out a source/sink specification object for Java method parameters
	 * 
	 * @param spec
	 *            The source/sink specification to write out
	 * @param document
	 *            The document in which to write the source/sink specification
	 * @param parentElement
	 *            The parent element in the DOM tree. This must be <source> or
	 *            <sink>
	 */
	private void writeJavaParameterSpec(JavaParameterSpec spec, Document document, Element parentElement) {
		Element parameter = document.createElement(RIFLConstants.PARAMETER_TAG);
		parentElement.appendChild(parameter);

		parameter.setAttribute(RIFLConstants.CLASS_ATTRIBUTE, spec.getClassName());
		parameter.setAttribute(RIFLConstants.METHOD_ATTRIBUTE, spec.getHalfSignature());
		parameter.setAttribute(RIFLConstants.PARAMETER_ATTRIBUTE, Integer.toString(spec.getParamIdx()));
	}

	/**
	 * Writes out a source/sink specification object for Java static fields
	 * 
	 * @param spec
	 *            The source/sink specification to write out
	 * @param document
	 *            The document in which to write the source/sink specification
	 * @param parentElement
	 *            The parent element in the DOM tree. This must be <source> or
	 *            <sink>
	 */
	private void writeJavaFieldSpec(JavaFieldSpec spec, Document document, Element parentElement) {
		Element parameter = document.createElement(RIFLConstants.FIELD_TAG);
		parentElement.appendChild(parameter);

		parameter.setAttribute(RIFLConstants.CLASS_ATTRIBUTE, spec.getClassName());
		parameter.setAttribute(RIFLConstants.FIELD_ATTRIBUTE, spec.getFieldName());
	}

	/**
	 * Writes out a source/sink specification object for the return values of
	 * Java methods
	 * 
	 * @param spec
	 *            The source/sink specification to write out
	 * @param document
	 *            The document in which to write the source/sink specification
	 * @param parentElement
	 *            The parent element in the DOM tree. This must be <source> or
	 *            <sink>
	 */
	private void writeJavaReturnValueSpec(JavaReturnValueSpec spec, Document document, Element parentElement) {
		Element parameter = document.createElement(RIFLConstants.RETURN_VALUE_TAG);
		parentElement.appendChild(parameter);

		parameter.setAttribute(RIFLConstants.CLASS_ATTRIBUTE, spec.getClassName());
		parameter.setAttribute(RIFLConstants.METHOD_ATTRIBUTE, spec.getHalfSignature());
	}

	/**
	 * Writes out a category specification object Java methods
	 * 
	 * @param spec
	 *            The source/sink specification to write out
	 * @param document
	 *            The document in which to write the source/sink specification
	 * @param parentElement
	 *            The parent element in the DOM tree. This must be <source> or
	 *            <sink>
	 */
	private void writeCategory(Category category, Document document, Element parentElement) {
		Element categoryElement = document.createElement(RIFLConstants.CATEGORY_TAG);
		parentElement.appendChild(categoryElement);

		categoryElement.setAttribute(RIFLConstants.NAME_ATTRIBUTE, category.getName());
		for (SourceSinkSpec element : category.getElements()) {
			writeSourceSinkSpec(element, document, categoryElement);
		}
	}

	/**
	 * Writes out the domains component of the RIFL document
	 * 
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeDomains(Document document, Element rootElement) {
		Element domains = document.createElement(RIFLConstants.DOMAINS_TAG);
		rootElement.appendChild(domains);

		for (DomainSpec spec : this.document.getDomains())
			writeDomainSpec(spec, document, domains);
	}

	/**
	 * Writes out a domain specification object
	 * 
	 * @param spec
	 *            The domain specification to write out
	 * @param document
	 *            The document in which to write the domain specification
	 * @param parentElement
	 *            The parent element in the DOM tree.
	 */
	private void writeDomainSpec(DomainSpec spec, Document document, Element parentElement) {
		Element categoryDomain = document.createElement(RIFLConstants.DOMAIN_TAG);
		parentElement.appendChild(categoryDomain);
		categoryDomain.setAttribute(RIFLConstants.NAME_ATTRIBUTE, spec.getName());
	}

	/**
	 * Writes out the domains assignments section of the RIFL document
	 * 
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeDomainAssignment(Document document, Element rootElement) {
		Element domainAssignment = document.createElement(RIFLConstants.DOMAIN_ASSIGNMENT_TAG);
		rootElement.appendChild(domainAssignment);

		for (DomainAssignment spec : this.document.getDomainAssignment())
			writeDomainAssignment(spec, document, domainAssignment);
	}

	/**
	 * Writes out a source or sink domain pair
	 * 
	 * @param pair
	 *            The domain assignment to write out
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeDomainAssignment(DomainAssignment pair, Document document, Element rootElement) {
		final Element pairElement = document.createElement(RIFLConstants.ASSIGN_TAG);
		rootElement.appendChild(pairElement);

		pairElement.setAttribute(RIFLConstants.HANDLE_ATTRIBUTE, pair.getSourceOrSink().getHandle());
		pairElement.setAttribute(RIFLConstants.DOMAIN_ATTRIBUTE, pair.getDomain().getName());
	}

	/**
	 * Writes out the flow policy component of the RIFL document
	 * 
	 * @param document
	 *            The XML document in which to write
	 * @param rootElement
	 *            The root element of the document
	 */
	private void writeFlowPolicy(Document document, Element rootElement) {
		Element flowPolicy = document.createElement(RIFLConstants.FLOW_RELATION_TAG);
		rootElement.appendChild(flowPolicy);

		for (FlowPair pair : this.document.getFlowPolicy())
			writeFlowPair(pair, document, flowPolicy);
	}

	/**
	 * Writes out a flow pair object for the use inside the flow policy
	 * 
	 * @param pair
	 *            The flow pair to write out
	 * @param document
	 *            The document in which to write the flow pair
	 * @param parentElement
	 *            The parent element in the DOM tree
	 */
	private void writeFlowPair(FlowPair pair, Document document, Element parentElement) {
		Element flowPair = document.createElement(RIFLConstants.FLOW_TAG);
		parentElement.appendChild(flowPair);

		flowPair.setAttribute(RIFLConstants.FROM_ATTRIBUTE, pair.getFirstDomain().getName());
		flowPair.setAttribute(RIFLConstants.TO_ATTRIBUTE, pair.getSecondDomain().getName());
	}

	/**
	 * Gets the document associated with this writer
	 * 
	 * @return The document associated with this writer
	 */
	public RIFLDocument getDocument() {
		return this.document;
	}
}
