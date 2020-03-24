package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

/**
 * Representation of a flow source
 * 
 * @author Steven Arzt
 */
public class FlowSource extends AbstractFlowSinkSource implements Cloneable {

	public FlowSource(SourceSinkType type, String baseType) {
		super(type, -1, baseType, null, null, false);
	}

	public FlowSource(SourceSinkType type, String baseType, GapDefinition gap) {
		super(type, -1, baseType, null, gap, null, false);
	}

	public FlowSource(SourceSinkType type, String baseType, GapDefinition gap, boolean matchStrict) {
		super(type, -1, baseType, null, gap, null, matchStrict);
	}

	public FlowSource(SourceSinkType type, String baseType, AccessPathFragment accessPath) {
		super(type, -1, baseType, accessPath, false);
	}

	public FlowSource(SourceSinkType type, String baseType, AccessPathFragment accessPath, GapDefinition gap) {
		super(type, -1, baseType, accessPath, gap, false);
	}

	public FlowSource(SourceSinkType type, String baseType, AccessPathFragment accessPath, GapDefinition gap,
			boolean matchStrict) {
		super(type, -1, baseType, accessPath, gap, matchStrict);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType) {
		super(type, parameterIdx, baseType, null, null, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, GapDefinition gap) {
		super(type, parameterIdx, baseType, null, gap, null, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath) {
		super(type, parameterIdx, baseType, accessPath, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
			GapDefinition gap) {
		super(type, parameterIdx, baseType, accessPath, gap, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
			GapDefinition gap, boolean matchStrict) {
		super(type, parameterIdx, baseType, accessPath, gap, matchStrict);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
			GapDefinition gap, Object userData, boolean matchStrict) {
		super(type, parameterIdx, baseType, accessPath, gap, userData, matchStrict);
	}

	@Override
	public String toString() {
		String gapString = getGap() == null ? "" : "Gap " + getGap().getSignature() + " ";

		if (isParameter())
			return gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

		if (isField())
			return gapString + "Field" + (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

		if (isThis())
			return "THIS";

		if (isReturn() && gap != null)
			return "Return value of gap " + gap.getSignature()
					+ (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

		if (isCustom())
			return "CUSTOM " + gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

		return "<unknown>";
	}

	/**
	 * Validates this flow source
	 * 
	 * @param methodName The name of the containing method. This will be used to
	 *                   give more context in exception messages
	 */
	public void validate(String methodName) {
		if (getType() == SourceSinkType.Return && getGap() == null)
			throw new InvalidFlowSpecificationException(
					"Return values cannot be sources. Offending method: " + methodName, this);
	}

	@Override
	public FlowSource replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowSource(type, parameterIdx, baseType, accessPath, newGap, matchStrict);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowSource(type, parameterIdx, baseType, accessPath, gap, userData, matchStrict);
	}

}
