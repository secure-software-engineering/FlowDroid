package soot.jimple.infoflow.values;

import soot.SootMethod;
import soot.Value;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

/**
 * Simple constant value provider that checks whether the queried value is
 * directly a Soot constant
 * 
 * @author Steven Arzt
 *
 */
public class SimpleConstantValueProvider implements IValueProvider {

	@SuppressWarnings("unchecked")
	@Override
	public Object getValue(SootMethod sm, Stmt stmt, Value value, Class type) {
		if (type == int.class || type == Integer.class) {
			if (value instanceof IntConstant)
				return ((IntConstant) value).value;
		} else if (type == long.class || type == Long.class) {
			if (value instanceof LongConstant)
				return ((LongConstant) value).value;
		} else if (type == float.class || type == Float.class) {
			if (value instanceof FloatConstant)
				return ((FloatConstant) value).value;
		} else if (type == double.class || type == Double.class) {
			if (value instanceof DoubleConstant)
				return ((DoubleConstant) value).value;
		} else if (type == String.class) {
			if (value instanceof StringConstant)
				return ((StringConstant) value).value;
		}
		return null;
	}

}
