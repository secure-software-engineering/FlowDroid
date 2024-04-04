package soot.jimple.infoflow.callmappers;

/**
 * Is applicaple when we have no mapping whatsoever.
 * @author Marc Miltenberger
 */
public class UnknownCallerCalleeMapper implements ICallerCalleeArgumentMapper {

	public static final ICallerCalleeArgumentMapper INSTANCE = new UnknownCallerCalleeMapper();

	@Override
	public int getCallerIndexOfCalleeParameter(int calleeParamIndex) {
		return UNKNOWN;
	}

	@Override
	public int getCalleeIndexOfCallerParameter(int callerParameterIndex) {
		return UNKNOWN;
	}

}
