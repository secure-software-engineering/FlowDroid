package soot.jimple.infoflow.methodSummary.xml;

import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import soot.jimple.infoflow.util.ResourceUtils;

/**
 * Abstract base class for reading flow summaries from XML files
 * 
 * @author Steven Arzt
 *
 */
abstract class AbstractXMLReader {

	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	/**
	 * Checks whether the given XML is valid against the XSD for the new data
	 * format.
	 * 
	 * @param is The stream from which to read the XML data
	 * @return true = valid XML false = invalid XML
	 * @throws IOException
	 */
	protected static boolean verifyXML(FileReader reader, String xsdFilePath) throws IOException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		StreamSource xsdFile = new StreamSource(ResourceUtils.getResourceStream(xsdFilePath));
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

	/**
	 * Gets the value of the XML attribute with the specified id
	 * 
	 * @param reader The reader from which to get the XML data
	 * @param id     The attribute id for which to get the data
	 * @return The data of the given attribute if it exists, otherwise an empty
	 *         string
	 */
	protected String getAttributeByName(XMLStreamReader reader, String id) {
		for (int i = 0; i < reader.getAttributeCount(); i++)
			if (reader.getAttributeLocalName(i).equals(id))
				return reader.getAttributeValue(i);
		return "";
	}

}
