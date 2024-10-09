package soot.jimple.infoflow.methodSummary.util;

import soot.RefType;
import soot.Type;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Utility class for handling aliasing-related checks
 * 
 * @author Steven Arzt
 *
 */
public class AliasUtils {

	/**
	 * Checks whether the given access path can potentially have aliases
	 * 
	 * @param ap The access path to check
	 * @return True if the given access path can potentially have aliases, otherwise
	 *         false
	 */
	public static boolean canAccessPathHaveAliases(AccessPath ap) {
		Type lastType = ap.getLastFieldType();
		return lastType instanceof RefType && !((RefType) lastType).getSootClass().getName().equals("java.lang.String");
	}

}
