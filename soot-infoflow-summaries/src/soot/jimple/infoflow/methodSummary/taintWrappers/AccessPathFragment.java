package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import soot.SootField;
import soot.Type;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * A portion of an access path
 * 
 * @author Steven Arzt
 *
 */
public class AccessPathFragment {

	private final String[] fields;
	private final String[] fieldTypes;
	private final ContainerContext[][] contexts;

	/**
	 * Creates a new instance of the {@link AccessPathFragment} class
	 * 
	 * @param fields     The names of the fields in this fragment of an access path
	 * @param fieldTypes The types of the given fields
	 */
	public AccessPathFragment(String[] fields, String[] fieldTypes) {
		this(fields, fieldTypes, new ContainerContext[fields == null ? 0 : fields.length][]);
	}

	/**
	 * Creates a new instance of the {@link AccessPathFragment} class
	 *
	 * @param fields     The names of the fields in this fragment of an access path
	 * @param fieldTypes The types of the given fields
	 * @param contexts   The contexts of the given fields
	 */
	public AccessPathFragment(String[] fields, String[] fieldTypes, ContainerContext[][] contexts) {
		this.fields = fields;
		this.fieldTypes = fieldTypes == null ? fieldsToTypes(fields) : fieldTypes;
		this.contexts = contexts;

		// Sanity check
		if (fields != null && fieldTypes != null && fields.length != fieldTypes.length)
			throw new RuntimeException("Access path array and type array must be of equal length");
		if (fields != null && fieldTypes != null && fields.length != contexts.length)
			throw new RuntimeException("Access path array and context array must be of equal length");
	}

	/**
	 * Creates a new instance of the {@link AccessPathFragment} class
	 * 
	 * @param fields     The fields in this fragment of an access path
	 * @param fieldTypes The types of the given fields
	 */
	public AccessPathFragment(SootField[] fields, Type[] fieldTypes, ContainerContext[][] contexts) {
		this(fieldArrayToStringArray(fields), typeArrayToStringArray(fieldTypes), contexts);
	}

	/**
	 * Creates a new instance of the {@link AccessPathFragment} class based on an
	 * existing access path
	 * 
	 * @param accessPath The original access path
	 */
	public AccessPathFragment(AccessPath accessPath) {
		this(accessPath.getFragmentCount() > 0 ? Arrays.stream(accessPath.getFragments())
				.map(f -> f.getField().toString()).collect(Collectors.toList()) : null,
				accessPath.getFragmentCount() > 0 ? Arrays.stream(accessPath.getFragments())
						.map(f -> f.getFieldType().toString()).collect(Collectors.toList()) : null,
				accessPath.getFragmentCount() > 0 ? Arrays.stream(accessPath.getFragments())
						.map(f -> f.getContext()).collect(Collectors.toList()) : null);
	}

	/**
	 * Creates a new instance of the {@link AccessPathFragment} class
	 * 
	 * @param fields     The fields in this fragment of an access path
	 * @param fieldTypes The types of the given fields
	 */
	public AccessPathFragment(List<String> fields, List<String> fieldTypes, List<ContainerContext[]> contexts) {
		this.fields = fields == null ? null : fields.toArray(new String[fields.size()]);
		this.fieldTypes = fieldTypes == null ? null : fieldTypes.toArray(new String[fieldTypes.size()]);
		this.contexts = contexts == null ? null : contexts.toArray(new ContainerContext[0][0]);
	}

	/**
	 * Converts an array of SootFields to an array of strings
	 * 
	 * @param fields The array of SootFields to convert
	 * @return The array of strings corresponding to the given array of SootFields
	 */
	private static String[] fieldArrayToStringArray(SootField[] fields) {
		if (fields == null)
			return null;
		String[] stringFields = new String[fields.length];
		for (int i = 0; i < fields.length; i++)
			stringFields[i] = fields[i].toString();
		return stringFields;
	}

