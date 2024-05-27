package soot.jimple.infoflow.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;
import soot.ArrayType;
import soot.Local;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.AccessPathConfiguration;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.accessPaths.SameFieldReductionStrategy;
import soot.jimple.infoflow.data.accessPaths.This0ReductionStrategy;
import soot.jimple.infoflow.typing.TypeUtils;

public class AccessPathFactory {

	protected final static Logger logger = LoggerFactory.getLogger(AccessPathFactory.class);

	private final InfoflowConfiguration config;
	private final TypeUtils typeUtils;

	private final static SameFieldReductionStrategy SAME_FIELD_REDUCTION = new SameFieldReductionStrategy();
	private final static This0ReductionStrategy THIS0_REDUCTION = new This0ReductionStrategy();

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
	public AccessPathFactory(InfoflowConfiguration config, TypeUtils typeUtils) {
		this.config = config;
		this.typeUtils = typeUtils;
	}

	private MyConcurrentHashMap<Type, Set<AccessPathFragment[]>> baseRegister = new MyConcurrentHashMap<>();

	public AccessPath createAccessPath(Value val, boolean taintSubFields) {
		return createAccessPath(val, null, null, taintSubFields, false, true, ArrayTaintType.ContentsAndLength);
	}

	public AccessPath createAccessPath(Value val, Type valType, boolean taintSubFields, ArrayTaintType arrayTaintType) {
		return createAccessPath(val, valType, null, taintSubFields, false, true, arrayTaintType);
	}

	public AccessPath createAccessPath(Value val, SootField[] appendingFields, boolean taintSubFields) {
		return createAccessPath(val, null, AccessPathFragment.createFragmentArray(appendingFields, null, null),
				taintSubFields, false, true, ArrayTaintType.ContentsAndLength);
	}

	public AccessPath createAccessPath(Value val, AccessPathFragment[] appendingFragments, boolean taintSubFields) {
		return createAccessPath(val, null, appendingFragments, taintSubFields, false, true,
				ArrayTaintType.ContentsAndLength);
	}

	public AccessPath createAccessPath(Value val, Type valType, AccessPathFragment[] fragments, boolean taintSubFields,
			boolean cutFirstField, boolean reduceBases, ArrayTaintType arrayTaintType) {
		return createAccessPath(val, valType, fragments, taintSubFields, cutFirstField, reduceBases, arrayTaintType,
				false);
	}

	public AccessPath createAccessPath(Value val, Type valType, AccessPathFragment[] appendingFragments,
			boolean taintSubFields, boolean cutFirstField, boolean reduceBases, ArrayTaintType arrayTaintType,
			boolean canHaveImmutableAliases) {
		return createAccessPath(val, valType, null, appendingFragments, taintSubFields, cutFirstField, reduceBases,
				arrayTaintType, canHaveImmutableAliases);
	}

	public AccessPath createAccessPath(Value val, Type valType, ContainerContext[] ctxt,
			AccessPathFragment[] appendingFragments, boolean taintSubFields, boolean cutFirstField, boolean reduceBases,
			ArrayTaintType arrayTaintType, boolean canHaveImmutableAliases) {
		// Make sure that the base object is valid
		if (val != null && !AccessPath.canContainValue(val)) {
			logger.error("Access paths cannot be rooted in values of type {}", val.getClass().getName());
			return null;
		}

		// Make sure that we have data for an access path at all
		if (val == null && appendingFragments == null)
			return null;

		final AccessPathConfiguration accessPathConfig = config.getAccessPathConfiguration();

		// Do we track types?
		if (!config.getEnableTypeChecking())
			valType = null;

		Local value;
		Type baseType;
		AccessPathFragment[] fragments;
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
			fragments = new AccessPathFragment[(appendingFragments == null ? 0 : appendingFragments.length) + 1];
			fragments[0] = new AccessPathFragment(ref.getField(), null);
			if (appendingFragments != null)
				System.arraycopy(appendingFragments, 0, fragments, 1, appendingFragments.length);
		} else if (val instanceof ArrayRef) {
			ArrayRef ref = (ArrayRef) val;
			value = (Local) ref.getBase();
			baseType = valType == null ? value.getType() : valType;

			// Copy the arrays to not destroy other APs
			fragments = appendingFragments == null ? null
					: Arrays.copyOf(appendingFragments, appendingFragments.length);
		} else {
			value = (Local) val;
			baseType = valType == null ? (value == null ? null : value.getType()) : valType;

			// Copy the arrays to not destroy other APs
			fragments = appendingFragments == null ? null
					: Arrays.copyOf(appendingFragments, appendingFragments.length);
		}

		// If we don't want to track fields at all, we can cut the field
		// processing short
		if (accessPathConfig.getAccessPathLength() == 0)
			fragments = null;

