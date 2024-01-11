package soot.jimple.infoflow.callmappers;

import soot.Body;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;

/**
 * Maps caller to callee arguments and vice versa.
 * There are special mappers e.g. for reflective calls and virtual edges 
 * @author Marc Miltenberger
 */
public abstract interface ICallerCalleeArgumentMapper {

	public static final int BASE_OBJECT = -1;

	/**
	 * Indicates that all parameters are possible.
	 */
	public static final int ALL_PARAMS = -2;

	/**
	 * Indicates that we have no information about a mapping.
	 */
	public static final int UNKNOWN = -3;

	/**
	 * Returns the index of the parameter in the caller corresponding to "calleeParamIndex"
	 * @param calleeParamIndex the desired parameter index
	 * @return the index of the parameter in the caller corresponding to calleeParamIndex
	 */
	public int getCallerIndexOfCalleeParameter(int calleeParamIndex);

	/**
	 * Uses getMappedIndexOfCallerParameter to determine the actual value
	 * @param expr the invoke expression of the caller
	 * @param calleeParamIndex the desired parameter index
	 * @return the mapped value in the caller invoke expr
	 */
	public default Value getCallerValueOfCalleeParameter(InvokeExpr expr, int calleeParamIndex) {
		int idx = getCallerIndexOfCalleeParameter(calleeParamIndex);
		if (idx == BASE_OBJECT) {
			if (expr instanceof InstanceInvokeExpr)
				return ((InstanceInvokeExpr) expr).getBase();
			return null;
		} else if (idx == UNKNOWN)
			return null;
		return expr.getArg(idx);
	}

	/**
	 * Returns the index of the parameter in the callee corresponding to the caller parameter  
	 * @param callerParamIndex the caller parameter
	 * @return the index of the parameter in the callee
	 */
	public int getCalleeIndexOfCallerParameter(int callerParamIndex);

	/**
	 * Returns the index of the parameter in the callee corresponding to the caller parameter  
	 * @param callerParamIndex the caller parameter
	 * @param body the body of the callee
	 * @return the mapped value of the parameter in the callee
	 */

	public default Value getCalleeValueOfCallerParameter(int callerParamIndex, Body body) {
		int idx = getCalleeIndexOfCallerParameter(callerParamIndex);
		if (idx == UNKNOWN)
			return null;
		else if (idx == BASE_OBJECT)
			return body.getThisLocal();
		else
			return body.getParameterLocal(idx);
	}

	public default boolean isReflectiveMapper() {
		return this instanceof ReflectionCallerCalleeMapper;
	}
}
