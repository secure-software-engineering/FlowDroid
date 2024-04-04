package soot.jimple.infoflow.data;

import java.util.Objects;

import soot.SootField;
import soot.Type;

/**
 * A fragment inside an access path
 * 
 * @author Steven Arzt
 *
 */
public class AccessPathFragment {

	private final SootField field;
	private final Type fieldType;
	private final ContextDefinition context;

	/**
	 * Creates a new {@link AccessPathFragment} without a context
	 * 
	 * @param field     The field that is dereferenced
	 * @param fieldType The propagated type of the field that is dereferenced
	 */
	public AccessPathFragment(SootField field, Type fieldType) {
		this.field = field;
		this.fieldType = fieldType == null ? field.getType() : fieldType;
		this.context = null;
	}

	/**
	 * Creates a new {@link AccessPathFragment} with a specific context
	 * 
	 * @param field     The field that is dereferenced
	 * @param fieldType The propagated type of the field that is dereferenced
	 * @param context   The context under which the access path is tainted
	 */
	public AccessPathFragment(SootField field, Type fieldType, ContextDefinition context) {
		this.field = field;
		this.fieldType = fieldType;
		this.context = context;
	}

	/**
	 * Gets the field referenced by this access path fragment
	 * 
	 * @return The field referenced by this access path fragment
	 */
	public SootField getField() {
		return field;
	}

	/**
	 * Gets the propagated type of the field referenced by this access path
	 * fragment. If no type information has been propagated, the declared type is
	 * used.
	 * 
	 * @return The propagated type of the field referenced by this access path
	 *         fragment
	 */
	public Type getFieldType() {
		return fieldType == null ? field.getType() : fieldType;
	}

	/**
	 * Gets the context in which the access path is tainted, or <code>null</code> if
	 * the access path is tainted in all contexts
	 * 
	 * @return The context in which the access path is tainted
	 */
	public ContextDefinition getContext() {
		return context;
	}

	@Override
	public String toString() {
		return field.toString();
	}

	/**
	 * Gets whether this access path fragment is valid
	 * 
	 * @return True if this access path fragment is valid, false otherwise
	 */
	public boolean isValid() {
		return fieldType != null;
	}

	/**
	 * Creates a sequence of access path fragments from an array of fields and field
	 * types
	 * 
	 * @param fields     A sequence of field dereferences
	 * @param fieldTypes The types of the fields in the sequence of dereferences
	 * @return The sequence of access path fragments
	 */
	public static AccessPathFragment[] createFragmentArray(SootField[] fields, Type[] fieldTypes) {
		if (fields == null || fields.length == 0)
			return null;
		AccessPathFragment fragments[] = new AccessPathFragment[fields.length];
		for (int i = 0; i < fields.length; i++)
			fragments[i] = new AccessPathFragment(fields[i], fieldTypes == null ? null : fieldTypes[i]);
		return fragments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, field, fieldType);
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
		return Objects.equals(context, other.context) && Objects.equals(field, other.field)
				&& Objects.equals(fieldType, other.fieldType);
	}

	/**
	 * Copies this access path fragment with a new propagated type. All other data
	 * is kept as-is.
	 * 
	 * @param newType The new type
	 * @return The new access path fragment
	 */
	public AccessPathFragment copyWithNewType(Type newType) {
		return new AccessPathFragment(field, newType, context);
	}

}
