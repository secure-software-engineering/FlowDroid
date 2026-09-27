package soot.jimple.infoflow.android.source.parsers.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Resolves stereotypes, which are essentially like C macros.
 */
public class PreprocessorHandler extends DefaultHandler {
	/* Example:
		<defineStereotype id="CipherFlowConditionsAPIs">
		<valueOnPath invocation="javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
		<valueOnPath invocation="javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String,java.lang.String)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
		<valueOnPath invocation="javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String,java.security.Provider)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
		<valueOnPath invocation="java.security.MessageDigest: java.security.MessageDigest getInstance(java.lang.String)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
		<valueOnPath invocation="java.security.MessageDigest: java.security.MessageDigest getInstance(java.lang.String,java.lang.String)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
		<valueOnPath invocation="java.security.MessageDigest: java.security.MessageDigest getInstance(java.lang.String,java.security.Provider)">
			<param index="0" regex="true" caseSensitive="false">.*${ALGORITHMNAME}.*</param>
		</valueOnPath>
	</defineStereotype>
	<defineStereotype id="CipherFlowConditions">
		<stereotype id="CipherFlowConditionsAPIs">
			<!-- The children are instantiating placeholders in the referenced stereotype -->
			<ALGORITHMNAME>DES</ALGORITHMNAME> <!-- replacing ${ALGORITHMNAME} -->
		</stereotype>
		<stereotype id="CipherFlowConditionsAPIs">
			<ALGORITHMNAME>RC4</ALGORITHMNAME>
		</stereotype>
		<stereotype id="CipherFlowConditionsAPIs">
			<ALGORITHMNAME>Blowfish</ALGORITHMNAME>
		</stereotype>
	</defineStereotype>
	 */

	static interface IHandler {
		public void apply(DefaultHandler handler, Map<String, String> replacements) throws SAXException;
	}

	private static class CharacterHandler implements IHandler {

		private String string;

		public CharacterHandler(char[] ch, int start, int length) {
			this.string = new String(ch, start, length);
		}

		@Override
		public void apply(DefaultHandler handler, Map<String, String> replacements) throws SAXException {
			String s = string;
			for (Entry<String, String> i : replacements.entrySet()) {
				s = s.replace(i.getKey(), i.getValue());
			}
			handler.characters(s.toCharArray(), 0, s.length());
		}

	}

	private static class StartElementHandler implements IHandler {

		private String uri;
		private String localName;
		private String qName;
		private Attributes attributes;

		public StartElementHandler(String uri, String localName, String qName, Attributes attributes) {
			this.uri = uri;
			this.localName = localName;
			this.qName = qName;
			//we have to clone
			this.attributes = new Attributes2Impl(attributes);
		}

		@Override
		public void apply(DefaultHandler handler, Map<String, String> replacements) throws SAXException {
			Attributes2Impl copy = new Attributes2Impl();
			for (int i = 0; i < attributes.getLength(); i++) {
				String uri = attributes.getURI(i);
				String localName = attributes.getLocalName(i);
				String qName = attributes.getQName(i);
				String type = attributes.getType(i);
				String value = attributes.getValue(i);
				if (value != null) {
					for (Entry<String, String> e : replacements.entrySet()) {
						value = value.replace(e.getKey(), e.getValue());
					}
				}
				copy.addAttribute(uri, localName, qName, type, value);
			}
			handler.startElement(uri, localName, qName, copy);
		}

	}

	private static class EndElementHandler implements IHandler {

		private String uri;
		private String localName;
		private String qName;

		public EndElementHandler(String uri, String localName, String qName) {
			this.uri = uri;
			this.localName = localName;
			this.qName = qName;
		}

		@Override
		public void apply(DefaultHandler handler, Map<String, String> replacements) throws SAXException {
			handler.endElement(uri, localName, qName);
		}

	}

	private static class RecordingHandler extends DefaultHandler implements IHandler {
		private List<IHandler> children = new ArrayList<>();

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			children.add(new CharacterHandler(ch, start, length));
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			children.add(new StartElementHandler(uri, localName, qName, attributes));
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			children.add(new EndElementHandler(uri, localName, qName));
		}

		public void apply(DefaultHandler handler, Map<String, String> replacements) throws SAXException {
			for (IHandler c : children)
				c.apply(handler, replacements);

		}

	}

	private DefaultHandler inner;
	private Map<String, RecordingHandler> stereotypes = new HashMap<>();
	private RecordingHandler definingStereotype;
	private String stereotypeId;

	private Map<String, String> replacements;
	private String replacementVariable;
	private String replacementValue;

	public PreprocessorHandler(DefaultHandler inner) {
		this.inner = inner;
	}

	@Override
	public void startDocument() throws SAXException {
		inner.startDocument();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (definingStereotype != null) {
			definingStereotype.characters(ch, start, length);
		} else if (replacementVariable != null) {
			this.replacementValue = new String(ch, start, length);
		} else {
			inner.characters(ch, start, length);
		}

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (definingStereotype != null) {
			if (qName.equals("defineStereotype"))
				throw new IllegalArgumentException("Defining a stereotype within a stereotype is not supported");
			definingStereotype.startElement(uri, localName, qName, attributes);
			return;
		}
		switch (qName) {
		case "defineStereotype":
			if (stereotypeId != null)
				throw new IllegalStateException();
			stereotypeId = attributes.getValue("id");
			if (stereotypeId == null)
				throw new IllegalArgumentException("No stereotype id defined");
			definingStereotype = new RecordingHandler();
			return;
		case "stereotype":
			if (stereotypeId != null)
				throw new IllegalStateException();
			stereotypeId = attributes.getValue("id");
			replacements = new HashMap<>();
			if (stereotypeId == null)
				throw new IllegalArgumentException("No stereotype id defined");
			return;
		default:
			if (replacements != null) {
				//within stereotype replacement
				this.replacementVariable = qName;
				return;
			}
		}
		inner.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (definingStereotype != null && !qName.equals("defineStereotype")) {
			definingStereotype.endElement(uri, localName, qName);
			return;
		}
		switch (qName) {
		case "defineStereotype":
			stereotypes.putIfAbsent(stereotypeId, definingStereotype);
			definingStereotype = null;
			stereotypeId = null;
			return;
		case "stereotype":
			RecordingHandler st = stereotypes.get(stereotypeId);
			if (st == null)
				throw new IllegalArgumentException(
						String.format("Stereotype %s was not found, known stereotypes at this point: %s", stereotypeId,
								stereotypes.keySet()));
			PreprocessorHandler recursion = new PreprocessorHandler(this.inner);
			recursion.stereotypes = stereotypes;
			st.apply(recursion, replacements);
			stereotypeId = null;
			replacements = null;
			return;
		default:
			if (replacementVariable != null) {
				if (replacementValue == null)
					throw new IllegalStateException("No replacement value defined");
				replacements.put("${" + replacementVariable + "}", replacementValue);
				replacementValue = null;
				replacementVariable = null;
				return;
			}
		}
		inner.endElement(uri, localName, qName);
	}

	@Override
	public void endDocument() throws SAXException {
		inner.endDocument();
	}

}
