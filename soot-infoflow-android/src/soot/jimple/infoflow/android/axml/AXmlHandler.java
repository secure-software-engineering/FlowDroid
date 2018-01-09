package soot.jimple.infoflow.android.axml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pxb.android.axml.AxmlWriter;
import pxb.android.axml.NodeVisitor;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.axml.parsers.IBinaryXMLFileParser;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;

/**
 * {@link AXmlHandler} provides functionality to parse a byte compressed android xml file and access all nodes.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class AXmlHandler {
	public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

	/**
	 * Contains the byte compressed xml which was parsed by this {@link AXmlHandler}.
	 */
	protected byte[] xml;

	/**
	 * The parser used for actually reading out the binary XML file
	 */
	protected final IBinaryXMLFileParser parser;

	/**
	 * Creates a new {@link AXmlHandler} which parses the {@link InputStream}.
	 * 
	 * @param	aXmlIs					InputStream reading a byte compressed android xml file
	 * @throws	IOException				if an I/O error occurs.
	 */
	public AXmlHandler(InputStream aXmlIs) throws IOException {
		this(aXmlIs, new AXML20Parser());
	}

	/**
	 * Creates a new {@link AXmlHandler} which parses the {@link InputStream}.
	 * 
	 * @param	aXmlIs					InputStream reading a byte compressed android xml file
	 * @param	parser					The parser implementation to be used
	 * @throws	IOException				if an I/O error occurs.
	 */
	public AXmlHandler(InputStream aXmlIs, IBinaryXMLFileParser parser) throws IOException {
		if (aXmlIs == null)
			throw new RuntimeException("NULL input stream for AXmlHandler");

		// wrap the InputStream within a BufferedInputStream
		// to have mark() and reset() methods
		BufferedInputStream buffer = new BufferedInputStream(aXmlIs);

		// read xml one time for writing the output later on
		{
			List<byte[]> chunks = new ArrayList<byte[]>();
			int bytesRead = 0;
			while (aXmlIs.available() > 0) {
				byte[] nextChunk = new byte[aXmlIs.available()];
				int chunkSize = buffer.read(nextChunk);
				if (chunkSize < 0)
					break;
				chunks.add(nextChunk);
				bytesRead += chunkSize;
			}

			// Create the full array
			this.xml = new byte[bytesRead];
			int bytesCopied = 0;
			for (byte[] chunk : chunks) {
				int toCopy = Math.min(chunk.length, bytesRead - bytesCopied);
				System.arraycopy(chunk, 0, this.xml, bytesCopied, toCopy);
				bytesCopied += toCopy;
			}
		}

		parser.parseFile(this.xml);
		this.parser = parser;
	}

	/**
	 * Returns the Android xml document.
	 * 
	 * @return	the Android xml document
	 */
	public AXmlDocument getDocument() {
		return parser.getDocument();
	}

	/**
	 * Returns a list containing all nodes of the xml document which have the given tag.
	 * 
	 * @param	tag		the tag being search for
	 * @return	list pointing on all nodes which have the given tag.
	 */
	public List<AXmlNode> getNodesWithTag(String tag) {
		return parser.getNodesWithTag(tag);
	}

	/**
	 * Returns the xml document as a compressed android xml byte array.
	 * This will consider all changes made to the root node and it's children.
	 * 
	 * @return	android byte compressed xml
	 */
	public byte[] toByteArray() {
		try {
			AxmlWriter aw = new AxmlWriter();

			// Write out all namespaces
			for (AXmlNamespace ns : this.getDocument().getNamespaces())
				aw.ns(ns.getPrefix(), ns.getUri(), ns.getLine());

			writeNode(aw, this.getDocument().getRootNode());

			return aw.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static final int resId_maxSdkVersion = 16843377;
	private static final int resId_minSdkVersion = 16843276;
	private static final int resId_name = 16842755;
	private static final int resId_onClick = 16843375;

	/**
	 * Returns the Android resource Id of the attribute which has the given name.
	 * 
	 * @param	name	the attribute's name.
	 * @return	the resource Id defined by Android or -1 if the attribute does not exist.
	 * @see		android.R.attr
	 */
	public static int getAttributeResourceId(String name) {
		// try to get attribute's resource Id from Androids R class. Since we
		// don't want a hard-coded reference to the Android classes, we maintain
		// our own list.
		if (name.equals("name"))
			return resId_name;
		else if (name.equals("maxSdkVersion"))
			return resId_maxSdkVersion;
		else if (name.equals("minSdkVersion"))
			return resId_minSdkVersion;
		else if (name.equals("onClick"))
			return resId_onClick;

		// If we couldn't find the value, try to find Android's R class in Soot
		SootClass rClass = Scene.v().forceResolve("android.R$attr", SootClass.BODIES);
		if (!rClass.declaresFieldByName(name))
			return -1;
		SootField idField = rClass.getFieldByName(name);
		for (Tag t : idField.getTags())
			if (t instanceof IntegerConstantValueTag) {
				IntegerConstantValueTag cvt = (IntegerConstantValueTag) t;
				return cvt.getIntValue();
			}
		return -1;
	}

	/**
	 * Writes out the given node
	 * @param parentNodeVisitor The visitor associated with the parent node
	 * under which the given node shall be registered as a child
	 * @param node The child node to write out under the given parent node
	 */
	private void writeNode(NodeVisitor parentNodeVisitor, AXmlNode node) {
		NodeVisitor childNodeVisitor = parentNodeVisitor.child(node.getNamespace(), node.getTag());

		// do not add excluded nodes
		if (!node.isIncluded()) {
			return;
		}

		// Write the attributes
		for (AXmlAttribute<?> attr : node.getAttributes().values()) {
			String namespace = attr.getNamespace();
			if (namespace != null && namespace.isEmpty())
				namespace = null;

			int resourceId = attr.getResourceId();
			if (resourceId < 0 && !node.getTag().equals("manifest"))
				resourceId = getAttributeResourceId(attr.getName());

			int attrType = attr.getAttributeType();
			if (attrType < 0)
				attrType = attr.getType();

			childNodeVisitor.attr(namespace, attr.getName(), resourceId, attrType, attr.getValue());
		}

		// Write the child nodes
		for (AXmlNode child : node.getChildren())
			writeNode(childNodeVisitor, child);

		childNodeVisitor.end();
	}

	@Override
	public String toString() {
		return this.toString(this.getDocument().getRootNode(), 0);
	}

	/**
	 * Returns a textual representation of the given node and it's data.
	 * 
	 * @param	node	current node which will be printed
	 * @param	depth	for the padding (indent)
	 * @return	String representation of the given node, it's attributes and children
	 */
	protected String toString(AXmlNode node, int depth) {
		StringBuilder sb = new StringBuilder();

		// construct indent for pretty console printing
		StringBuilder padding = new StringBuilder();
		for (int i = 0; i < depth; i++)
			padding.append("	");

		// append this nodes tag
		sb.append(padding).append(node.getTag());

		// add attributes
		for (AXmlAttribute<?> attr : node.getAttributes().values()) {
			sb.append("\n").append(padding).append("- ").append(attr.getName()).append(": ").append(attr.getValue());
		}

		// recursivly append children
		for (AXmlNode n : node.getChildren()) {
			sb.append("\n").append(this.toString(n, depth + 1));
		}

		return sb.toString();
	}
}
