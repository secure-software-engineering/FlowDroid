/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.data;

import java.util.Arrays;

import soot.Local;
import soot.NullType;
import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;

/**
 * This class represents the taint, containing a base value and a list of fields
 * (length is bounded by Infoflow.ACCESSPATHLENGTH)
 */
public class AccessPath implements Cloneable {

	private static AccessPath zeroAccessPath = null;

	public enum ArrayTaintType {
		Contents, Length, ContentsAndLength
	}

	// ATTENTION: This class *must* be immutable!
	/*
	 * tainted value, is not null for non-static values
	 */
	private final Local value;
	/**
	 * list of fields, either they are based on a concrete @value or they indicate a
	 * static field
	 */
	private final SootField[] fields;

	private final Type baseType;
	private final Type[] fieldTypes;

	private final boolean taintSubFields;
	private final boolean cutOffApproximation;
	private final ArrayTaintType arrayTaintType;

	private final boolean canHaveImmutableAliases;

	private int hashCode = 0;

	/**
	 * The empty access path denotes a code region depending on a tainted
	 * conditional. If a function is called inside the region, there is no tainted
	 * value inside the callee, but there is taint - modeled by the empty access
	 * path.
	 */
	private static final AccessPath emptyAccessPath = new AccessPath();

	private AccessPath() {
		this.value = null;
		this.fields = null;
		this.baseType = null;
		this.fieldTypes = null;
		this.taintSubFields = true;
		this.cutOffApproximation = false;
		this.arrayTaintType = ArrayTaintType.ContentsAndLength;
		this.canHaveImmutableAliases = false;
	}

	AccessPath(Local val, SootField[] appendingFields, Type valType, Type[] appendingFieldTypes, boolean taintSubFields,
			boolean isCutOffApproximation, ArrayTaintType arrayTaintType, boolean canHaveImmutableAliases) {
		this.value = val;
		this.fields = appendingFields;
		this.baseType = valType;
		this.fieldTypes = appendingFieldTypes;
		this.taintSubFields = taintSubFields;
		this.cutOffApproximation = isCutOffApproximation;
		this.arrayTaintType = arrayTaintType;
		this.canHaveImmutableAliases = canHaveImmutableAliases;
	}

	/**
	 * Checks whether the given value can be the base value value of an access path
	 * 
	 * @param val The value to check
	 * @return True if the given value can be the base value value of an access path
	 */
	public static boolean canContainValue(Value val) {
		if (val == null)
			return false;

		return val instanceof Local || val instanceof InstanceFieldRef || val instanceof StaticFieldRef
				|| val instanceof ArrayRef;
	}

	public Local getPlainValue() {
		return value;
	}

	public Value getCompleteValue() {
		SootField f = getFirstField();
		if (value == null) {
			if (f == null)
				return null;

			return Jimple.v().newStaticFieldRef(f.makeRef());
		} else {
			if (f == null)
				return value;
			return Jimple.v().newInstanceFieldRef(value, f.makeRef());
		}
	}

	public SootField getLastField() {
		if (fields == null || fields.length == 0)
			return null;
		return fields[fields.length - 1];
	}

	public Type getLastFieldType() {
		if (fieldTypes == null || fieldTypes.length == 0)
			return baseType;
		return fieldTypes[fieldTypes.length - 1];
	}

	public SootField getFirstField() {
		if (fields == null || fields.length == 0)
			return null;
		return fields[0];
	}

	/**
	 * Checks whether the first field of this access path matches the given field
	 * 
	 * @param field The field to check against
	 * @return True if this access path has a non-empty field list and the first
	 *         field matches the given one, otherwise false
	 */
	public boolean firstFieldMatches(SootField field) {
		if (fields == null || fields.length == 0)
			return false;
		if (field == fields[0])
			return true;
		return false;
	}

	public Type getFirstFieldType() {
		if (fieldTypes == null || fieldTypes.length == 0)
			return null;
		return fieldTypes[0];
	}

	public SootField[] getFields() {
		return fields;
	}

	public Type[] getFieldTypes() {
		return fieldTypes;
	}

	public int getFieldCount() {
		return fields == null ? 0 : fields.length;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());

		result = prime * result + ((fields == null) ? 0 : Arrays.hashCode(fields));
		result = prime * result + ((fieldTypes == null) ? 0 : Arrays.hashCode(fieldTypes));

		result = prime * result + (this.taintSubFields ? 1 : 0);
		result = prime * result + this.arrayTaintType.hashCode();
		result = prime * result + (this.canHaveImmutableAliases ? 1 : 0);
		this.hashCode = result;

