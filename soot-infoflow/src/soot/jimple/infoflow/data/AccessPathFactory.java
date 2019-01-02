package soot.jimple.infoflow.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Local;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.AccessPathConfiguration;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.util.TypeUtils;

public class AccessPathFactory {

	protected final static Logger logger = LoggerFactory.getLogger(AccessPathFactory.class);

	private final InfoflowConfiguration config;

	/**
	 * Specialized pair class for field bases
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class BasePair {

		private final SootField[] fields;
		private final Type[] types;
		private int hashCode = 0;

		private BasePair(SootField[] fields, Type[] types) {
			this.fields = fields;
			this.types = types;

			// Check whether this base makes sense
			if (fields == null || fields.length == 0)
				throw new RuntimeException("A base must contain at least one field");
		}

		public SootField[] getFields() {
			return this.fields;
		}

		public Type[] getTypes() {
			return this.types;
		}

		@Override
		public int hashCode() {
			if (hashCode == 0) {
				final int prime = 31;
				int result = 1;
				result = prime * result + Arrays.hashCode(fields);
				result = prime * result + Arrays.hashCode(types);
				hashCode = result;
			}
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BasePair other = (BasePair) obj;
			if (!Arrays.equals(fields, other.fields))
				return false;
			if (!Arrays.equals(types, other.types))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return Arrays.toString(fields);
		}

	}

	/**
	 * Creates a new instance of the {@link AccessPathFactory} class
	 * 
	 * @param config The FlowDroid configuration object
	 */
	public AccessPathFactory(InfoflowConfiguration config) {
		this.config = config;
	}

	private MyConcurrentHashMap<Type, Set<BasePair>> baseRegister = new MyConcurrentHashMap<Type, Set<BasePair>>();

	public AccessPath createAccessPath(Value val, boolean taintSubFields) {
		return createAccessPath(val, (SootField[]) null, null, (Type[]) null, taintSubFields, false, true,
				ArrayTaintType.ContentsAndLength);
	}

	public AccessPath createAccessPath(Value val, Type valType, boolean taintSubFields, ArrayTaintType arrayTaintType) {
		return createAccessPath(val, null, valType, null, taintSubFields, false, true, arrayTaintType);
	}

	public AccessPath createAccessPath(Value val, SootField[] appendingFields, boolean taintSubFields) {
		return createAccessPath(val, appendingFields, null, null, taintSubFields, false, true,
				ArrayTaintType.ContentsAndLength);
	}

	public AccessPath createAccessPath(Value val, SootField[] appendingFields, Type valType, Type[] appendingFieldTypes,
			boolean taintSubFields, boolean cutFirstField, boolean reduceBases, ArrayTaintType arrayTaintType) {
		return createAccessPath(val, appendingFields, valType, appendingFieldTypes, taintSubFields, cutFirstField,
				reduceBases, arrayTaintType, false);
	}

