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

import com.google.common.base.Joiner;
import soot.*;
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

	private final Local value;
	private final Type baseType;
	private final ContainerContext[] baseContext;

	private final AccessPathFragment[] fragments;

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
		this.baseType = null;
		this.baseContext = null;
		this.fragments = null;
		this.taintSubFields = true;
		this.cutOffApproximation = false;
		this.arrayTaintType = ArrayTaintType.ContentsAndLength;
		this.canHaveImmutableAliases = false;
	}

	AccessPath(Local val, SootField[] appendingFields, Type valType, Type[] appendingFieldTypes, boolean taintSubFields,
			boolean isCutOffApproximation, ArrayTaintType arrayTaintType, boolean canHaveImmutableAliases) {
		this(val, valType, null,
			 AccessPathFragment.createFragmentArray(appendingFields, appendingFieldTypes, null),
			 taintSubFields, isCutOffApproximation, arrayTaintType, canHaveImmutableAliases);
	}

	AccessPath(Local val, Type valType, AccessPathFragment[] fragments, boolean taintSubFields,
			boolean isCutOffApproximation, ArrayTaintType arrayTaintType, boolean canHaveImmutableAliases) {
		this(val, valType, null, fragments, taintSubFields, isCutOffApproximation, arrayTaintType, canHaveImmutableAliases);
	}

	AccessPath(Local val, Type valType, ContainerContext[] ctxt, AccessPathFragment[] fragments, boolean taintSubFields,
               boolean isCutOffApproximation, ArrayTaintType arrayTaintType, boolean canHaveImmutableAliases) {
		this.value = val;
		this.baseType = valType;
		this.baseContext = ctxt;
		this.fragments = fragments;
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

	/**
	 * Gets the last fragment in the sequence of field dereferences
	 * 
	 * @return The last fragment in the sequence of field dereferences
	 */
	public AccessPathFragment getLastFragment() {
		if (fragments == null || fragments.length == 0)
			return null;
		return fragments[fragments.length - 1];
	}

	/**
	 * Gets the first fragment in the sequence of field dereferences
	 * 
	 * @return The first fragment in the sequence of field dereferences
	 */
	public AccessPathFragment getFirstFragment() {
		if (fragments == null || fragments.length == 0)
			return null;
		return fragments[0];
	}

	/**
	 * Gets the first field in the sequence of field dereferences
	 * 
	 * @return The first field in the sequence of field dereferences
	 */
	public SootField getFirstField() {
		if (fragments == null || fragments.length == 0)
			return null;
		return fragments[0].getField();
	}

	/**
	 * Gets the last field in the sequence of field dereferences
	 * 
	 * @return The last field in the sequence of field dereferences
	 */
	public SootField getLastField() {
		if (fragments == null || fragments.length == 0)
			return null;
		return fragments[fragments.length - 1].getField();
	}

	/**
	 * Gets the type of the first field in the sequence of field dereferences
	 * 
	 * @return The type of the first field in the sequence of field dereferences
	 */
	public Type getFirstFieldType() {
		if (fragments == null || fragments.length == 0)
			return null;
		return fragments[0].getFieldType();
	}

	/**
	 * Gets the type of the last field in the sequence of field dereferences
	 * 
	 * @return The type of the last field in the sequence of field dereferences
	 */
	public Type getLastFieldType() {
		if (fragments == null || fragments.length == 0)
			return getBaseType();
		return fragments[fragments.length - 1].getFieldType();
	}

	/**
	 * Checks whether the first field of this access path matches the given field
	 * 
	 * @param field The field to check against
	 * @return True if this access path has a non-empty field list and the first
	 *         field matches the given one, otherwise false
	 */
	public boolean firstFieldMatches(SootField field) {
		if (fragments == null || fragments.length == 0)
			return false;
		return field == fragments[0].getField();
	}

	/**
	 * Gets the sequence of field dereferences in this access path
	 * 
	 * @return The sequence of field dereferences in this access path
	 */
	public AccessPathFragment[] getFragments() {
		return fragments;
	}

	/**
	 * Gets the number of field dereferences in this access path
	 * 
	 * @return The number of field dereferences in this access path
	 */
	public int getFragmentCount() {
		return fragments == null ? 0 : fragments.length;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());

		result = prime * result + ((fragments == null) ? 0 : Arrays.hashCode(fragments));

		result = prime * result + (this.taintSubFields ? 1 : 0);
		result = prime * result + this.arrayTaintType.hashCode();
		result = prime * result + (this.canHaveImmutableAliases ? 1 : 0);
		this.hashCode = result;

		return this.hashCode;
	}

	private int hashCodeWOContext = 0;
	public int hashCodeWithoutContext() {
		if (hashCodeWOContext != 0)
			return hashCodeWOContext;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((baseType == null) ? 0 : baseType.hashCode());

		if (fragments == null)
			result *= prime;
		else
			for (AccessPathFragment f : fragments)
				result = prime * result + (f == null ? 0 : f.hashCodeWithoutContext());

		result = prime * result + (this.taintSubFields ? 1 : 0);
		result = prime * result + this.arrayTaintType.hashCode();
		result = prime * result + (this.canHaveImmutableAliases ? 1 : 0);
		this.hashCodeWOContext = result;

		return this.hashCodeWOContext;
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

		if (!Arrays.equals(fragments, other.fragments))
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

	public boolean equalsWithoutContext(Object obj) {
		if (obj == this || super.equals(obj))
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		AccessPath other = (AccessPath) obj;

		if (this.hashCodeWOContext != 0 && other.hashCodeWOContext != 0 && this.hashCodeWOContext != other.hashCodeWOContext)
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

		if (this.taintSubFields != other.taintSubFields)
			return false;
		if (this.arrayTaintType != other.arrayTaintType)
			return false;

		if (this.canHaveImmutableAliases != other.canHaveImmutableAliases)
			return false;

		assert this.hashCodeWithoutContext() == other.hashCodeWithoutContext();
		return true;
	}

	public boolean isStaticFieldRef() {
		return value == null && fragments != null && fragments.length > 0;
	}

	public boolean isInstanceFieldRef() {
		return value != null && fragments != null && fragments.length > 0;
	}

	public boolean isFieldRef() {
		return fragments != null && fragments.length > 0;
	}

	public boolean isLocal() {
		return value != null && value instanceof Local && (fragments == null || fragments.length == 0);
	}

	@Override
	public String toString() {
		String str = "";
		if (value != null)
			str += value.toString() + "(" + baseType + ")";
		if (baseContext != null)
			str += "@[" + Joiner.on(",").join(baseContext) + "]";
		if (fragments != null && fragments.length > 0) {
			for (int i = 0; i < fragments.length; i++)
				if (fragments[i] != null) {
					if (!str.isEmpty())
						str += " ";
					str += fragments[i];
				}
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

		AccessPath a = new AccessPath(value, baseType, fragments, taintSubFields, cutOffApproximation, arrayTaintType,
				canHaveImmutableAliases);
		assert a.equals(this);
		return a;
	}

	public static AccessPath getEmptyAccessPath() {
		return emptyAccessPath;
	}

	/**
	 * Gets whether this access path is empty, i.e., references nothing
	 * 
	 * @return True if this access path is empty, false otherwise
	 */
	public boolean isEmpty() {
		return value == null && (fragments == null || fragments.length == 0);
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

		// If other taints all subfields but this one does not, this does not entail other
		if (!this.taintSubFields && a2.taintSubFields)
			return false;

		// This must at least taint everything of an array other taints
		if (this.arrayTaintType != ArrayTaintType.ContentsAndLength && this.arrayTaintType != a2.arrayTaintType)
			return false;

		if (this.fragments != null && a2.fragments != null) {
			// If this access path is deeper than the other one, it cannot entail it
			if (this.fragments.length > a2.fragments.length)
				return false;

			// Check the fields in detail
			for (int i = 0; i < this.fragments.length; i++) {
				if (!this.fragments[i].getField().equals(a2.fragments[i].getField()))
					return false;

				// Check that if this has a context, the context also entails the other context
				if (this.fragments[i].hasContext()) {
					if (!a2.fragments[i].hasContext())
						return false;

					ContainerContext[] ctxt1 = this.fragments[i].getContext();
					ContainerContext[] ctxt2 = a2.fragments[i].getContext();
					for (int j = 0; j < ctxt1.length; j++) {
						if (!ctxt1[j].entails(ctxt2[j]))
							return false;
					}
				}
			}
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
		if (fragments == null || fragments.length == 0)
			return this;

		final AccessPathFragment[] newFragments;
		if (fragments.length > 1) {
			int newLength = fragments.length - 1;
			newFragments = new AccessPathFragment[newLength];
			System.arraycopy(fragments, 0, newFragments, 0, newLength);
		} else
			newFragments = null;
		return new AccessPath(value, baseType, newFragments, taintSubFields, cutOffApproximation, arrayTaintType,
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
	 * Gets the type of the base value
	 *
	 * @return The type of the base value
	 */
	public ContainerContext[] getBaseContext() {
		return this.baseContext;
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
			return this.value == null && getFirstField() == ((StaticFieldRef) val).getField();
		else if (val instanceof InstanceFieldRef) {
			InstanceFieldRef iref = (InstanceFieldRef) val;
			return this.value == iref.getBase() && getFirstField() == iref.getField();
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
