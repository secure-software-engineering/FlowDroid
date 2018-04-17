package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper to save an AccessPath with the information about sink and sources.
 * 
 * @author Joern Tillmanns
 * @author Steven Arzt
 */
public class AccessPathTuple {

	private final String baseType;
	private final String[] fields;
	private final String[] fieldTypes;
	private final SourceSinkType sinkSource;
	private String description;

	private int hashCode = 0;

	private static AccessPathTuple SOURCE_TUPLE;
	private static AccessPathTuple SINK_TUPLE;

	AccessPathTuple(String[] fields, String[] fieldTypes, SourceSinkType sinkSource) {
		this(null, fields, fieldTypes, sinkSource);
	}

	AccessPathTuple(String baseType, String[] fields, String[] fieldTypes, SourceSinkType sinkSource) {
		this.baseType = baseType;
		this.fields = fields;
		this.fieldTypes = fieldTypes;
		this.sinkSource = sinkSource;
	}

	/**
	 * Simplified factory method for creating an access path that just denotes the
	 * base object
	 * 
	 * @param isSource
	 *            True if the referenced access path shall be considered a data flow
	 *            source
	 * @param isSink
	 *            True if the referenced access path shall be considered a data flow
	 *            sink
	 * @return The newly created access path object
	 */
	public static AccessPathTuple create(boolean isSource, boolean isSink) {
		return fromPathElements((String[]) null, (String[]) null, isSource, isSink);
	}

	public static AccessPathTuple fromPathElements(List<String> fields, List<String> fieldTypes, boolean isSource,
			boolean isSink) {
		String[] fieldArray = fields == null || fields.isEmpty() ? null : fields.toArray(new String[fields.size()]);
		String[] fieldTypeArray = fieldTypes == null || fieldTypes.isEmpty() ? null
				: fieldTypes.toArray(new String[fieldTypes.size()]);
		return fromPathElements(fieldArray, fieldTypeArray, isSource, isSink);
	}

	public static AccessPathTuple fromPathElements(List<String> fields, List<String> fieldTypes,
			SourceSinkType sourceSinkType) {
		return fromPathElements(null, fields, fieldTypes, sourceSinkType);
	}

	public static AccessPathTuple fromPathElements(String field, String fieldType, SourceSinkType sourceSinkType) {
		return fromPathElements(null, Collections.singletonList(field), Collections.singletonList(fieldType),
				sourceSinkType);
	}

	public static AccessPathTuple fromPathElements(String baseType, List<String> fields, List<String> fieldTypes,
			SourceSinkType sourceSinkType) {
		String[] fieldArray = fields == null || fields.isEmpty() ? null : fields.toArray(new String[fields.size()]);
		String[] fieldTypeArray = fieldTypes == null || fieldTypes.isEmpty() ? null
				: fieldTypes.toArray(new String[fieldTypes.size()]);
		return new AccessPathTuple(baseType, fieldArray, fieldTypeArray, sourceSinkType);
	}

	public static AccessPathTuple fromPathElements(String[] fields, String[] fieldTypes, boolean isSource,
			boolean isSink) {
		return new AccessPathTuple(fields, fieldTypes, SourceSinkType.fromFlags(isSink, isSource));
	}

	public String getBaseType() {
		return this.baseType;
	}

	public String[] getFields() {
		return this.fields;
	}

	public String[] getFieldTypes() {
		return this.fieldTypes;
	}

	public SourceSinkType getSourceSinkType() {
		return this.sinkSource;
	}

	/**
	 * Gets the shared tuple that denoted just the base object as a source
	 * 
	 * @return The tuple that denotes just the base object as a source
	 */
	public static AccessPathTuple getBlankSourceTuple() {
		if (SOURCE_TUPLE == null)
			SOURCE_TUPLE = create(true, false);
		return SOURCE_TUPLE;
	}

	/**
	 * Gets the shared tuple that denoted just the base object as a sink
	 * 
	 * @return The tuple that denotes just the base object as a sink
	 */
	public static AccessPathTuple getBlankSinkTuple() {
		if (SINK_TUPLE == null)
			SINK_TUPLE = create(false, true);
		return SINK_TUPLE;
	}

	/**
	 * Gets the description of this access path
	 * 
	 * @return The description of this access path
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this access path
	 * 
	 * @param description
	 *            The description of this access path
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + Arrays.hashCode(fieldTypes);
		result = prime * result + Arrays.hashCode(fields);
		result = prime * result + ((sinkSource == null) ? 0 : sinkSource.hashCode());
		hashCode = result;
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
		AccessPathTuple other = (AccessPathTuple) obj;
		if (baseType == null) {
			if (other.baseType != null)
				return false;
		} else if (!baseType.equals(other.baseType))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (!Arrays.equals(fieldTypes, other.fieldTypes))
			return false;
		if (!Arrays.equals(fields, other.fields))
			return false;
		if (sinkSource != other.sinkSource)
			return false;
		return true;
	}

	/**
	 * Checks whether this tuple is equivalent to one of the simple predefined ones.
	 * If so, it returns the shared predefined object. Otherwise, it returns this
	 * object.
	 * 
	 * @return A shared object that is equal to this one if possible, otherwise this
	 *         object
	 */
	public AccessPathTuple simplify() {
		AccessPathTuple blankSource = getBlankSourceTuple();
		AccessPathTuple blankSink = getBlankSinkTuple();
		if (this.equals(blankSource))
			return blankSource;
		else if (this.equals(blankSink))
			return blankSink;
		else
			return this;
	}

}
