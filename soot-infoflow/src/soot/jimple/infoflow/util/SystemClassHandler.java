package soot.jimple.infoflow.util;

import soot.RefType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Utility class for checking whether methods belong to system classes
 * 
 * @author Steven Arzt
 */
public class SystemClassHandler {

	/**
	 * Checks whether the given class name belongs to a system package
	 * 
	 * @param className
	 *            The class name to check
	 * @return True if the given class name belongs to a system package, otherwise
	 *         false
	 */
	public static boolean isClassInSystemPackage(String className) {
		return className.startsWith("android.") || className.startsWith("java.") || className.startsWith("javax.")
				|| className.startsWith("sun.") || className.startsWith("org.omg.")
				|| className.startsWith("org.w3c.dom.") || className.startsWith("com.google.")
				|| className.startsWith("com.android.");
	}

	/**
	 * Checks whether the type belongs to a system package
	 * 
	 * @param type
	 *            The type to check
	 * @return True if the given type belongs to a system package, otherwise false
	 */
	public static boolean isClassInSystemPackage(Type type) {
		if (type instanceof RefType)
			return isClassInSystemPackage(((RefType) type).getSootClass().getName());
		return false;
	}

	/**
	 * If the base object is tainted and in a system class, but we reference some
	 * field in an application object derived from the system class, no tainting
	 * happens. The system class cannot access or know about user-code fields. This
	 * leaves reflection aside, but we don't support reflection anyway.
	 * 
	 * @param taintedPath
	 *            The access path of the incoming taint
	 * @param method
	 *            The method that gets called
	 * @return True if the given taint is visible to the callee, otherwise false
	 */
	public static boolean isTaintVisible(AccessPath taintedPath, SootMethod method) {
		// If we don't know anything about the tainted access path, we have to
		// conservatively assume that it's visible in the calllee
		if (taintedPath == null)
			return true;

		// If the complete base object is tainted, this is always visible
		if (!taintedPath.isInstanceFieldRef())
			return true;

		// User code can cast objects to arbitrary user and system types
		if (!SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return true;

		// Check whether we have a system-defined field followed by a user-defined field
		// in our access path
		boolean hasSystemType = taintedPath.getBaseType() != null
				&& SystemClassHandler.isClassInSystemPackage(taintedPath.getBaseType());
		for (SootField fld : taintedPath.getFields()) {
			boolean curFieldIsSystem = SystemClassHandler.isClassInSystemPackage(fld.getType());
			if (SystemClassHandler.isClassInSystemPackage(fld.getDeclaringClass().getType()))
				curFieldIsSystem = true;

			if (curFieldIsSystem) {
				hasSystemType = true;
			} else {
				if (hasSystemType)
					return false;
			}
		}

		// We don't have a reason to believe that this taint is invisible to the
		// callee
		return true;
	}

}