		// Cut the first field if requested
		if (cutFirstField && fragments != null && fragments.length > 0) {
			AccessPathFragment[] newFragments = new AccessPathFragment[fragments.length - 1];
			System.arraycopy(fragments, 1, newFragments, 0, newFragments.length);
			fragments = newFragments.length > 0 ? newFragments : null;
		}

		// If we have a chain of fields that reduces to itself, we can throw
		// away the recursion. Example:
		// <java.lang.Thread: java.lang.ThreadGroup group>
		// <java.lang.ThreadGroup: java.lang.Thread[] threads>
		// <java.lang.Thread: java.lang.ThreadGroup group>
		// <java.lang.ThreadGroup: java.lang.Thread[] threads> *
		if (config.getAccessPathConfiguration().getUseSameFieldReduction() && fragments != null
				&& fragments.length > 1) {
			fragments = SAME_FIELD_REDUCTION.reduceAccessPath(value, fragments);
		}

		// Make sure that the actual types are always as precise as the declared
		// ones. If types become incompatible, we drop the whole access path.
		if (config.getEnableTypeChecking()) {
			if (value != null && value.getType() != baseType) {
				baseType = typeUtils.getMorePreciseType(baseType, value.getType());
				if (baseType == null)
					return null;

				// If we have a more precise base type in the first field, we
				// take that
				if (fragments != null && fragments.length > 0 && !(baseType instanceof ArrayType))
					baseType = typeUtils.getMorePreciseType(baseType,
							fragments[0].getField().getDeclaringClass().getType());
				if (baseType == null)
					return null;
			}
			if (fragments != null && fragments.length > 0)
				for (int i = 0; i < fragments.length - 1; i++) {
					// If we have a more precise base type in the next field, we
					// take that
					AccessPathFragment curFragment = fragments[i];
					Type curType = curFragment.getFieldType();
					Type oldType = curType;
					if (!(curType instanceof ArrayType))
						curType = typeUtils.getMorePreciseType(curType,
								fragments[i + 1].getField().getDeclaringClass().getType());
					if (curType != oldType)
						fragments[i] = curFragment.copyWithNewType(curType);
				}
		}

		// Check the validity of our fragments
		if (fragments != null && Arrays.stream(fragments).anyMatch(f -> !f.isValid()))
			return null;

		// Make sure that only heap objects may have fields. Primitive arrays
		// with fields may occur on impossible type casts in the target program.
		if (value != null && value.getType() instanceof ArrayType) {
			ArrayType at = (ArrayType) value.getType();
			if (!(at.getArrayElementType() instanceof RefLikeType) && fragments != null && fragments.length > 0)
				return null;
		}

		// We can always merge a.inner.this$0.c to a.c. We do this first so that
		// we don't create recursive bases for stuff we don't need anyway.
		if (accessPathConfig.getUseThisChainReduction() && reduceBases && fragments != null) {
			fragments = THIS0_REDUCTION.reduceAccessPath(value, fragments);
		}

		// Check for recursive data structures. If a last field maps back to
		// something we already know, we build a repeatable component from it
		boolean recursiveCutOff = false;
		if (accessPathConfig.getUseRecursiveAccessPaths() && reduceBases && fragments != null) {
			// f0...fi references an object of type T, look for an extension f0...fi...fj
			// that also references an object of type T
			RefType objectType = Scene.v().getObjectType();
			int ei = val instanceof StaticFieldRef ? 1 : 0;
			while (ei < fragments.length) {
				final Type eiType = ei == 0 ? baseType : fragments[ei - 1].getFieldType();
				if (eiType != objectType) {
					final ContainerContext[] eiContext = ei == 0 ? null : fragments[ei - 1].getContext();
					int ej = ei;
					while (ej < fragments.length) {
						AccessPathFragment fj = fragments[ej];
						if (fj.getField().isPhantom())
							break;
						if ((fj.getFieldType() == eiType || fj.getField().getType() == eiType)
								&& Arrays.equals(eiContext, fj.getContext())) {
							// The types match, f0...fi...fj maps back to an object of the same type as
							// f0...fi. We must thus convert the access path to f0...fi-1[...fj]fj+1
							AccessPathFragment[] newFragments = new AccessPathFragment[fragments.length - (ej - ei)
									- 1];
							System.arraycopy(fragments, 0, newFragments, 0, ei);
							if (fragments.length > ej)
								System.arraycopy(fragments, ej + 1, newFragments, ei, fragments.length - ej - 1);

							// Register the base
							AccessPathFragment[] base = new AccessPathFragment[ej - ei + 1];
							System.arraycopy(fragments, ei, base, 0, base.length);
							registerBase(eiType, base);

							fragments = newFragments;
							recursiveCutOff = true;
						} else
							ej++;
					}
				}
				ei++;
			}
		}