	public AccessPath createAccessPath(Value val, SootField[] appendingFields, Type valType, Type[] appendingFieldTypes,
			boolean taintSubFields, boolean cutFirstField, boolean reduceBases, ArrayTaintType arrayTaintType,
			boolean canHaveImmutableAliases) {
		// Make sure that the base object is valid
		assert (val == null && appendingFields != null && appendingFields.length > 0)
				|| AccessPath.canContainValue(val);
		final AccessPathConfiguration accessPathConfig = config.getAccessPathConfiguration();

		// Do we track types?
		if (!config.getEnableTypeChecking()) {
			valType = null;
			appendingFieldTypes = null;
		}

		// Initialize the field type information if necessary
		if (appendingFields != null && appendingFieldTypes == null) {
			appendingFieldTypes = new Type[appendingFields.length];
			for (int i = 0; i < appendingFields.length; i++)
				appendingFieldTypes[i] = appendingFields[i].getType();
		}

		Local value;
		Type baseType;
		SootField[] fields;
		Type[] fieldTypes;
		boolean cutOffApproximation;

		// Get the base object, field and type
		if (val instanceof FieldRef) {
			FieldRef ref = (FieldRef) val;

			// Set the base value and type if we have one
			if (val instanceof InstanceFieldRef) {
				InstanceFieldRef iref = (InstanceFieldRef) val;
				value = (Local) iref.getBase();
				baseType = value.getType();
			} else {
				value = null;
				baseType = null;
			}

			// Handle the fields
			fields = new SootField[(appendingFields == null ? 0 : appendingFields.length) + 1];
			fields[0] = ref.getField();
			if (appendingFields != null)
				System.arraycopy(appendingFields, 0, fields, 1, appendingFields.length);

			fieldTypes = new Type[(appendingFieldTypes == null ? 0 : appendingFieldTypes.length) + 1];
			fieldTypes[0] = valType != null ? valType : fields[0].getType();
			if (appendingFieldTypes != null)
				System.arraycopy(appendingFieldTypes, 0, fieldTypes, 1, appendingFieldTypes.length);
		} else if (val instanceof ArrayRef) {
			ArrayRef ref = (ArrayRef) val;
			value = (Local) ref.getBase();
			baseType = valType == null ? value.getType() : valType;

			// Copy the arrays to not destroy other APs
			fields = appendingFields == null ? null : Arrays.copyOf(appendingFields, appendingFields.length);
			fieldTypes = appendingFieldTypes == null ? null
					: Arrays.copyOf(appendingFieldTypes, appendingFieldTypes.length);
		} else {
			value = (Local) val;
			baseType = valType == null ? (value == null ? null : value.getType()) : valType;

			// Copy the arrays to not destroy other APs
			fields = appendingFields == null ? null : Arrays.copyOf(appendingFields, appendingFields.length);
			fieldTypes = appendingFieldTypes == null ? null
					: Arrays.copyOf(appendingFieldTypes, appendingFieldTypes.length);
		}

		// If we don't want to track fields at all, we can cut the field
		// processing short
		if (accessPathConfig.getAccessPathLength() == 0) {
			fields = null;
			fieldTypes = null;
		}

		// Cut the first field if requested
		if (cutFirstField && fields != null && fields.length > 0) {
			SootField[] newFields = new SootField[fields.length - 1];
			Type[] newTypes = new Type[newFields.length];
			System.arraycopy(fields, 1, newFields, 0, newFields.length);
			System.arraycopy(fieldTypes, 1, newTypes, 0, newTypes.length);
			fields = newFields.length > 0 ? newFields : null;
			fieldTypes = newTypes.length > 0 ? newTypes : null;
		}

		// If we have a chain of fields that reduces to itself, we can throw
		// away the recursion. Example:
		// <java.lang.Thread: java.lang.ThreadGroup group>
		// <java.lang.ThreadGroup: java.lang.Thread[] threads>
		// <java.lang.Thread: java.lang.ThreadGroup group>
		// <java.lang.ThreadGroup: java.lang.Thread[] threads> *
		if (config.getAccessPathConfiguration().getUseSameFieldReduction() && fields != null && fields.length > 1) {
			for (int bucketStart = fields.length - 2; bucketStart >= 0; bucketStart--) {
				// Check if we have a repeating field
				int repeatPos = -1;
				for (int i = bucketStart + 1; i < fields.length; i++)
					if (fields[i] == fields[bucketStart]) {
						repeatPos = i;
						break;
					}
				int repeatLen = repeatPos - bucketStart;
				if (repeatPos < 0)
					continue;

				// Check that everything between bucketStart and repeatPos
				// really repeats after bucketStart
				boolean matches = true;
				for (int i = 0; i < repeatPos - bucketStart; i++)
					matches &= (repeatPos + i < fields.length) && fields[bucketStart + i] == fields[repeatPos + i];
				if (matches) {
					SootField[] newFields = new SootField[fields.length - repeatLen];
					Type[] newTypes = new Type[fields.length - repeatLen];

					System.arraycopy(fields, 0, newFields, 0, bucketStart + 1);
					System.arraycopy(fields, repeatPos + 1, newFields, bucketStart + 1, fields.length - repeatPos - 1);
					fields = newFields;

					System.arraycopy(fieldTypes, 0, newTypes, 0, bucketStart + 1);
					System.arraycopy(fieldTypes, repeatPos + 1, newTypes, bucketStart + 1,
							fieldTypes.length - repeatPos - 1);
					fieldTypes = newTypes;

					break;
				}
			}
		}

		// Make sure that the actual types are always as precise as the declared
		// ones. If types become incompatible, we drop the whole access path.
		if (config.getEnableTypeChecking()) {
			if (value != null && value.getType() != baseType) {
				baseType = TypeUtils.getMorePreciseType(baseType, value.getType());
				if (baseType == null)
					return null;

				// If we have a more precise base type in the first field, we
				// take that
				if (fields != null && fields.length > 0 && !(baseType instanceof ArrayType))
					baseType = TypeUtils.getMorePreciseType(baseType, fields[0].getDeclaringClass().getType());
				if (baseType == null)
					return null;
			}
			if (fields != null && fieldTypes != null)
				for (int i = 0; i < fields.length; i++) {
					fieldTypes[i] = TypeUtils.getMorePreciseType(fieldTypes[i], fields[i].getType());
					if (fieldTypes[i] == null)
						return null;

					// If we have a more precise base type in the next field, we
					// take that
					if (fields.length > i + 1 && !(fieldTypes[i] instanceof ArrayType))
						fieldTypes[i] = TypeUtils.getMorePreciseType(fieldTypes[i],
								fields[i + 1].getDeclaringClass().getType());
					if (fieldTypes[i] == null)
						return null;
				}
		}

		// Make sure that only heap objects may have fields. Primitive arrays
		// with fields may occur on impossible type casts in the target program.
		if (value != null && value.getType() instanceof ArrayType) {
			ArrayType at = (ArrayType) value.getType();
			if (!(at.getArrayElementType() instanceof RefLikeType) && fields != null && fields.length > 0)
				return null;
		}

		// We can always merge a.inner.this$0.c to a.c. We do this first so that
		// we don't create recursive bases for stuff we don't need anyway.
		if (accessPathConfig.getUseThisChainReduction() && reduceBases && fields != null) {
			for (int i = 0; i < fields.length; i++) {
				// Is this a reference to an outer class?
				if (fields[i].getName().startsWith("this$")) {
					// Get the name of the outer class
					String outerClassName = ((RefType) fields[i].getType()).getClassName();

					// Check the base object
					int startIdx = -1;
					if (value != null && value.getType() instanceof RefType
							&& ((RefType) value.getType()).getClassName().equals(outerClassName)) {
						startIdx = 0;
					} else {
						// Scan forward to find the same reference
						for (int j = 0; j < i; j++)
							if (fields[j].getType() instanceof RefType
									&& ((RefType) fields[j].getType()).getClassName().equals(outerClassName)) {
								startIdx = j;
								break;
							}
					}

					if (startIdx >= 0) {
						SootField[] newFields = new SootField[fields.length - (i - startIdx) - 1];
						Type[] newFieldTypes = new Type[fieldTypes.length - (i - startIdx) - 1];

						System.arraycopy(fields, 0, newFields, 0, startIdx);
						System.arraycopy(fieldTypes, 0, newFieldTypes, 0, startIdx);

						System.arraycopy(fields, i + 1, newFields, startIdx, fields.length - i - 1);
						System.arraycopy(fieldTypes, i + 1, newFieldTypes, startIdx, fieldTypes.length - i - 1);

						fields = newFields;
						fieldTypes = newFieldTypes;
						break;
					}
				}
			}
		}

		// Check for recursive data structures. If a last field maps back to
		// something we already know, we build a repeatable component from it
		boolean recursiveCutOff = false;
		if (accessPathConfig.getUseRecursiveAccessPaths() && reduceBases && fields != null) {
			// f0...fi references an object of type T, look for an extension f0...fi...fj
			// that also references an object of type T
			int ei = val instanceof StaticFieldRef ? 1 : 0;
			while (ei < fields.length) {
				final Type eiType = ei == 0 ? baseType : fieldTypes[ei - 1];
				int ej = ei;
				while (ej < fields.length) {
					if (fieldTypes[ej] == eiType || fields[ej].getType() == eiType) {
						// The types match, f0...fi...fj maps back to an object of the same type as
						// f0...fi. We must thus convert the access path to f0...fi-1[...fj]fj+1
						SootField[] newFields = new SootField[fields.length - (ej - ei) - 1];
						Type[] newTypes = new Type[newFields.length];

						System.arraycopy(fields, 0, newFields, 0, ei);
						System.arraycopy(fieldTypes, 0, newTypes, 0, ei);

						if (fields.length > ej) {
							System.arraycopy(fields, ej + 1, newFields, ei, fields.length - ej - 1);
							System.arraycopy(fieldTypes, ej + 1, newTypes, ei, fieldTypes.length - ej - 1);
						}

						// Register the base
						SootField[] base = new SootField[ej - ei + 1];
						Type[] baseTypes = new Type[ej - ei + 1];
						System.arraycopy(fields, ei, base, 0, base.length);
						System.arraycopy(fieldTypes, ei, baseTypes, 0, base.length);
						registerBase(eiType, base, baseTypes);

						fields = newFields;
						fieldTypes = newTypes;
						recursiveCutOff = true;
					} else
						ej++;
				}
				ei++;
			}
		}

		// Cut the fields at the maximum access path length. If this happens,
		// we must always add a star
		if (fields != null) {
			final int maxAccessPathLength = accessPathConfig.getAccessPathLength();
			if (maxAccessPathLength >= 0) {
				int fieldNum = Math.min(maxAccessPathLength, fields.length);
				if (fields.length > fieldNum) {
					taintSubFields = true;
					cutOffApproximation = true;
				} else {
					cutOffApproximation = recursiveCutOff;
				}

				if (fieldNum == 0) {
					fields = null;
					fieldTypes = null;
				} else {
					SootField[] newFields = new SootField[fieldNum];
					Type[] newFieldTypes = new Type[fieldNum];

					System.arraycopy(fields, 0, newFields, 0, fieldNum);
					System.arraycopy(fieldTypes, 0, newFieldTypes, 0, fieldNum);

					fields = newFields;
					fieldTypes = newFieldTypes;
				}
			} else
				cutOffApproximation = recursiveCutOff;
		} else {
			cutOffApproximation = false;
			fields = null;
			fieldTypes = null;
		}

		// Type checks
		assert value == null || !(!(baseType instanceof ArrayType) && !TypeUtils.isObjectLikeType(baseType)
				&& value.getType() instanceof ArrayType);
		assert value == null || !(baseType instanceof ArrayType && !(value.getType() instanceof ArrayType)
				&& !TypeUtils.isObjectLikeType(value.getType())) : "Type mismatch. Type was " + baseType
						+ ", value was: " + (value == null ? null : value.getType());

		if ((fields == null && fieldTypes != null) || (fields != null && fieldTypes == null))
			throw new RuntimeException("When there are fields, there must be field types and vice versa");
		if (fields != null && fields.length != fieldTypes.length)
			throw new RuntimeException("Field and field type arrays must be of equal length");

		// Sanity check
		if (baseType instanceof PrimType) {
			if (fields != null) {
				logger.warn("Primitive types cannot have fields");
				return null;
			}
		}
		if (fields != null) {
			for (int i = 0; i < fields.length - 2; i++)
				if (fields[i].getType() instanceof PrimType) {
					logger.warn("Primitive types cannot have fields");
					return null;
				}
		}

		return new AccessPath(value, fields, baseType, fieldTypes, taintSubFields, cutOffApproximation, arrayTaintType,
				canHaveImmutableAliases);
	}