	/**
	 * Converts an array of Soot Types to an array of strings
	 * 
	 * @param fields The array of Soot Types to convert
	 * @return The array of strings corresponding to the given array of Soot Types
	 */
	private static String[] typeArrayToStringArray(Type[] types) {
		if (types == null)
			return null;
		String[] stringTypes = new String[types.length];
		for (int i = 0; i < types.length; i++)
			stringTypes[i] = types[i].toString();
		return stringTypes;
	}

	private static String[] fieldsToTypes(String[] fields) {
		if (fields == null)
			return null;

		String[] types = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			String f = fields[i];
			types[i] = f.substring(f.indexOf(":") + 2, f.lastIndexOf(" "));
		}
		return types;
	}

	/**
	 * Gets the number of fields in this access path fragments
	 * 
	 * @return The length of this access path fragments
	 */
	public int length() {
		return fields == null ? 0 : fields.length;
	}

	/**
	 * Gets the names of the fields in this access path fragment
	 * 
	 * @return The names of the fields in this access path fragment
	 */
	public String[] getFields() {
		return fields;
	}

	/**
	 * Gets the types of the fields in this access path fragment
	 * 
	 * @return The types of the fields in this access path fragment
	 */
	public String[] getFieldTypes() {
		return fieldTypes;
	}

	public ContainerContext[][] getContexts() {
		return contexts;
	}

	/**
	 * Gets the name of the field at the given index
	 *
	 * @param idx The field index
	 * @return The name of the field at the given index
	 */
	public ContainerContext[] getContext(int idx) {
		if (contexts == null || idx < 0 || idx >= contexts.length)
			return null;
		return contexts[idx];
	}

	/**
	 * Gets the name of the last field in this access path fragment
	 * 
	 * @return The name of the last field in this access path fragment
	 */
	public String getLastFieldName() {
		if (fields == null || fields.length == 0)
			return null;
		return fields[fields.length - 1];
	}

	/**
	 * Gets the name of the first field in this access path fragment
	 * 
	 * @return The name of the first field in this access path fragment
	 */
	public String getFirstFieldName() {
		if (fields == null || fields.length == 0)
			return null;
		return fields[0];
	}

	/**
	 * Gets the type of the last field in this access path fragment
	 * 
	 * @return The type of the last field in this access path fragment
	 */
	public String getLastFieldType() {
		if (fieldTypes == null || fieldTypes.length == 0)
			return null;
		return fieldTypes[fieldTypes.length - 1];
	}

	/**
	 * Gets the type of the first field in this access path fragment
	 * 
	 * @return The type of the first field in this access path fragment
	 */
	public String getFirstFieldType() {
		if (fieldTypes == null || fieldTypes.length == 0)
			return null;
		return fieldTypes[0];
	}

	public ContainerContext[] getFirstFieldContext() {
		if (contexts == null || contexts.length == 0)
			return null;
		return contexts[0];
	}

	/**
	 * Gets whether this access path fragment is empty
	 * 
	 * @return true if this access path fragment is empty, false otherwise
	 */
	public boolean isEmpty() {
		return this.fields == null || this.fields.length == 0;
	}

	/**
	 * Append the given access path fragment to this one
	 * 
	 * @param toAppend The access path fragment to append to this one
	 * @return The concatenated access path fragment containing all elements from
	 *         this fragment followed by those from the given fragment
	 */
	public AccessPathFragment append(AccessPathFragment toAppend) {
		// If only one of the two operands contains actual data, we simply take that
		// object and don't need to append anything
		if (toAppend == null || toAppend.isEmpty()) {
			if (this.isEmpty())
				return null;
			return this;
		}
		if (this.isEmpty())
			return toAppend;

		String[] toAppendFields = toAppend.getFields();
		String[] toAppendFieldTypes = toAppend.getFieldTypes();
		ContainerContext[][] toAppendContexts = toAppend.getContexts();

		String[] appendedFields = new String[fields.length + toAppendFields.length];
		System.arraycopy(fields, 0, appendedFields, 0, fields.length);
		System.arraycopy(toAppendFields, 0, appendedFields, fields.length, toAppendFields.length);

		String[] appendedTypes = new String[fieldTypes.length + toAppendFieldTypes.length];
		System.arraycopy(fieldTypes, 0, appendedTypes, 0, fieldTypes.length);
		System.arraycopy(toAppendFieldTypes, 0, appendedTypes, fieldTypes.length, toAppendFieldTypes.length);

		ContainerContext[][] appendedContexts = new ContainerContext[contexts.length + toAppendContexts.length][];
		System.arraycopy(contexts, 0, appendedContexts, 0, contexts.length);
		System.arraycopy(toAppendContexts, 0, appendedContexts, contexts.length, toAppendContexts.length);

		return new AccessPathFragment(appendedFields, appendedTypes, appendedContexts);
	}

	/**
	 * Derives a new access path fragment by replacing the field type at the given
	 * index with the given type
	 * 
	 * @param idx       The index at which to change the field type
	 * @param fieldType The new field type
	 * @return The new access path fragment with the updated field type
	 */
	public AccessPathFragment updateFieldType(int idx, String fieldType) {
		String[] newFieldTypes = Arrays.copyOf(fieldTypes, fieldTypes.length);
		newFieldTypes[idx] = fieldType;
		return new AccessPathFragment(fields, newFieldTypes, contexts);
	}

	/**
	 * Gets the name of the field at the given index
	 * 
	 * @param idx The field index
	 * @return The name of the field at the given index
	 */
	public String getField(int idx) {
		if (idx < 0 || idx >= fields.length)
			return null;
		return fields[idx];
	}

	/**
	 * Gets the prefix of this access path with the given length
	 * 
	 * @param length The length to which this access path shall be cut
	 * @return This access path cut to the given length
	 */
	public AccessPathFragment prefix(int length) {
		if (length < 0)
			return this;
		if (length() <= length)
			return this;

		String[] newFields = new String[length];
		String[] newFieldTypes = new String[length];
		ContainerContext[][] newContexts = new ContainerContext[length][];

		System.arraycopy(fields, 0, newFields, 0, length);
		System.arraycopy(fieldTypes, 0, newFieldTypes, 0, length);
		System.arraycopy(fieldTypes, 0, newFieldTypes, 0, length);
		return new AccessPathFragment(newFields, newFieldTypes, newContexts);
	}

	@Override
	public String toString() {
		return fields == null ? "<null>" : Arrays.toString(fields);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fieldTypes);
		result = prime * result + Arrays.hashCode(fields);
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
		AccessPathFragment other = (AccessPathFragment) obj;
		if (!Arrays.equals(fieldTypes, other.fieldTypes))
			return false;
		if (!Arrays.equals(fields, other.fields))
			return false;
		return true;
	}

	/**
	 * Obtains the string representation of the given access path fragment
	 * 
	 * @param accessPath The access path fragment for which to get the string
	 *                   representation
	 * @return The string representation of the given access path fragment
	 */
	public static String toString(AccessPathFragment accessPath) {
		return accessPath.fields == null ? "" : Arrays.toString(accessPath.fields);
	}

	/**
	 * Appends the given suffix to the given access path
	 * 
	 * @param accessPath The base access path, which will become the first part of
	 *                   the result
	 * @param suffix     The access path to append, which will become the second
	 *                   part of the result
	 * @return The concatenated access path
	 */
	public static AccessPathFragment append(AccessPathFragment accessPath, AccessPathFragment suffix) {
		if (accessPath == null)
			return suffix;
		if (suffix == null)
			return accessPath;
		return accessPath.append(suffix);
	}

	public AccessPathFragment addContext(ContainerContext[] ctxt) {
		ContainerContext[][] contexts = new ContainerContext[fields.length][];;
		System.arraycopy(this.contexts, 1, contexts, 1, this.contexts.length - 1);
		contexts[0] = ctxt;
		return new AccessPathFragment(fields, fieldTypes, contexts);
	}
}
