package soot.jimple.infoflow.typing;

import soot.ArrayType;
import soot.FastHierarchy;
import soot.PrimType;
import soot.Scene;
import soot.Type;

/**
 * A type checker that queries the normal Soot data structures
 * 
 * @author Steven Arzt
 *
 */
class SootBasedTypeChecker implements ITypeChecker {

	@Override
	public Type getMorePreciseType(Type tp1, Type tp2) {
		final FastHierarchy fastHierarchy = Scene.v().getOrMakeFastHierarchy();

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
			return tp1; // arbitrary choice
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

}
