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

	public static final int ANY_PARAMETER = -2;

	public FlowSource(SourceSinkType type, String baseType, ConstraintType isConstrained) {
		super(type, -1, baseType, null, null, false, isConstrained);
	}

	public FlowSource(SourceSinkType type, String baseType, GapDefinition gap, ConstraintType isConstrained) {
		super(type, -1, baseType, null, gap, null, false, isConstrained);
	}

	public FlowSource(SourceSinkType type, String baseType, GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		super(type, -1, baseType, null, gap, null, matchStrict, isConstrained);
	}

	public FlowSource(SourceSinkType type, String baseType, AccessPathFragment apf, GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		super(type, baseType, apf, gap, matchStrict, isConstrained);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, null, null, false, isConstrained);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, GapDefinition gap, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, null, gap, null, false, isConstrained);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
					  GapDefinition gap, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, accessPath, gap, false, isConstrained);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
					  GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, accessPath, gap, matchStrict, isConstrained);
	}

	public FlowSource(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
					  GapDefinition gap, Object userData, boolean matchStrict, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, accessPath, gap, userData, matchStrict, isConstrained);
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

		if (isReturn())
			return "Return value" + (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

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
		return new FlowSource(type, parameterIdx, baseType, accessPath, newGap, matchStrict, isConstrained);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowSource(type, parameterIdx, baseType, accessPath, gap, userData, matchStrict, isConstrained);
	}

}
