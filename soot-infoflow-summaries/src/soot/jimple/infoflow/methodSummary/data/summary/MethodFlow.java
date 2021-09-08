package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Map;

import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

/**
 * Class representing a summarized data flow in a given API method
 * 
 * @author Steven Arzt
 *
 */
public class MethodFlow extends AbstractMethodSummary {

	private final FlowSource from;
	private final FlowSink to;
	private final boolean isAlias;
	private final Boolean typeChecking;
	private final Boolean ignoreTypes;
	private final Boolean cutSubFields;

	/**
	 * Creates a new instance of the MethodFlow class
	 * 
	 * @param methodSig    The signature of the method containing the flow
	 * @param from         The start of the data flow (source)
	 * @param to           The end of the data flow (sink)
	 * @param isAlias      True if the source and the sink alias, false if this is
	 *                     not the case.
	 * @param typeChecking True if type checking shall be performed before applying
	 *                     this data flow, otherwise false
	 * @param ignoreTypes  True if the type of potential fields should not be
	 *                     altered
	 * @param cutSubFields True if no sub fields shall be copied from the source to
	 *                     the sink. If "a.b" is tainted and the source is "a", the
	 *                     field "b" will not appended to the sink if this option is
	 *                     enabled.
	 */
	public MethodFlow(String methodSig, FlowSource from, FlowSink to, boolean isAlias, Boolean typeChecking,
			Boolean ignoreTypes, Boolean cutSubFields) {
		super(methodSig);
		this.from = from;
		this.to = to;
		this.isAlias = isAlias;
		this.typeChecking = typeChecking;
		this.ignoreTypes = ignoreTypes;
		this.cutSubFields = cutSubFields;
	}

	/**
	 * Gets the source, i.e., the incoming flow
	 * 
	 * @return The incoming flow
	 */
	public FlowSource source() {
		return from;
	}

	/**
	 * Gets the sink, i.e., the outgoing flow
	 * 
	 * @return The outgoing flow
	 */
	public FlowSink sink() {
		return to;
	}

	/**
	 * Checks whether the current flow is coarser than the given flow, i.e., if all
	 * elements referenced by the given flow are also referenced by this flow
	 * 
	 * @param flow The flow with which to compare the current flow
	 * @return True if the current flow is coarser than the given flow, otherwise
	 *         false
	 */
	public boolean isCoarserThan(MethodFlow flow) {
		if (flow.equals(this))
			return true;

		return this.from.isCoarserThan(flow.source()) && this.to.isCoarserThan(flow.sink());
	}

	/**
	 * Reverses the current flow
	 * 
	 * @return The reverse of the current flow
	 */
	public MethodFlow reverse() {
		// Special case: If the source is a gap base object, we have to correct the
		// specification format
		boolean taintSubFields = to.taintSubFields();
		SourceSinkType fromType = to.getType();
		SourceSinkType toType = from.getType();
		if (from.getType() == SourceSinkType.Field && !from.hasAccessPath() && from.hasGap()) {
			toType = SourceSinkType.GapBaseObject;
			taintSubFields = false;
		}
		if (to.isGapBaseObject()) {
			fromType = SourceSinkType.Field;
			taintSubFields = true;
		}

		FlowSource reverseSource = new FlowSource(fromType, to.getParameterIndex(), to.getBaseType(),
				to.getAccessPath(), to.getGap(), to.isMatchStrict());
		FlowSink reverseSink = new FlowSink(toType, from.getParameterIndex(), from.getBaseType(), from.getAccessPath(),
				taintSubFields, from.getGap(), from.isMatchStrict());
		return new MethodFlow(methodSig, reverseSource, reverseSink, isAlias, typeChecking, ignoreTypes, cutSubFields);
	}

	/**
	 * Gets whether the source and the sink of this data flow alias
	 * 
	 * @return True the source and the sink of this data flow alias, otherwise false
	 */
	public boolean isAlias() {
		return this.isAlias;
	}

	/**
	 * Gets whether type checking shall be performed before applying this method
	 * flow
	 * 
	 * @return True if type checking shall be performed before applying this method
	 *         flow, otherwise false
	 */
	public Boolean getTypeChecking() {
		return this.typeChecking;
	}

	/**
	 * Gets whether sub fields shall not be appended when applying this method flow.
	 * If "a.b" is tainted and the source is "a", the field "b" will not appended to
	 * the sink if this option is enabled.
	 * 
	 * @return True if sub fields shall be discarded and shall not be appended to
	 *         the sink
	 */
	public Boolean getCutSubFields() {
		return cutSubFields;
	}

	/**
	 * Gets whether this flow has a custom source or sink
	 * 
	 * @return True if this flow has a custom source or sink, otherwise false
	 */
	public boolean isCustom() {
		return from.isCustom() || to.isCustom();
	}

	@Override
	public MethodFlow replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (replacementMap == null)
			return this;
		return new MethodFlow(methodSig, from.replaceGaps(replacementMap), to.replaceGaps(replacementMap), isAlias,
				typeChecking, ignoreTypes, cutSubFields);
	}

	/**
	 * Checks for errors inside this data flow summary
	 */
	public void validate() {
		source().validate(methodSig);
		sink().validate(methodSig);

		// Make sure that the types of gap base objects and incoming flows are
		// cast-compatible
		if (sink().getType() == SourceSinkType.GapBaseObject && sink().getGap() != null) {
			String sinkType = SootMethodRepresentationParser.v().parseSootMethodString(sink().getGap().getSignature())
					.getClassName();

			Type t1 = RefType.v(sink().getBaseType());
			Type t2 = RefType.v(sinkType);

			if (!Scene.v().getFastHierarchy().canStoreType(t1, t2) // cast-up,
																	// i.e.
																	// Object to
																	// String
					&& !Scene.v().getFastHierarchy().canStoreType(t2, t1)) // cast-down,
																			// i.e.
																			// String
																			// to
																			// Object
				throw new RuntimeException("Target type of gap base flow is invalid");
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodFlow other = (MethodFlow) obj;
		if (cutSubFields == null) {
			if (other.cutSubFields != null)
				return false;
		} else if (!cutSubFields.equals(other.cutSubFields))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (isAlias != other.isAlias)
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (typeChecking == null) {
			if (other.typeChecking != null)
				return false;
		} else if (!typeChecking.equals(other.typeChecking))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((cutSubFields == null) ? 0 : cutSubFields.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + (isAlias ? 1231 : 1237);
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((typeChecking == null) ? 0 : typeChecking.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "{" + methodSig + " Source: [" + from.toString() + "] Sink: [" + to.toString() + "]" + "}";
	}

	public boolean getIgnoreTypes() {
		if (ignoreTypes == null) {
			if (typeChecking != null && !typeChecking.booleanValue()) {
				if ("java.lang.Object[]".equals(to.getLastFieldType()))
					return true;
			}
			return false;
		}
		return ignoreTypes;
	}

}
