package soot.jimple.infoflow.values;

import java.util.Set;

import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.Stmt;

/**
 * Common interface for all algorithms that provide constant values
 * 
 * @author Steven Arzt
 *
 */
public interface IValueProvider {

	/**
	 * Tries to find pseudo-constants for a given value at a given statement in a method 
	 * @param <T> the type the pseudo-constant should be
	 * @param sm the method the statement is within
	 * @param stmt the statement for which to inquire a value
	 * @param value the value the analysis is interested in
	 * @param type the type the pseudo-constant should be
	 * @return the value as type <i>T</i> or null
	 */
	public <T> Set<T> getValue(SootMethod sm, Stmt stmt, Value value, Class<T> type);

	/**
	 * Tries to find a set of concrete types of a value
	 * @param sm the method the statement is within
	 * @param stmt the statement for which to inquire the type
	 * @param value the value the analysis is interested in
	 * @return a set of possible types for <i>value</i>
	 */
	public Set<Type> getType(SootMethod sm, Stmt stmt, Value value);

}
