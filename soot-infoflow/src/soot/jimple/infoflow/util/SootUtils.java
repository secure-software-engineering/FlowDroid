package soot.jimple.infoflow.util;

import java.util.ArrayList;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;

/**
 * Class containing various utility methods for dealing with Soot
 * 
 * @author Steven Arzt
 *
 */
public class SootUtils {

	public static List<Value> getUseAndDefValues(Unit u) {
		List<Value> valueList = new ArrayList<>();
		for (ValueBox vb : u.getUseAndDefBoxes()) {
			Value val = vb.getValue();
			if (val instanceof InvokeExpr) {
				InvokeExpr iexpr = (InvokeExpr) val;
				for (ValueBox invBox : iexpr.getUseBoxes())
					valueList.add(invBox.getValue());
			} else
				valueList.add(val);
		}
		return valueList;
	}

	/**
	 * Finds a method with the given signature in the given class or one of its
	 * super classes
	 * 
	 * @param currentClass The current class in which to start the search
	 * @param subsignature The subsignature of the method to find
	 * @return The method with the given signature if it has been found, otherwise
	 *         null
	 */
	public static SootMethod findMethod(SootClass currentClass, String subsignature) {
		Scene sc = Scene.v();
		return sc.getOrMakeFastHierarchy().resolveMethod(currentClass,
				sc.makeMethodRef(currentClass, subsignature, false), true);
	}

}
