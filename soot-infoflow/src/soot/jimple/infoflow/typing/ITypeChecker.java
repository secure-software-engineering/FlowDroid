package soot.jimple.infoflow.typing;

import soot.Type;

/**
 * Common interface for all classes that can check type compatibility
 * 
 * @author Steven Arzt
 *
 */
public interface ITypeChecker {

	/**
	 * Gets the more precise one of the two given types. If there is no ordering
	 * (i.e., the two types are not cast-compatible) null is returned.
	 * 
	 * @param tp1 The first type
	 * @param tp2 The second type
	 * @return The more precise one of the two given types
	 */
	public Type getMorePreciseType(Type tp1, Type tp2);

}
