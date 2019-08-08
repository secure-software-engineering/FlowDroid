package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.Arrays;
import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;

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
		super(type, -1, baseType, null, null, gap, false);
	}

	public FlowSource(SourceSinkType type, String baseType, GapDefinition gap, boolean matchStrict) {
		super(type, -1, baseType, null, null, gap, matchStrict);
	}

	public FlowSource(SourceSinkType type, String baseType, String[] fields, String[] fieldTypes) {
		super(type, -1, baseType, fields, fieldTypes, false);
	}

	public FlowSource(SourceSinkType type, String baseType, String[] fields, String[] fieldTypes, GapDefinition gap) {
		super(type, -1, baseType, fields, fieldTypes, gap, false);
	}

	public FlowSource(SourceSinkType type, String baseType, String[] fields, String[] fieldTypes, GapDefinition gap,
			boolean matchStrict) {
		super(type, -1, baseType, fields, fieldTypes, gap, matchStrict);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType) {
		super(type, parameterIdx, baseType, null, null, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, GapDefinition gap) {
		super(type, parameterIdx, baseType, null, null, gap, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes) {
		super(type, parameterIdx, baseType, fields, fieldTypes, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes,
			GapDefinition gap) {
		super(type, parameterIdx, baseType, fields, fieldTypes, gap, false);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes,
			GapDefinition gap, boolean matchStrict) {
		super(type, parameterIdx, baseType, fields, fieldTypes, gap, matchStrict);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes,
			GapDefinition gap, Object userData, boolean matchStrict) {
		super(type, parameterIdx, baseType, fields, fieldTypes, gap, userData, matchStrict);
	}

	@Override
	public String toString() {
		String gapString = getGap() == null ? "" : "Gap " + getGap().getSignature() + " ";

		if (isParameter())
			return gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + Arrays.toString(accessPath));

		if (isField())
			return gapString + "Field" + (accessPath == null ? "" : " " + Arrays.toString(accessPath));

		if (isThis())
			return "THIS";

		if (isReturn() && gap != null)
			return "Return value of gap " + gap.getSignature();

		if (isCustom())
			return "CUSTOM " + gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + Arrays.toString(accessPath));

		return "<unknown>";
	}

	/**
	 * Validates this flow source
	 * 
	 * @param methodName
	 *            The name of the containing method. This will be used to give more
	 *            context in exception messages
	 */
	public void validate(String methodName) {
		if (getType() == SourceSinkType.Return && getGap() == null)
			throw new RuntimeException("Return values cannot be sources. " + "Offending method: " + methodName);
	}

	@Override
	public FlowSource replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowSource(type, parameterIdx, baseType, accessPath, accessPathTypes, newGap, matchStrict);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowSource(type, parameterIdx, baseType, accessPath, accessPathTypes, gap, userData, matchStrict);
	}

}