		// Cut the fields at the maximum access path length. If this happens,
		// we must always add a star
		if (fragments != null) {
			final int maxAccessPathLength = accessPathConfig.getAccessPathLength();
			if (maxAccessPathLength >= 0) {
				int fieldNum = Math.min(maxAccessPathLength, fragments.length);
				if (fragments.length > fieldNum) {
					taintSubFields = true;
					cutOffApproximation = true;
				} else {
					cutOffApproximation = recursiveCutOff;
				}

				if (fieldNum == 0) {
					fragments = null;
				} else {
					AccessPathFragment[] newFragments = new AccessPathFragment[fieldNum];
					System.arraycopy(fragments, 0, newFragments, 0, fieldNum);
					fragments = newFragments;
				}
			} else
				cutOffApproximation = recursiveCutOff;
		} else {
			cutOffApproximation = false;
			fragments = null;
		}

		// Type checks
		assert value == null || !(!(baseType instanceof ArrayType) && !TypeUtils.isObjectLikeType(baseType)
				&& value.getType() instanceof ArrayType);
		assert value == null || !(baseType instanceof ArrayType && !(value.getType() instanceof ArrayType)
				&& !TypeUtils.isObjectLikeType(value.getType()))
				: "Type mismatch. Type was " + baseType + ", value was: " + (value == null ? null : value.getType());

		// Sanity check
		if (baseType instanceof PrimType) {
			if (fragments != null && fragments.length > 0) {
				logger.warn("Primitive types cannot have fields: baseType={} fields={}", baseType,
						Arrays.toString(fragments));
				return null;
			}
		}
		if (fragments != null) {
			for (int i = 0; i < fragments.length - 2; i++) {
				SootField f = fragments[i].getField();
				Type fieldType = f.getType();
				if (fieldType instanceof PrimType) {
					logger.warn("Primitive types cannot have fields: field={} type={}", f, fieldType);
					return null;
				}
			}
		}

		return new AccessPath(value, baseType, ctxt, fragments, taintSubFields, cutOffApproximation, arrayTaintType,
				canHaveImmutableAliases);
	}

	private void registerBase(Type eiType, AccessPathFragment[] base) {
		Set<AccessPathFragment[]> bases = baseRegister.computeIfAbsent(eiType,
				t -> Collections.synchronizedSet(new TCustomHashSet<>(new HashingStrategy<AccessPathFragment[]>() {

					private static final long serialVersionUID = 3017690689067651070L;

					@Override
					public int computeHashCode(AccessPathFragment[] arg0) {
						return Arrays.hashCode(arg0);
					}

					@Override
					public boolean equals(AccessPathFragment[] arg0, AccessPathFragment[] arg1) {
						return Arrays.equals(arg0, arg1);
					}

				})));
		bases.add(base);
	}

	public Collection<AccessPathFragment[]> getBaseForType(Type tp) {
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
		return copyWithNewValue(original, val, newType, cutFirstField, reduceBases, arrayTaintType, null);
	}

	/**
	 * value val gets new base, fields are preserved.
	 *
	 * @param original       The original access path
	 * @param val            The new base value
	 * @param reduceBases    True if circular types shall be reduced to bases
	 * @param arrayTaintType The way a tainted array shall be handled
	 * @param baseCtxt       New context
	 * @return This access path with the base replaced by the value given in the val
	 *         parameter
	 */
	public AccessPath copyWithNewValue(AccessPath original, Value val, Type newType, boolean cutFirstField,
			boolean reduceBases, ArrayTaintType arrayTaintType, ContainerContext[] baseCtxt) {
		// If this copy would not add any new information, we can safely use the
		// old object
		if (original.getPlainValue() != null && original.getPlainValue().equals(val)
				&& original.getBaseType().equals(newType) && original.getArrayTaintType() == arrayTaintType
				&& Arrays.equals(original.getBaseContext(), baseCtxt))
			return original;

		// Create the new access path
		AccessPath newAP = createAccessPath(val, newType, baseCtxt, original.getFragments(),
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
		return appendFields(ap1, ap2.getFragments(), ap2.getTaintSubFields());
	}

	/**
	 * Appends additional fields to the given access path
	 * 
	 * @param original       The original access path to which to append the fields
	 * @param toAppend       The access path fragments to append
	 * @param taintSubFields True if the new access path shall taint all objects
	 *                       reachable through it, false if it shall only point to
	 *                       precisely one object
	 * @return The new access path
	 */
	public AccessPath appendFields(AccessPath original, AccessPathFragment[] toAppend, boolean taintSubFields) {
		if (toAppend == null || toAppend.length == 0)
			return original;

		int offset = original.getFragmentCount();
		AccessPathFragment[] fragments = new AccessPathFragment[offset + (toAppend == null ? 0 : toAppend.length)];
		if (offset > 0)
			System.arraycopy(original.getFragments(), 0, fragments, 0, offset);
		System.arraycopy(toAppend, 0, fragments, offset, toAppend.length);

		return createAccessPath(original.getPlainValue(), original.getBaseType(), fragments, taintSubFields, false,
				true, original.getArrayTaintType());
	}

}
