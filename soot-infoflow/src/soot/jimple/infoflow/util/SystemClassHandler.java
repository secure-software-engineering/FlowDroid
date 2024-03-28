package soot.jimple.infoflow.util;

import soot.Body;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.cfg.FlowDroidUserClass;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;

/**
 * Utility class for checking whether methods belong to system classes
 * 
 * @author Steven Arzt
 */
public class SystemClassHandler {

	private static SystemClassHandler instance;

	private boolean excludeSystemComponents = true;

	/**
	 * Gets the global system class handler instance
	 * 
	 * @return The global system class handler instance
	 */
	public static SystemClassHandler v() {
		if (instance == null)
			instance = new SystemClassHandler();
		return instance;
	}

	/**
	 * Overwrites the global system class handler instance
	 * 
	 * @param instance The new the global system class handler instance
	 */
	public static void setInstance(SystemClassHandler instance) {
		SystemClassHandler.instance = instance;
	}

	/**
	 * Checks whether the given class belongs to a system package
	 * 
	 * @param clazz The class to check
	 * @return True if the given class belongs to a system package, otherwise false
	 */
	public boolean isClassInSystemPackage(SootClass clazz) {
		return clazz != null && !clazz.hasTag(FlowDroidUserClass.TAG_NAME) && isClassInSystemPackage(clazz.getName());
	}

	/**
	 * Checks whether the given class name belongs to a system package
	 * 
	 * @param className The class name to check
	 * @return True if the given class name belongs to a system package, otherwise
	 *         false
	 */
	public boolean isClassInSystemPackage(String className) {
		return (className.startsWith("android.") || className.startsWith("java.") || className.startsWith("javax.")
				|| className.startsWith("sun.") || className.startsWith("org.omg.")
				|| className.startsWith("org.w3c.dom.") || className.startsWith("com.google.")
				|| className.startsWith("com.android.")) && this.excludeSystemComponents;
	}

	/**
	 * Checks whether the type belongs to a system package
	 * 
	 * @param type The type to check
	 * @return True if the given type belongs to a system package, otherwise false
	 */
	public boolean isClassInSystemPackage(Type type) {
		if (type instanceof RefType)
			return isClassInSystemPackage(((RefType) type).getSootClass());
		return false;
	}

	/**
	 * If the base object is tainted and in a system class, but we reference some
	 * field in an application object derived from the system class, no tainting
	 * happens. The system class cannot access or know about user-code fields. This
	 * leaves reflection aside, but we don't support reflection anyway.
	 * 
	 * @param taintedPath The access path of the incoming taint
	 * @param method      The method that gets called
	 * @return True if the given taint is visible to the callee, otherwise false
	 */
	public boolean isTaintVisible(AccessPath taintedPath, SootMethod method) {
		// If we don't know anything about the tainted access path, we have to
		// conservatively assume that it's visible in the calllee
		if (taintedPath == null)
			return true;

		// If the complete base object is tainted, this is always visible
		if (!taintedPath.isInstanceFieldRef())
			return true;

		// User code can cast objects to arbitrary user and system types
		if (!isClassInSystemPackage(method.getDeclaringClass().getName()))
			return true;

		// Check whether we have a system-defined field followed by a user-defined field
		// in our access path
		boolean hasSystemType = taintedPath.getBaseType() != null && isClassInSystemPackage(taintedPath.getBaseType());
		for (AccessPathFragment fragment : taintedPath.getFragments()) {
			boolean curFieldIsSystem = isClassInSystemPackage(fragment.getFieldType())
					|| isClassInSystemPackage(fragment.getField().getDeclaringClass().getType());

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

	/**
	 * Sets whether components in system or framework-related packages shall be
	 * excluded from the analysis
	 *
	 * @param excludeSystemComponents True to exclude components in system packages
	 *                                from the analysis, false otherwise
	 */
	public void setExcludeSystemComponents(boolean excludeSystemComponents) {
		this.excludeSystemComponents = excludeSystemComponents;
	}

	/**
	 * Checks whether the given method body is a stub implementation and can safely
	 * be overwritten
	 * 
	 * @param body The body to check
	 * @return True if the given method body is a stub implementation, otherwise
	 *         false
	 */
	public boolean isStubImplementation(Body body) {
		Constant stubConst = StringConstant.v("Stub!");
		for (Unit u : body.getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				InvokeExpr iexpr = stmt.getInvokeExpr();
				SootMethod targetMethod = iexpr.getMethod();
				if (targetMethod.isConstructor()
						&& targetMethod.getDeclaringClass().getName().equals("java.lang.RuntimeException"))
					if (iexpr.getArgCount() > 0 && iexpr.getArg(0).equals(stubConst))
						return true;
			}
		}
		return false;
	}

}
