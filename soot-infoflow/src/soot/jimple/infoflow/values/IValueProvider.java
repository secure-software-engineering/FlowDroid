package soot.jimple.infoflow.values;

import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

/**
 * Common interface for all algorithms that provide constant values
 * 
 * @author Steven Arzt
 *
 */
public interface IValueProvider {

	public <T> T getValue(SootMethod sm, Stmt stmt, Value value, Class<T> type);

}
