package soot.jimple.infoflow.results.xml;

import java.util.Arrays;

/**
 * Class representing an access path read from an external storage. Therefore,
 * this class cannot reference any Soot objects.
 * 
 * @author Steven Arzt
 *
 */
public class SerializedAccessPath {
	
	private final String base;
	private final String baseType;
	private final boolean taintSubFields;
	
	private final String[] fields;
	private final String[] types;
	
	/**
	 * Creates a new instance of the SerializedAccessPath class
	 * @param base The base variable
	 * @param baseType The type of the base variable
	 * @param taintSubFields Specifies whether fields following the specified
	 * ones shall also be considered as tainted
	 * @param fields The sequence of fields
	 * @param types The types of the fields
	 */
	SerializedAccessPath(String base, String baseType, boolean taintSubFields,
			String[] fields, String [] types) {
		this.base = base;
		this.baseType = baseType;
		this.taintSubFields = taintSubFields;
		this.fields = fields;
		this.types = types;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result
				+ ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + Arrays.hashCode(fields);
		result = prime * result + (taintSubFields ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(types);
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
		SerializedAccessPath other = (SerializedAccessPath) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (baseType == null) {
			if (other.baseType != null)
				return false;
		} else if (!baseType.equals(other.baseType))
			return false;
		if (!Arrays.equals(fields, other.fields))
			return false;
		if (taintSubFields != other.taintSubFields)
			return false;
		if (!Arrays.equals(types, other.types))
			return false;
		return true;
	}
	
	/**
	 * Gets the tainted base object
	 * @return The tainted base object
	 */
	public String getBase() {
		return this.base;
	}
	
	/**
	 * Gets the type of the tainted base object
	 * @return The type of the tainted base object
	 */
	public String getBaseType() {
		return this.baseType;
	}
	
	/**
	 * Gets whether fields following the specified ones shall also be
	 * considered as tainted
	 * @return True if sub-fields shall be considered as tainted, otherwise
	 * false
	 */
	public boolean getTaintSubFields() {
		return this.taintSubFields;
	}
	
	/**
	 * Gets the sequence of fields in this access path
	 * @return The sequence of fields in this access path
	 */
	public String[] getFields() {
		return this.fields;
	}
	
	/**
	 * Gets the types of the fields in this access path
	 * @return The types of the fields in this access path
	 */
	public String[] getTypes() {
		return this.types;
	}
	
}
