package soot.jimple.infoflow.callmappers;

/**
 * Maps caller and callee parameters in a one to one relationship
 * @author Marc Miltenberger
 */
public class IdentityCallerCalleeMapper implements ICallerCalleeArgumentMapper {

	public static final ICallerCalleeArgumentMapper INSTANCE = new IdentityCallerCalleeMapper();

	@Override
	public int getCallerIndexOfCalleeParameter(int calleeParamIndex) {
		return calleeParamIndex;
	}

	@Override
	public int getCalleeIndexOfCallerParameter(int callerParameterIndex) {
		return callerParameterIndex;
	}

}
