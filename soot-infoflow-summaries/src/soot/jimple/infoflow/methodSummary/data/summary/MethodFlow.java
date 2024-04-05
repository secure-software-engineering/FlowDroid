package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;
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
	private final Boolean typeChecking;
	private final Boolean ignoreTypes;
	private final Boolean cutSubFields;
	private final boolean isFinal;
	private final boolean excludedOnClear;

	/**
	 * Creates a new instance of the MethodFlow class
	 *
	 * @param methodSig       The signature of the method containing the flow
	 * @param from            The start of the data flow (source)
	 * @param to              The end of the data flow (sink)
	 * @param isAlias         True if the source and the sink alias, false if this
	 *                        is not the case.
	 * @param typeChecking    True if type checking shall be performed before
	 *                        applying this data flow, otherwise false
	 * @param ignoreTypes     True if the type of potential fields should not be
	 *                        altered
	 * @param cutSubFields    True if no sub fields shall be copied from the source
	 *                        to the sink. If "a.b" is tainted and the source is
	 *                        "a", the field "b" will not appended to the sink if
	 *                        this option is enabled.
	 * @param constraints     List of constraints that may be referenced in the flow
	 * @param isFinal         True if the flow should is complete, i.e. does not
	 *                        need a fixpoint
	 * @param excludedOnClear True if the flow should not be applied if the incoming
	 *                        taint is killed
	 */
	public MethodFlow(String methodSig, FlowSource from, FlowSink to, IsAliasType isAlias, Boolean typeChecking,
			Boolean ignoreTypes, Boolean cutSubFields, FlowConstraint[] constraints, boolean isFinal,
			boolean excludedOnClear) {
		super(methodSig, constraints, isAlias);
		this.from = from;
		this.to = to;
		this.typeChecking = typeChecking;
		this.ignoreTypes = ignoreTypes;
		this.cutSubFields = cutSubFields;
		this.isFinal = isFinal;
		this.excludedOnClear = excludedOnClear;
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
				to.getAccessPath(), to.getGap(), to.isMatchStrict(), to.getConstraintType());
		FlowSink reverseSink = new FlowSink(toType, from.getParameterIndex(), from.getBaseType(), from.getAccessPath(),
				taintSubFields, from.getGap(), from.isMatchStrict(), from.getConstraintType());
		return new MethodFlow(methodSig, reverseSource, reverseSink, isAlias, typeChecking, ignoreTypes, cutSubFields,
				constraints, isFinal, false);
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

	public boolean isFinal() {
		return isFinal;
	}

	public boolean isExcludedOnClear() {
		return excludedOnClear;
	}

	@Override
	public boolean isAlias(Taint t) {
		return isAlias(t, from);
	}

	@Override
	public MethodFlow replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (replacementMap == null)
			return this;
		return new MethodFlow(methodSig, from.replaceGaps(replacementMap), to.replaceGaps(replacementMap), isAlias,
				typeChecking, ignoreTypes, cutSubFields, constraints, isFinal, false);
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
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		MethodFlow that = (MethodFlow) o;
		return isAlias == that.isAlias && Objects.equals(from, that.from) && Objects.equals(to, that.to)
				&& Objects.equals(typeChecking, that.typeChecking) && Objects.equals(ignoreTypes, that.ignoreTypes)
				&& Objects.equals(cutSubFields, that.cutSubFields) && Arrays.equals(constraints, that.constraints)
				&& isFinal == that.isFinal;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), from, to, isAlias, typeChecking, ignoreTypes, cutSubFields,
				Arrays.hashCode(constraints), isFinal);
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
				if ("java.lang.Object[]".equals(from.getLastFieldType()))
					return true;
				if (to.isField() && !to.hasAccessPath())
					return true;
			}
			return false;
		}
		return ignoreTypes;
	}
}
