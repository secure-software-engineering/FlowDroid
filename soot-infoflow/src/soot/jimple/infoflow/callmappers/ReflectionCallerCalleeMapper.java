package soot.jimple.infoflow.callmappers;

/**
 * Maps caller and callee parameters in a reflective method call
 * @author Marc Miltenberger
 */
public class ReflectionCallerCalleeMapper implements ICallerCalleeArgumentMapper {

	public static final ICallerCalleeArgumentMapper INSTANCE = new ReflectionCallerCalleeMapper();

	@Override
	public int getCallerIndexOfCalleeParameter(int calleeParamIndex) {
		//handling invoke(base, args);
		if (calleeParamIndex == BASE_OBJECT)
			return 0;
		else
			return 1;
	}

	@Override
	public int getCalleeIndexOfCallerParameter(int callerParameterIndex) {
		if (callerParameterIndex == 0)
			return BASE_OBJECT;
		//Since the caller argument is an entire array, all parameter indices are possible
		return ALL_PARAMS;
	}

}
