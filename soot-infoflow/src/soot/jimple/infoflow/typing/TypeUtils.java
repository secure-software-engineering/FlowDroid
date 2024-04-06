package soot.jimple.infoflow.typing;

import java.util.List;
import java.util.stream.Collectors;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FastHierarchy;
import soot.FloatType;
import soot.Hierarchy;
import soot.IntType;
import soot.LongType;
import soot.MethodSubSignature;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Class containing various utility methods for dealing with type information
 * 
 * @author Steven Arzt
 *
 */
public class TypeUtils {

	private final InfoflowManager manager;
	private final Scene scene;

	public TypeUtils(InfoflowManager manager) {
		this.manager = manager;
		this.scene = Scene.v();
	}

	/**
	 * Checks whether the given type is a string
	 * 
	 * @param tp The type of check
	 * @return True if the given type is a string, otherwise false
	 */
	public static boolean isStringType(Type tp) {
		if (!(tp instanceof RefType))
			return false;
		RefType refType = (RefType) tp;
		return refType.getClassName().equals("java.lang.String");
	}

	/**
	 * Checks whether the given type is java.lang.Object, java.io.Serializable, or
	 * java.lang.Cloneable.
	 * 
	 * @param tp The type to check
	 * @return True if the given type is one of the three "object-like" types,
	 *         otherwise false
	 */
	public static boolean isObjectLikeType(Type tp) {
		if (!(tp instanceof RefType))
			return false;

		RefType rt = (RefType) tp;
		final String className = rt.getSootClass().getName();
		return className.equals("java.lang.Object") || className.equals("java.io.Serializable")
				|| className.equals("java.lang.Cloneable");
	}

	/**
	 * Checks whether the given source type can be cast to the given destination
	 * type
	 * 
	 * @param destType   The destination type to which to cast
	 * @param sourceType The source type from which to cast
	 * @return True if the given types are cast-compatible, otherwise false
	 */
	public boolean checkCast(Type destType, Type sourceType) {
		if (!manager.getConfig().getEnableTypeChecking())
			return true;

		// If we don't have a source type, we generally allow the cast
		if (sourceType == null)
			return true;

		// If both types are equal, we allow the cast
		if (sourceType == destType)
			return true;

		// If both types are primitive, they can be cast.
		if (destType instanceof PrimType && sourceType instanceof PrimType)
			return true;

		// If we have a reference type, we use the Soot hierarchy
		FastHierarchy hierarchy = manager.getHierarchy();
		if (hierarchy != null) {
			if (hierarchy.canStoreType(destType, sourceType) // cast-up,
					// i.e.
					// Object
					// to
					// String
					|| manager.getHierarchy().canStoreType(sourceType, destType)) // cast-down,
																					// i.e.
																					// String
																					// to
																					// Object
				return true;
		}

		return false;
	}

	/**
	 * Checks whether the type of the given taint can be cast to the given target
	 * type
	 * 
	 * @param accessPath The access path of the taint to be cast
	 * @param type       The target type to which to cast the taint
	 * @return True if the cast is possible, otherwise false
	 */
	public boolean checkCast(AccessPath accessPath, Type type) {
		if (!manager.getConfig().getEnableTypeChecking())
			return true;

		final int fragmentCount = accessPath.getFragmentCount();
		int fieldStartIdx = 0;
		if (accessPath.isStaticFieldRef()) {
			if (!checkCast(type, accessPath.getFirstFieldType()))
				return false;

			// If the target type is a primitive array, we cannot have any
			// subsequent field
			if (isPrimitiveArray(type))
				if (fragmentCount > 1)
					return false;
			fieldStartIdx = 1;
		} else {
			if (!checkCast(type, accessPath.getBaseType()))
				return false;
			// If the target type is a primitive array, we cannot have any
			if (isPrimitiveArray(type))
				// subsequent fields
				if (!accessPath.isLocal())
					return false;
		}

		// The next field's base type must also be cast-compatible to the new
		// base type
		if (accessPath.isFieldRef() && fragmentCount > fieldStartIdx) {
			// Unpack any array type first
			if (type instanceof ArrayType)
				type = ((ArrayType) type).getElementType();
			if (!checkCast(type, accessPath.getFragments()[fieldStartIdx].getField().getDeclaringClass().getType()))
				return false;
		}

		// No type problems found
		return true;
	}