	private void registerBase(Type eiType, SootField[] base, Type[] baseTypes) {
		// Check whether we can further normalize the base
		assert base.length == baseTypes.length;
		for (int i = 0; i < base.length; i++)
			if (baseTypes[i] == eiType) {
				SootField[] newBase = new SootField[i + 1];
				Type[] newTypes = new Type[i + 1];

				System.arraycopy(base, 0, newBase, 0, i + 1);
				System.arraycopy(baseTypes, 0, newTypes, 0, i + 1);

				base = newBase;
				baseTypes = newTypes;
				break;
			}

		Set<BasePair> bases = baseRegister.putIfAbsentElseGet(eiType, new ConcurrentHashSet<BasePair>());
		bases.add(new BasePair(base, baseTypes));
	}

	public Collection<BasePair> getBaseForType(Type tp) {
		return baseRegister.get(tp);
	}

	/**
	 * Copies the given access path with a new base value, but retains the base type
	 * 
	 * @param original The original access path
	 * @param val      The new value
	 * @return The new access path with the exchanged value
	 */
	public AccessPath copyWithNewValue(AccessPath original, Value val) {
		return copyWithNewValue(original, val, original.getBaseType(), false);
	}

	/**
	 * value val gets new base, fields are preserved.
	 * 
	 * @param original The original access path
	 * @param val      The new base value
	 * @return This access path with the base replaced by the value given in the val
	 *         parameter
	 */
	public AccessPath copyWithNewValue(AccessPath original, Value val, Type newType, boolean cutFirstField) {
		return copyWithNewValue(original, val, newType, cutFirstField, true);
	}

