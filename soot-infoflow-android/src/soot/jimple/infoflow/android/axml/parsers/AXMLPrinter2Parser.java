package soot.jimple.infoflow.android.axml.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.AXmlResourceParser;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNamespace;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.AXmlTypes;

/**
 * Class for parsing Android binary XML files using the AXMLPrinter2 library
 * 
 * @author Steven Arzt
 */
public class AXMLPrinter2Parser extends AbstractBinaryXMLFileParser {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void parseFile(byte[] inputBuffer) throws IOException {
		InputStream buffer = new BufferedInputStream(new ByteArrayInputStream(inputBuffer));

		// init
		AXmlNode node = null;
		AXmlNode parent = null;

		// create parser and parse the xml's contents
		AXmlResourceParser parser = new AXmlResourceParser();
		parser.open(buffer);

		int type = -1;
		String tag;
		try {
			// Register the namespaces
			for (int i = 0; i < parser.getNamespaceCount(parser.getDepth()); i++) {
				this.document.addNamespace(new AXmlNamespace(parser.getNamespacePrefix(i), parser.getNamespaceUri(i),
						parser.getLineNumber()));
			}

			// Load the nodes
			while ((type = parser.next()) != AXmlResourceParser.END_DOCUMENT) {
				switch (type) {
				// Currently nothing to do at the document's start
				case AXmlResourceParser.START_DOCUMENT:
					break;

				// To handle an opening tag we create a new node
				// and fetch the namespace and all attributes
				case AXmlResourceParser.START_TAG:
					tag = parser.getName();
					parent = node;
					node = new AXmlNode(tag, parser.getNamespace(), parent, false);
					this.addPointer(tag, node);

					// add attributes to node object
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						String name = parser.getAttributeName(i);
						String ns = parser.getAttributeNamespace(i);
						int atype = parser.getAttributeValueType(i);
						AXmlAttribute<?> attr = null;

						// we only parse attribute of types string, boolean and
						// integer
						int resourceId = parser.getAttributeNameResource(i);
						switch (atype) {
						case AXmlTypes.TYPE_STRING:
							attr = new AXmlAttribute<String>(name, resourceId, atype, parser.getAttributeValue(i), ns,
									false);
							break;
						case AXmlTypes.TYPE_INT_BOOLEAN:
							attr = new AXmlAttribute<Boolean>(name, resourceId, atype,
									parser.getAttributeBooleanValue(i, false), ns, false);
							break;
						case AXmlTypes.TYPE_FIRST_INT:
						case AXmlTypes.TYPE_INT_HEX:
							attr = new AXmlAttribute<Integer>(name, resourceId, atype,
									parser.getAttributeIntValue(i, 0), ns, false);
							break;
						case AXmlTypes.TYPE_REFERENCE:
							attr = new AXmlAttribute<Integer>(name, resourceId, atype,
									parser.getAttributeResourceValue(i, 0), ns, false);
							break;
						default:
							logger.error("Unsupported attribute type: %d", atype);
						}

						// if we can't handle the attributes type we simply
						// ignore it
						if (attr != null)
							node.addAttribute(attr);
					}
					break;

				// A closing tag indicates we must move
				// one level upwards in the xml tree
				case AXmlResourceParser.END_TAG:
					this.document.setRootNode(node);
					node = parent;
					parent = (parent == null ? null : parent.getParent());
					break;

				// Android XML documents do not contain text
				case AXmlResourceParser.TEXT:
					break;

				// Currently nothing to do at the document's end, see loop
				// condition
				case AXmlResourceParser.END_DOCUMENT:
					break;
				}
			}
		} catch (XmlPullParserException ex) {
			throw new IOException(ex);
		}
	}

}
