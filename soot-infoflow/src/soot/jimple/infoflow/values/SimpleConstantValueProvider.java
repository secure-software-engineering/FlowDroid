package soot.jimple.infoflow.values;

import java.util.List;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.tagkit.ConstantValueTag;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.StringConstantValueTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Simple constant value provider that checks whether the queried value is
 * directly a Soot constant or is derived from a field with a
 * {@link ConstantValueTag}
 * 
 * @author Steven Arzt
 *
 */
public class SimpleConstantValueProvider implements IValueProvider {

	@SuppressWarnings("unchecked")
	@Override
	public Object getValue(SootMethod sm, Stmt stmt, Value value, Class type) {
		if (value instanceof Constant)
			return getConstantOfType(value, type);

		if (value instanceof Local) {
			// Find the defs
			BriefUnitGraph ug = new BriefUnitGraph(sm.getActiveBody());
			SimpleLocalDefs du = new SimpleLocalDefs(ug);
			List<Unit> defs = du.getDefsOfAt((Local) value, stmt);
			for (Unit def : defs) {
				if (!(def instanceof AssignStmt))
					continue;

				// Get the field of the rhs
				Value rightOp = ((AssignStmt) def).getRightOp();
				if (!(rightOp instanceof FieldRef))
					continue;

				// Use the ConstantTag to retrieve the constant value
				Object constant = getConstantFromTag(((FieldRef) rightOp).getField(), type);
				if (constant != null)
					return constant;
			}
		}

		return null;
	}

	private Object getConstantOfType(Value value, Class<?> type) {
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

	private Object getConstantFromTag(SootField f, Class<?> type) {
		if (type == int.class || type == Integer.class) {
			IntegerConstantValueTag t = (IntegerConstantValueTag) f.getTag("IntegerConstantValueTag");
			return t == null ? null : t.getIntValue();
		} else if (type == long.class || type == Long.class) {
			LongConstantValueTag t = (LongConstantValueTag) f.getTag("LongConstantValueTag");
			return t == null ? null : t.getLongValue();
		} else if (type == float.class || type == Float.class) {
			FloatConstantValueTag t = (FloatConstantValueTag) f.getTag("FloatConstantValueTag");
			return t == null ? null : t.getFloatValue();
		} else if (type == double.class || type == Double.class) {
			DoubleConstantValueTag t = (DoubleConstantValueTag) f.getTag("DoubleConstantValueTag");
			return t == null ? null : t.getDoubleValue();
		} else if (type == String.class) {
			StringConstantValueTag t = (StringConstantValueTag) f.getTag("StringConstantValueTag");
			return t == null ? null : t.getStringValue();
		}
		return null;
	}
}