	/**
	 * value val gets new base, fields are preserved.
	 * 
	 * @param original    The original access path
	 * @param val         The new base value
	 * @param reduceBases True if circular types shall be reduced to bases
	 * @return This access path with the base replaced by the value given in the val
	 *         parameter
	 */
	public AccessPath copyWithNewValue(AccessPath original, Value val, Type newType, boolean cutFirstField,
			boolean reduceBases) {
		return copyWithNewValue(original, val, newType, cutFirstField, reduceBases, original.getArrayTaintType());
	}

	/**
	 * value val gets new base, fields are preserved.
	 * 
	 * @param original       The original access path
	 * @param val            The new base value
	 * @param reduceBases    True if circular types shall be reduced to bases
	 * @param arrayTaintType The way a tainted array shall be handled
	 * @return This access path with the base replaced by the value given in the val
	 *         parameter
	 */
	public AccessPath copyWithNewValue(AccessPath original, Value val, Type newType, boolean cutFirstField,
			boolean reduceBases, ArrayTaintType arrayTaintType) {
		// If this copy would not add any new information, we can safely use the
		// old
		// object
		if (original.getPlainValue() != null && original.getPlainValue().equals(val)
				&& original.getBaseType().equals(newType) && original.getArrayTaintType() == arrayTaintType)
			return original;

		// Create the new access path
		AccessPath newAP = createAccessPath(val, original.getFields(), newType, original.getFieldTypes(),
				original.getTaintSubFields(), cutFirstField, reduceBases, arrayTaintType,
				original.getCanHaveImmutableAliases());

		// Again, check whether we can do without the new object
		if (newAP != null && newAP.equals(original))
			return original;
		else
			return newAP;
	}

