package soot.jimple.infoflow.android.axml;

import pxb.android.axml.AxmlVisitor;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.BooleanResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.IntegerResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;

/**
 * Represents an attribute of an Android XML node.
 * 
 * @param <T> determines the attribute's type. Currently {@link Integer},
 *            {@link Boolean} and {@link String} are supported.
 * @author Stefan Haas, Mario Schlipf
 * @author Steven Arzt
 */
public class AXmlAttribute<T> extends AXmlElement {

	/**
	 * The attribute's name.
	 */
	protected String name;

	/**
	 * The attribute's type
	 */
	protected int type;

	/**
	 * The attribute's value.
	 */
	protected T value;

	/**
	 * The attribute's resource id
	 */
	protected int resourceId;

	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>,
	 * <code>value</code> and <code>namespace</code>.<br />
	 * The <code>addded</code> flag is defaulted to true (see
	 * {@link AXmlElement#added}).
	 * 
	 * @param name  the attribute's name.
	 * @param value the attribute's value.
	 * @param ns    the attribute's namespace.
	 */
	public AXmlAttribute(String name, T value, String ns) {
		this(name, -1, value, ns, true);
	}

	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>,
	 * <code>value</code> and <code>namespace</code>.<br />
	 * The <code>addded</code> flag is defaulted to true (see
	 * {@link AXmlElement#added}).
	 * 
	 * @param name       the attribute's name.
	 * @param resourceId the attribute's resource id.
	 * @param value      the attribute's value.
	 * @param ns         the attribute's namespace.
	 */
	public AXmlAttribute(String name, int resourceId, T value, String ns) {
		this(name, resourceId, value, ns, true);
	}

	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>,
	 * <code>value</code> and <code>namespace</code>.
	 * 
	 * @param name       the attribute's name.
	 * @param resourceId the attribute's resource id.
	 * @param value      the attribute's value.
	 * @param ns         the attribute's namespace.
	 * @param added      wheter this attribute was part of a parsed xml file or
	 *                   added afterwards.
	 */
	public AXmlAttribute(String name, int resourceId, T value, String ns, boolean added) {
		this(name, resourceId, -1, value, ns, added);
	}

	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>,
	 * <code>type</code>, <code>value</code> and <code>namespace</code>.
	 * 
	 * @param name       the attribute's name.
	 * @param resourceId the attribute's resource id.
	 * @param type       the attribute's type
	 * @param value      the attribute's value.
	 * @param ns         the attribute's namespace.
	 * @param added      wheter this attribute was part of a parsed xml file or
	 *                   added afterwards.
	 */
	public AXmlAttribute(String name, int resourceId, int type, T value, String ns, boolean added) {
		super(ns, added);
		this.name = name;
		this.resourceId = resourceId;
		this.type = type;
		this.value = value;
	}

	/**
	 * Returns the name of this attribute.
	 * 
	 * @return the attribute's name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the resource id of this attribute.
	 * 
	 * @return the attribute's resource id.
	 */
	public int getResourceId() {
		return this.resourceId;
	}

	/**
	 * Sets the value for this attribute.
	 * 
	 * @param value the new value.
	 */
	public void setValue(T value) {
		this.value = value;
	}

	/**
	 * Returns the value of this attribute.
	 * 
	 * @return the attribute's value.
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * Returns an integer which identifies this attribute's type. Currently if
	 * AXmlAttribute is typed as {@link Integer} this will return
	 * {@link AxmlVisitor#TYPE_INT_HEX}, typed {@link Boolean} will result in
	 * {@link AxmlVisitor#TYPE_INT_BOOLEAN} and otherwise (even if not typed as
	 * {@link String}) this returns {@link AxmlVisitor#TYPE_STRING}.
	 * 
	 * @return integer representing the attribute's type
	 * @see AxmlVisitor#TYPE_INT_HEX
	 * @see AxmlVisitor#TYPE_INT_BOOLEAN
	 * @see AxmlVisitor#TYPE_STRING
	 */
	public int getType() {
		if (this.value instanceof Integer)
			return AXmlTypes.TYPE_INT_HEX;
		else if (this.value instanceof Boolean)
			return AXmlTypes.TYPE_INT_BOOLEAN;
		else
			return AXmlTypes.TYPE_STRING;
	}

	/**
	 * Returns an integer which identifies this attribute's type. This is the type
	 * originally passed to the constructor.
	 * 
	 * @return integer representing the attribute's type
	 * @see AxmlVisitor#TYPE_INT_HEX
	 * @see AxmlVisitor#TYPE_INT_BOOLEAN
	 * @see AxmlVisitor#TYPE_STRING
	 */
	public int getAttributeType() {
		return this.type;
	}

	@Override
	public String toString() {
		return this.name + "=\"" + this.value + "\"";
	}

	/**
	 * Gets this value as a Boolean. This function only looks at the immediate value
	 * and does not resolve references.
	 * 
	 * @return This value as a Boolean.
	 */
	public boolean asBoolean() {
		return !getValue().equals(Boolean.FALSE);
	}

	/**
	 * Gets this value as a Boolean. If this value is a reference to a resource, a
	 * lookup is performed.
	 * 
	 * @param arscParser The ARSC parser for resource lookups
	 * @return This value as a Boolean.
	 */
	public boolean asBoolean(ARSCFileParser arscParser) {
		if (type == AXmlTypes.TYPE_REFERENCE && value instanceof Integer) {
			AbstractResource res = arscParser.findResource((int) value);
			if (res instanceof BooleanResource)
				return ((BooleanResource) res).getValue();
			return false;
		} else
			return asBoolean();
	}

	/**
	 * Gets this value as a string. This function only looks at the immediate value
	 * and does not resolve references.
	 * 
	 * @return This value as a string
	 */
	public String asString() {
		return (String) value;
	}

	/**
	 * Gets this value as a string. If this value is a reference to a resource, a
	 * lookup is performed.
	 * 
	 * @param arscParser The ARSC parser for resource lookups
	 * @return This value as a string
	 */
	public String asString(ARSCFileParser arscParser) {
		if (type == AXmlTypes.TYPE_REFERENCE && value instanceof Integer) {
			AbstractResource res = arscParser.findResource((int) value);
			if (res instanceof StringResource)
				return ((StringResource) res).getValue();
			return null;
		} else
			return asString();
	}

	/**
	 * Gets this value as an integer value. This function only looks at the
	 * immediate value and does not resolve references.
	 * 
	 * @return This value as an integer value
	 */
	public int asInteger() {
		if (value instanceof Integer)
			return (int) value;

		// If the value has a weird data type, we stil try to parse it
		return value == null ? -1 : Integer.valueOf((String) value);
	}

	/**
	 * Gets this value as an integer value. If this value is a reference to a
	 * resource, a lookup is performed.
	 * 
	 * @param arscParser The ARSC parser for resource lookups
	 * @return This value as an integer value
	 */
	public int asInteger(ARSCFileParser arscParser) {
		if (type == AXmlTypes.TYPE_REFERENCE && value instanceof Integer) {
			AbstractResource res = arscParser.findResource((int) value);
			if (res instanceof IntegerResource)
				return ((IntegerResource) res).getValue();
			return -1;
		} else
			return asInteger();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + resourceId;
		result = prime * result + type;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AXmlAttribute<?> other = (AXmlAttribute<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (resourceId != other.resourceId)
			return false;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
