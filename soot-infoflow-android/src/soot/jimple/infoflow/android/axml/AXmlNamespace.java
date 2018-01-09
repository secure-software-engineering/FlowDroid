package soot.jimple.infoflow.android.axml;

/**
 * Represents a namesapce in an Android XML document.
 * 
 * @author Steven Arzt
 */
public class AXmlNamespace {
	
	protected String prefix;
	
	protected String uri;
	
	protected int line;
	
	/**
	 * Creates a new namespace definition for Android xml documents
	 * @param prefix The prefix to be used when referring to this namespace
	 * @param uri The uri uniquely identifying the namespace
	 * @param line The line in which the namespace was defined
	 */
	public AXmlNamespace(String prefix, String uri, int line) {
		super();
		this.prefix = prefix;
		this.uri = uri;
		this.line = line;
	}
	
	/**
	 * Gets the prefix to be used when referring to this namespace
	 * @return The prefix to be used when referring to this namespace
	 */
	public String getPrefix() {
		return this.prefix;
	}
	
	/**
	 * Gets the uri uniquely identifying the namespace
	 * @return The uri uniquely identifying the namespace
	 */
	public String getUri() {
		return this.uri;
	}
	
	/**
	 * Gets the line in which the namespace was defined
	 * @return The line in which the namespace was defined
	 */
	public int getLine() {
		return this.line;
	}

}