		return this.hashCode;
	}

	public int getHashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this || super.equals(obj))
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		AccessPath other = (AccessPath) obj;

		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;

		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (baseType == null) {
			if (other.baseType != null)
				return false;
		} else if (!baseType.equals(other.baseType))
			return false;

		if (!Arrays.equals(fields, other.fields))
			return false;
		if (!Arrays.equals(fieldTypes, other.fieldTypes))
			return false;

		if (this.taintSubFields != other.taintSubFields)
			return false;
		if (this.arrayTaintType != other.arrayTaintType)
			return false;

		if (this.canHaveImmutableAliases != other.canHaveImmutableAliases)
			return false;

		assert this.hashCode() == obj.hashCode();
		return true;
	}

	public boolean isStaticFieldRef() {
		return value == null && fields != null && fields.length > 0;
	}

	public boolean isInstanceFieldRef() {
		return value != null && fields != null && fields.length > 0;
	}

	public boolean isFieldRef() {
		return fields != null && fields.length > 0;
	}

	public boolean isLocal() {
		return value != null && value instanceof Local && (fields == null || fields.length == 0);
	}

	@Override
	public String toString() {
		String str = "";
		if (value != null)
			str += value.toString() + "(" + value.getType() + ")";
		if (fields != null)
			for (int i = 0; i < fields.length; i++)
				if (fields[i] != null) {
					if (!str.isEmpty())
						str += " ";
					str += fields[i];
				}
		if (taintSubFields)
			str += " *";

		if (arrayTaintType == ArrayTaintType.ContentsAndLength)
			str += " <+length>";
		else if (arrayTaintType == ArrayTaintType.Length)
			str += " <length>";

		return str;
	}

	@Override
	public AccessPath clone() {
		// The empty access path is a singleton
		if (this == emptyAccessPath)
			return this;

		AccessPath a = new AccessPath(value, fields, baseType, fieldTypes, taintSubFields, cutOffApproximation,
				arrayTaintType, canHaveImmutableAliases);
		assert a.equals(this);
		return a;
	}

	public static AccessPath getEmptyAccessPath() {
		return emptyAccessPath;
	}

	public boolean isEmpty() {
		return value == null && (fields == null || fields.length == 0);
	}

	/**
	 * Checks whether this access path entails the given one, i.e. refers to all
	 * objects the other access path also refers to.
	 * 
	 * @param a2 The other access path
	 * @return True if this access path refers to all objects the other access path
	 *         also refers to
	 */
	public boolean entails(AccessPath a2) {
		if (this.isEmpty() || a2.isEmpty())
			return false;

		// If one of the access paths refers to an instance object and the other
		// one doesn't, there can't be an entailment
		if ((this.value != null && a2.value == null) || (this.value == null && a2.value != null))
			return false;

		// There cannot be an entailment for two instance references with
		// different base objects
		if (this.value != null && !this.value.equals(a2.value))
			return false;

		if (this.fields != null && a2.fields != null) {
			// If this access path is deeper than the other one, it cannot entail it
			if (this.fields.length > a2.fields.length)
				return false;

			// Check the fields in detail
			for (int i = 0; i < this.fields.length; i++)
				if (!this.fields[i].equals(a2.fields[i]))
					return false;
		}
		return true;
	}

	/**
	 * Gets a copy of this access path, but drops the last field. If this access
	 * path has no fields, the identity is returned.
	 * 
	 * @return A copy of this access path with the last field being dropped.
	 */
	public AccessPath dropLastField() {
		if (fields == null || fields.length == 0)
			return this;

		final SootField[] newFields;
		final Type[] newTypes;
		if (fields.length > 1) {
			newFields = new SootField[fields.length - 1];
			System.arraycopy(fields, 0, newFields, 0, fields.length - 1);

			newTypes = new Type[fields.length - 1];
			System.arraycopy(fieldTypes, 0, newTypes, 0, fields.length - 1);
		} else {
			newFields = null;
			newTypes = null;
		}
		return new AccessPath(value, newFields, baseType, newTypes, taintSubFields, cutOffApproximation, arrayTaintType,
				canHaveImmutableAliases);
	}

	/**
	 * Gets the type of the base value
	 * 
	 * @return The type of the base value
	 */
	public Type getBaseType() {
		return this.baseType;
	}

	/**
	 * Gets whether sub-fields shall be tainted. If this access path is e.g. a.b.*,
	 * the result is true, whereas it is false for a.b.
	 * 
	 * @return True if this access path includes all objects rechable through it,
	 *         otherwise false
	 */
	public boolean getTaintSubFields() {
		return this.taintSubFields;
	}

	/**
	 * Gets whether this access path has been (transitively) constructed from one
	 * which was cut off by the access path length limitation. If this is the case,
	 * this AP might not be precise.
	 * 
	 * @return True if this access path was constructed from a cut-off one.
	 */
	public boolean isCutOffApproximation() {
		return this.cutOffApproximation;
	}

	/**
	 * Gets whether this access path references only the length of the array to
	 * which it points, not the contents of that array
	 * 
	 * @return True if this access paths points refers to the length of an array
	 *         instead of to its contents
	 */
	public ArrayTaintType getArrayTaintType() {
		return this.arrayTaintType;
	}

	/**
	 * Checks whether this access path starts with the given value
	 * 
	 * @param val The value that is a potential prefix of the current access path
	 * @return True if this access paths with the given value (i.e., the given value
	 *         is a prefix of this access path), otherwise false
	 */
	public boolean startsWith(Value val) {
		// Filter out constants, etc.
		if (!canContainValue(val))
			return false;

		// Check the different types of values we can have
		if (val instanceof Local && this.value == val)
			return true;
		else if (val instanceof StaticFieldRef)
			return this.value == null && this.fields != null && this.fields.length > 0
					&& this.fields[0] == ((StaticFieldRef) val).getField();
		else if (val instanceof InstanceFieldRef) {
			InstanceFieldRef iref = (InstanceFieldRef) val;
			return this.value == iref.getBase() && this.fields != null && this.fields.length > 0
					&& this.fields[0] == iref.getField();
		} else
			// Some unsupported value type
			return false;
	}

	/**
	 * Returns whether the tainted object can have immutable aliases.
	 * 
	 * @return true if the tainted object can have immutable aliases.
	 */
	public boolean getCanHaveImmutableAliases() {
		return canHaveImmutableAliases;
	}

	/**
	 * Creates the access path that is used in the zero abstraction
	 * 
	 * @return The access path that is used in the zero abstraction
	 */
	static AccessPath getZeroAccessPath() {
		if (zeroAccessPath == null)
			zeroAccessPath = new AccessPath(Jimple.v().newLocal("zero", NullType.v()), null, NullType.v(), null, false,
					false, ArrayTaintType.ContentsAndLength, false);
		return zeroAccessPath;
	}

}
