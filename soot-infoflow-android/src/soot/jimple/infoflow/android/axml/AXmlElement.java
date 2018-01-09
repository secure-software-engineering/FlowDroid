package soot.jimple.infoflow.android.axml;

/**
 * Basic class for all classes which represent elements from an <i>Android XML file</i>.
 * Provides functionality to either include or exclude the element from the XML.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public abstract class AXmlElement {
	/**
	 * The include flag determines whether this element will be written out in the document.
	 */
	protected boolean include = true;
	
	/**
	 * The added flag determines wheter this element was part of a parsed xml file or added afterwards.
	 */
	protected boolean added;
	
	/**
	 * The element's namespace.
	 */
	String ns;
	
	/**
	 * Constructor for a new Android XML Element.
	 * 
	 * @param	ns		namespace of this element.
	 * @param	added	wheter this element was part of a parsed xml file or added afterwards.
	 */
	public AXmlElement(String ns, boolean added) {
		this.ns = ns;
		this.added = added;
	}
	
	/**
	 * Returns the namespace of this element.
	 * 
	 * @return	the element's namespace
	 */
	public String getNamespace() {
		return this.ns;
	}
	
	/**
	 * Sets the namespace of this element.
	 * 
	 * @param	ns		the element's namespace
	 */
	public void setNamespace(String ns) {
		this.ns = ns;
	}
	
	/**
	 * The <code>added</code> flag determines wheter this element
	 * was part of a parsed xml file or added afterwards.
	 * 
	 * @param	added
	 */
	public void setAdded(boolean added) {
		this.added = added;
	}
	
	/**
	 * Returns wheter this element was part of a parsed xml file or added afterwards.
	 * 
	 * @return	<code>added</code> flag
	 */
	public boolean isAdded() {
		return this.added;
	}
	
	/**
	 * Returns the current value of the <code>include</code> flag.
	 * If true the manifest will contain information about this element.
	 * 
	 * @return	whether or not the element will be included in the manifest
	 */
	public boolean isIncluded() {
		return this.include;
	}
	
	/**
	 * Sets the <code>include</code> flag to true so that the manifest will contain information about this element.
	 */
	public void include() {
		this.include(true);
	}

	/**
	 * Sets the <code>include</code> flag to false so that the manifest will <i>not</i> contain information about this element.
	 */
	public void exclude() {
		this.include(false);
	}
	
	/**
	 * Sets the <code>include</code> flag to the passed boolean value.
	 * If given true this element will be included in the manifest.
	 * 
	 * @param	included	value for the <code>include</code> flag
	 */
	protected void include(boolean include) {
		this.include = include;
	}
}
