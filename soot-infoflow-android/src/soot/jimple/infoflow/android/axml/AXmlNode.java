package soot.jimple.infoflow.android.axml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a node of an Android XML document.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class AXmlNode extends AXmlElement {
	/**
	 * The node's tag.
	 */
	protected String tag;
	
	/**
	 * The parent node.
	 */
	protected AXmlNode parent = null;
	
	/**
	 * List containing all children.
	 */
	ArrayList<AXmlNode> children = null;
	
	/**
	 * Map containing all attributes. The key matches the attribute's name.
	 */
	Map<String, AXmlAttribute<?>> attributes = null;
	
	/**
	 * Creates a new {@link AXmlNode} object with the given <code>tag</code>, <code>namespace</code> and <code>parent</code>.<br />
	 * Keep in mind that this node will automatically be added as child to the given parent node with <code>parent.addChild(this)</code>.
	 * If you want to create a root node you can set <code>parent</code> to null.<br />
	 * The <code>addded</code> flag is defaulted to true (see {@link AXmlElement#added}).
	 * 
	 * @param	tag		the node's tag.
	 * @param	ns		the node's namespace.
	 * @param	parent	the node's parent node.
	 */
	public AXmlNode(String tag, String ns, AXmlNode parent) {
		this(tag, ns, parent, true);
	}
	
	/**
	 * Creates a new {@link AXmlNode} object with the given <code>tag</code>, <code>namespace</code> and <code>parent</code>.
	 * Keep in mind that this node will automatically be added as child to the given parent node with <code>parent.addChild(this)</code>.
	 * 
	 * @param	tag		the node's tag.
	 * @param	ns		the node's namespace.
	 * @param	parent	the node's parent node.
	 * @param	added	wheter this node was part of a parsed xml file or added afterwards.
	 */
	public AXmlNode(String tag, String ns, AXmlNode parent, boolean added) {
		super(ns, added);
		this.tag = tag;
		this.parent = parent;
		if(parent != null) parent.addChild(this);
	}
	
	/**
	 * Returns the tag of this node.
	 * 
	 * @return	the node's tag
	 */
	public String getTag() {
		return tag;
	}
	
	/**
	 * Adds the given node as sibling <b>before</b> this node and returns true.<br />
	 * If this node has no parent the sibling cannot be added and false will be returned instead.
	 * 
	 * @param	sibling
	 * @return	false if this node has no parent, otherwise true
	 */
	public boolean addSiblingBefore(AXmlNode sibling) {
		if(this.parent != null) {
			this.parent.addChild(sibling, this.parent.getChildren().indexOf(this));
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds the given node as sibling <b>after</b> this node and returns true.<br />
	 * If this node has no parent the sibling cannot be added and false will be returned instead.
	 * 
	 * @param	sibling
	 * @return	false if this node has no parent, otherwise true
	 */
	public boolean addSiblingAfter(AXmlNode sibling) {
		if(this.parent != null) {
			this.parent.addChild(sibling, this.parent.getChildren().indexOf(this) + 1);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds the given node as child.
	 * 
	 * @param	child	a new child for this node
	 * @return	this node itself for method chaining
	 */
	public AXmlNode addChild(AXmlNode child) {
		if (this.children == null)
			this.children = new ArrayList<AXmlNode>();
		this.children.add(child);
		return this;
	}
	
	/**
	 * Adds the given node as child at position index.
	 * 
	 * 
	 * @param	child	a new child for this node
	 * @return	this node itself for method chaining
	 * @throws	IndexOutOfBoundsException if the index is out of range (index < 0 || index > children.size())
	 */
	public AXmlNode addChild(AXmlNode child, int index) {
		if (this.children == null)
			this.children = new ArrayList<AXmlNode>();
		this.children.add(index, child);
		return this;
	}
	
	/**
	 * List containing all children of this node.
	 * 
	 * @return list with all children
	 */
	public List<AXmlNode> getChildren() {
		if (this.children == null)
			return Collections.emptyList();
		return new ArrayList<AXmlNode>(this.children);
	}
	
	/**
	 * List containing all children of this node which have the given <code>tag</code>.
	 * 
	 * @param	tag		the children's tag
	 * @return	list with all children with <code>tag</code>
	 */
	public List<AXmlNode> getChildrenWithTag(String tag) {
		if (this.children == null)
			return Collections.emptyList();
		
		ArrayList<AXmlNode> children = new ArrayList<AXmlNode>();
		for(AXmlNode child : this.children) {
			if(child.getTag().equals(tag))
				children.add(child);
		}
		
		return children;
	}
	
	/**
	 * Returns a map which contains all attributes. The keys match the attributes' names.
	 * 
	 * @return	map with all attributes belonging to this node
	 */
	public Map<String, AXmlAttribute<?>> getAttributes() {
		if (this.attributes == null)
			return Collections.emptyMap();
		return new HashMap<String, AXmlAttribute<?>>(this.attributes);
	}
	
	/**
	 * Returns whether this node has an attribute with the given <code>name</code>.
	 * 
	 * @param	name	the attribute's name
	 * @return	if this node has an attribute with <code>name</code>
	 */
	public boolean hasAttribute(String name) {
		if (this.attributes == null)
			return false;
		return this.attributes.containsKey(name);
	}
	
	/**
	 * Returns the attribute with the given <code>name</code>.
	 * 
	 * @param	name	the attribute's name.
	 * @return	attribute with <code>name</code>.
	 */
	public AXmlAttribute<?> getAttribute(String name) {
		if (this.attributes == null)
			return null;
		return this.attributes.get(name);
	}
	
	/**
	 * Adds the given attribute to this node. Attributes have unique names.
	 * An attribute with the same name will be overwritten. 
	 * 
	 * @param	attr	the attribute to be added.
	 */
	public void addAttribute(AXmlAttribute<?> attr) {
		if(attr == null)
			throw new NullPointerException("AXmlAttribute is null");
		
		if (this.attributes == null)
			this.attributes = new HashMap<String, AXmlAttribute<?>>();
		this.attributes.put(attr.getName(), attr);
	}
		
	/**
	 * Returns the parent node.<br />
	 * If this node is part of a valid XML document, consider
	 * that only the root node's parent can be and has to be null.
	 * 
	 * @return	parent node
	 */
	public AXmlNode getParent() {
		return this.parent;
	}
	
	/**
	 * Sets the parent of this node.
	 * 
	 * @param	parent	this node's new parent
	 */
	public void setParent(AXmlNode parent) {
		this.parent = parent;
	}
	
	@Override
	public String toString() {
		String attributes = "";
		if (this.attributes != null)
			for (AXmlAttribute<?> attrNode : this.attributes.values())
				attributes += " " + attrNode;
		return "<" + tag + attributes + ">";
	}

	/**
	 * Remove child 'child' of this node
	 * @param child
	 */
	public void removeChild(AXmlNode child) {
		children.remove(child);
	}
	
}