	public static boolean isPrimitiveArray(Type type) {
		if (type instanceof ArrayType) {
			ArrayType at = (ArrayType) type;
			if (at.getArrayElementType() instanceof PrimType)
				return true;
		}
		return false;
	}

	public boolean hasCompatibleTypesForCall(AccessPath apBase, SootClass dest) {
		if (!manager.getConfig().getEnableTypeChecking())
			return true;

		// Cannot invoke a method on a primitive type
		if (apBase.getBaseType() instanceof PrimType)
			return false;
		// Cannot invoke a method on an array
		if (apBase.getBaseType() instanceof ArrayType)
			return dest.getName().equals("java.lang.Object");

		return checkCast(apBase, dest.getType());
	}

	/**
	 * Gets the more precise one of the two given types. If there is no ordering
	 * (i.e., the two types are not cast-compatible) null is returned.
	 * 
	 * @param tp1 The first type
	 * @param tp2 The second type
	 * @return The more precise one of the two given types
	 */
	public Type getMorePreciseType(Type tp1, Type tp2) {
		final FastHierarchy fastHierarchy = scene.getOrMakeFastHierarchy();

		if (tp1 == null)
			return tp2;
		else if (tp2 == null)
			return tp1;
		else if (tp1 == tp2)
			return tp1;
		else if (TypeUtils.isObjectLikeType(tp1))
			return tp2;
		else if (TypeUtils.isObjectLikeType(tp2))
			return tp1;
		else if (tp1 instanceof PrimType && tp2 instanceof PrimType)
			return null;
		else if (fastHierarchy.canStoreType(tp2, tp1))
			return tp2;
		else if (fastHierarchy.canStoreType(tp1, tp2))
			return tp1;
		else {
			// If one type is an array type and the other one is the base type,
			// we still accept the cast
			if (tp1 instanceof ArrayType && tp2 instanceof ArrayType) {
				ArrayType at1 = (ArrayType) tp1;
				ArrayType at2 = (ArrayType) tp2;
				if (at1.numDimensions != at2.numDimensions)
					return null;
				Type preciseType = getMorePreciseType(at1.getElementType(), at2.getElementType());
				if (preciseType == null)
					return null;

				return ArrayType.v(preciseType, at1.numDimensions);
			} else if (tp1 instanceof ArrayType) {
				ArrayType at = (ArrayType) tp1;
				return getMorePreciseType(at.getElementType(), tp2);
			} else if (tp2 instanceof ArrayType) {
				ArrayType at = (ArrayType) tp2;
				return getMorePreciseType(tp1, at.getElementType());
			}
		}
		return null;
	}

	/**
	 * Gets the more precise one of the two given types
	 * 
	 * @param tp1 The first type
	 * @param tp2 The second type
	 * @return The more precise one of the two given types
	 */
	public String getMorePreciseType(String tp1, String tp2) {
		Type newType = getMorePreciseType(getTypeFromString(tp1), getTypeFromString(tp2));
		return newType == null ? null : "" + newType;
	}

	/**
	 * Creates a Soot Type from the given string
	 * 
	 * @param type A string representing a Soot type
	 * @return The Soot Type corresponding to the given string
	 */
	public static Type getTypeFromString(String type) {
		if (type == null || type.isEmpty())
			return null;

		// Reduce arrays
		int numDimensions = 0;
		while (type.endsWith("[]")) {
			numDimensions++;
			type = type.substring(0, type.length() - 2);
		}

		// Generate the target type
		final Type t;
		if (type.equals("int"))
			t = IntType.v();
		else if (type.equals("long"))
			t = LongType.v();
		else if (type.equals("float"))
			t = FloatType.v();
		else if (type.equals("double"))
			t = DoubleType.v();
		else if (type.equals("boolean"))
			t = BooleanType.v();
		else if (type.equals("char"))
			t = CharType.v();
		else if (type.equals("short"))
			t = ShortType.v();
		else if (type.equals("byte"))
			t = ByteType.v();
		else {
			// Do not create types for stuff that isn't loaded in the current
			// scene, i.e., does not appear in the program under analysis
			if (Scene.v().containsClass(type))
				t = RefType.v(type);
			else
				return null;
		}

		if (numDimensions == 0)
			return t;
		return ArrayType.v(t, numDimensions);
	}

