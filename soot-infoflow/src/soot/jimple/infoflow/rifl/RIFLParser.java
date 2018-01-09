package soot.jimple.infoflow.rifl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.rifl.RIFLDocument.Assignable;
import soot.jimple.infoflow.rifl.RIFLDocument.Category;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkType;

/**
 * Class for reading in RIFL specifications from file
 * 
 * @author Steven Arzt
 *
 */
public class RIFLParser {
	
	/**
	 * States during RIFL document parsing. This corresponds to the various tags
	 * in the specification.
	 */
	private enum RIFLState {
		RiflSpec,
		InterfaceSpec,
		Assignable,
		Source,
		Sink,
		ReturnValue,
		Parameter,
		Field,
		Category,
		Domains,
		Domain,
		FlowRelation,
		Flow,
		DomainAssignments,
		Assign
	}
	
	/**
	 * Parses the given RIFL specification file into a DOM
	 * @param riflFile The RIFL file to parse
	 * @return The DOM generated from the given RIFL file
	 * @throws IOException Thrown if the XML parsing failed
	 * @throws SAXException Thrown if the XML parser could not be initialized
	 */
	public RIFLDocument parseRIFL(File riflFile) throws SAXException, IOException {
		// Check the file
		if (!riflFile.exists())
			throw new FileNotFoundException("RIFL file " + riflFile + " not found");
		
		final RIFLDocument doc = new RIFLDocument();
		
		// Load the file
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(riflFile, new DefaultHandler() {
				
				private Stack<RIFLState> stateStack = new Stack<>();
				
				private String assignableHandle = "";
				private Assignable assignable = null;
				private SourceSinkSpec sourceSinkSpec = null;
				
				@Override
				public void startDocument() throws SAXException {
					super.startDocument();
					stateStack.push(RIFLState.RiflSpec);
				}
				
				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					super.startElement(uri, localName, qName, attributes);
					
					RIFLState curState = stateStack.peek();
					if (curState == RIFLState.RiflSpec
							&& localName.equalsIgnoreCase(RIFLConstants.INTERFACE_SPEC_TAG)) {
						stateStack.push(RIFLState.InterfaceSpec);
					}
					else if (curState == RIFLState.InterfaceSpec
							&& localName.equalsIgnoreCase(RIFLConstants.ASSIGNABLE_TAG)) {
						stateStack.push(RIFLState.Assignable);
						assignableHandle = attributes.getValue(RIFLConstants.HANDLE_ATTRIBUTE);
					}
					else if (curState == RIFLState.Assignable
							&& localName.equalsIgnoreCase(RIFLConstants.SOURCE_TAG)) {
						stateStack.push(RIFLState.Source);
					}
					else if ((curState == RIFLState.Source || curState == RIFLState.Sink)
							&& localName.equalsIgnoreCase(RIFLConstants.PARAMETER_TAG)) {
						stateStack.push(RIFLState.Parameter);
						
						int parameterIdx = Integer.parseInt(attributes.getValue(RIFLConstants.PARAMETER_ATTRIBUTE));
						String className = attributes.getValue(RIFLConstants.CLASS_ATTRIBUTE);
						String methodSig = attributes.getValue(RIFLConstants.METHOD_ATTRIBUTE);
						
						SourceSinkType type = getSourceSinkTypeFromState(curState);
						SourceSinkSpec newSpec = doc.new JavaParameterSpec(type, className,
								methodSig, parameterIdx);
						
						if (sourceSinkSpec == null)
							sourceSinkSpec = newSpec;
						else if (sourceSinkSpec instanceof Category)
							((Category) sourceSinkSpec).getElements().add(newSpec);
					}
					else if ((curState == RIFLState.Source || curState == RIFLState.Sink)
							&& localName.equalsIgnoreCase(RIFLConstants.FIELD_TAG)) {
						stateStack.push(RIFLState.Field);
						
						String className = attributes.getValue(RIFLConstants.CLASS_ATTRIBUTE);
						String fieldSig = attributes.getValue(RIFLConstants.FIELD_ATTRIBUTE);
						
						SourceSinkType type = getSourceSinkTypeFromState(curState);
						SourceSinkSpec newSpec = doc.new JavaFieldSpec(type, className, fieldSig);
						
						if (sourceSinkSpec == null)
							sourceSinkSpec = newSpec;
						else if (sourceSinkSpec instanceof Category)
							((Category) sourceSinkSpec).getElements().add(newSpec);
					}
					else if ((curState == RIFLState.Source || curState == RIFLState.Sink)
							&& localName.equalsIgnoreCase(RIFLConstants.RETURN_VALUE_TAG)) {
						stateStack.push(RIFLState.Field);
						
						String className = attributes.getValue(RIFLConstants.CLASS_ATTRIBUTE);
						String methodSig = attributes.getValue(RIFLConstants.METHOD_ATTRIBUTE);
						
						SourceSinkType type = getSourceSinkTypeFromState(curState);
						SourceSinkSpec newSpec = doc.new JavaReturnValueSpec(type, className, methodSig);
						
						if (sourceSinkSpec == null)
							sourceSinkSpec = newSpec;
						else if (sourceSinkSpec instanceof Category)
							((Category) sourceSinkSpec).getElements().add(newSpec);
					}
					else if (curState == RIFLState.Assignable
							&& localName.equalsIgnoreCase(RIFLConstants.CATEGORY_TAG)) {
						stateStack.push(RIFLState.Category);
						sourceSinkSpec = doc.new Category(attributes.getValue(RIFLConstants.NAME_ATTRIBUTE));
					}
					else if (curState == RIFLState.RiflSpec
							&& localName.equalsIgnoreCase(RIFLConstants.DOMAINS_TAG)) {
						stateStack.push(RIFLState.Domains);
					}
					else if (curState == RIFLState.Domains
							&& localName.equalsIgnoreCase(RIFLConstants.DOMAIN_TAG)) {
						stateStack.push(RIFLState.Domain);
						doc.getDomains().add(doc.new DomainSpec(attributes.getValue(RIFLConstants.NAME_ATTRIBUTE)));
					}
					else if (curState == RIFLState.RiflSpec
							&& localName.equalsIgnoreCase(RIFLConstants.FLOW_RELATION_TAG)) {
						stateStack.push(RIFLState.FlowRelation);
					}
					else if (curState == RIFLState.FlowRelation
							&& localName.equalsIgnoreCase(RIFLConstants.FLOW_TAG)) {
						stateStack.push(RIFLState.Flow);
						
						String fromDomain = attributes.getValue(RIFLConstants.FROM_ATTRIBUTE);
						String toDomain = attributes.getValue(RIFLConstants.TO_ATTRIBUTE);
						
						doc.getFlowPolicy().add(doc.new FlowPair(doc.getDomainByName(fromDomain),
								doc.getDomainByName(toDomain)));
					}
					else if (curState == RIFLState.RiflSpec
							&& localName.equalsIgnoreCase(RIFLConstants.DOMAIN_ASSIGNMENT_TAG)) {
						stateStack.push(RIFLState.DomainAssignments);
					}
					else if (curState == RIFLState.DomainAssignments
							&& localName.equalsIgnoreCase(RIFLConstants.ASSIGN_TAG)) {
						stateStack.push(RIFLState.Assign);
						
						String assignableName = attributes.getValue(RIFLConstants.HANDLE_ATTRIBUTE);
						String domainName = attributes.getValue(RIFLConstants.DOMAIN_ATTRIBUTE);
						
						doc.getDomainAssignment().add(doc.new DomainAssignment(
								doc.getInterfaceSpec().getElementByHandle(assignableName),
								doc.getDomainByName(domainName)));
					}
				}
				
				private SourceSinkType getSourceSinkTypeFromState(
						RIFLState curState) {
					switch (curState) {
					case Source:
						return SourceSinkType.Source;
					case Sink:
						return SourceSinkType.Sink;
					default:
						throw new RuntimeException("Invalid source/sink type: " + curState);
					}
				}

				@Override
				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					super.endElement(uri, localName, qName);
					
					RIFLState curState = stateStack.pop();
					if (curState == RIFLState.Assignable) {
						assignable = doc.new Assignable(assignableHandle, sourceSinkSpec);
						doc.getInterfaceSpec().getSourcesSinks().add(assignable);
						sourceSinkSpec = null;
					}
				}
				
			});
			
			return doc;
		}
		catch (ParserConfigurationException ex) {
			// should not happen
			return null;
		}
	}
	
}