	/**
	 * Merges the two given access paths , i.e., adds the fields of ap2 to ap1.
	 * 
	 * @param ap1 The access path to which to append the fields
	 * @param ap2 The access path whose fields to append to ap1
	 * @return The new access path
	 */
	public AccessPath merge(AccessPath ap1, AccessPath ap2) {
		return appendFields(ap1, ap2.getFields(), ap2.getFieldTypes(), ap2.getTaintSubFields());
	}

	/**
	 * Appends additional fields to the given access path
	 * 
	 * @param original       The original access path to which to append the fields
	 * @param apFields       The fields to append
	 * @param apFieldTypes   The types of the fields to append
	 * @param taintSubFields True if the new access path shall taint all objects
	 *                       reachable through it, false if it shall only point to
	 *                       precisely one object
	 * @return The new access path
	 */
	public AccessPath appendFields(AccessPath original, SootField[] apFields, Type[] apFieldTypes,
			boolean taintSubFields) {
		int offset = original.getFields() == null ? 0 : original.getFields().length;
		SootField[] fields = new SootField[offset + (apFields == null ? 0 : apFields.length)];
		Type[] fieldTypes = new Type[offset + (apFields == null ? 0 : apFields.length)];
		if (original.getFields() != null) {
			System.arraycopy(original.getFields(), 0, fields, 0, original.getFields().length);
			System.arraycopy(original.getFieldTypes(), 0, fieldTypes, 0, original.getFieldTypes().length);
		}
		if (apFields != null && apFields.length > 0) {
			System.arraycopy(apFields, 0, fields, offset, apFields.length);
			System.arraycopy(apFieldTypes, 0, fieldTypes, offset, apFieldTypes.length);
		}

		return createAccessPath(original.getPlainValue(), fields, original.getBaseType(), fieldTypes, taintSubFields,
				false, true, original.getArrayTaintType());
	}

}