	/**
	 * Builds a new array of the given type if it is a base type or increments the
	 * dimensions of the given array by 1 otherwise.
	 * 
	 * @param type      The base type or incoming array
	 * @param arrayType The declared type of the array
	 * @return The resulting array
	 */
	public static Type buildArrayOrAddDimension(Type type, Type arrayType) {
		// If the given element is a base type and the array has a more general
		// type, we must take the array type instead
		if (!(type instanceof ArrayType)) {
			return arrayType;
		}

		// If code takes a tainted array and recursively creates a higher-
		// dimensional one that receives the old array as an element, we end up
		// with an infinitely growing number of dimensions. We therefore kill
		// the type information if we get more dimensions.
		if (type instanceof ArrayType) {
			ArrayType array = (ArrayType) type;
			if (array.numDimensions >= 3)
				return null;
			return array.makeArrayType();
		} else
			return ArrayType.v(type, 1);
	}

	/**
	 * Gets all classes that inherit from the given class or that transitively
	 * implement the given interface
	 * 
	 * @param classOrInterface The class or interface for which to enumerate the
	 *                         derived classes
	 * @return The classes derived from the given class or interface
	 */
	public static List<SootClass> getAllDerivedClasses(SootClass classOrInterface) {
		final Hierarchy h = Scene.v().getActiveHierarchy();
		if (classOrInterface.isInterface()) {
			return h.getSubinterfacesOfIncluding(classOrInterface).stream()
					.flatMap(i -> h.getImplementersOf(i).stream()).collect(Collectors.toList());
		} else
			return h.getSubclassesOfIncluding(classOrInterface);
	}

	/**
	 * Checks whether a value of type "child" can be stored in a variable of type
	 * "parent". If one of the types is <code>null</code>, we assume that the value
	 * cannot be stored.
	 * 
	 * @param fh     The {@link FastHierarchy}
	 * @param child  The actual type of the value
	 * @param parent The declared type of the variable
	 * @return True if a value of type "child" can be stored in a variable of type
	 *         "parent", false otherwise
	 */
	public static boolean canStoreType(FastHierarchy fh, Type child, Type parent) {
		if (child == null || parent == null)
			return false;
		return fh.canStoreType(child, parent);
	}

	/**
	 * Checks whether "overriden" is an overriden version of "originalSubSig". We
	 * have to check for covariance of the return value and the contra variance of
	 * all parameters
	 * 
	 * @param fh             The {@link FastHierarchy}
	 * @param originalSubSig The sub signature of the base method
	 * @param overriden      The potentially method overriding the method of
	 *                       "originalSubSig"
	 * @return true if "overriden" overrides "originalSubSig"
	 */
	public static boolean isOverriden(FastHierarchy fh, MethodSubSignature originalSubSig, SootMethod overriden) {
		if (!originalSubSig.getMethodName().equals(overriden.getName())
				|| originalSubSig.getParameterTypes().size() != overriden.getParameterCount())
			return false;

		if (!fh.canStoreType(overriden.getReturnType(), originalSubSig.returnType))
			return false;

		for (int i = 0; i < overriden.getParameterCount(); i++) {
			if (!fh.canStoreType(originalSubSig.getParameterTypes().get(i), overriden.getParameterType(i)))
				return false;
		}

		return true;
	}

	/**
	 * Checks whether "overriden" is an overriden version of "originalSubSig". We
	 * have to check for covariance of the return value and the contra variance of
	 * all parameters
	 * 
	 * @param originalSubSig The sub signature of the base method
	 * @param overriden      The potentially method overriding the method of
	 *                       "originalSubSig"
	 * @return true if "overriden" overrides "originalSubSig"
	 */
	public boolean isOverriden(MethodSubSignature originalSubSig, SootMethod overriden) {
		FastHierarchy hierarchy = manager.getHierarchy();
		return isOverriden(hierarchy, originalSubSig, overriden);
	}
}
