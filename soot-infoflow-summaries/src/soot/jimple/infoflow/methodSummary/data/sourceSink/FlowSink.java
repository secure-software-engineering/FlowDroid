package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;
import soot.jimple.infoflow.methodSummary.xml.XMLConstants;

/**
 * Representation of a flow sink.
 * 
 * @author Steven Arzt
 */
public class FlowSink extends AbstractFlowSinkSource implements Cloneable {

	protected final boolean taintSubFields;

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields, ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, accessPath, false, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields, GapDefinition gap, ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, accessPath, gap, false, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields, GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, accessPath, gap, matchStrict, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields, GapDefinition gap, Object userData, boolean matchStrict,
			ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, accessPath, gap, userData, matchStrict, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, boolean taintSubFields,
			ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, null, null, false, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, boolean taintSubFields, GapDefinition gap,
			ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, null, gap, null, false, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, int paramterIdx, String baseType, boolean taintSubFields, GapDefinition gap,
			Object userData, ConstraintType isConstrained) {
		super(type, paramterIdx, baseType, null, gap, userData, false, isConstrained);
		this.taintSubFields = taintSubFields;
	}

	public FlowSink(SourceSinkType type, String baseType, AccessPathFragment accessPath, boolean taintSubFields2,
			GapDefinition gap, boolean matchStrict, ConstraintType isConstrained) {
		super(type, -1, baseType, accessPath, gap, matchStrict, isConstrained);
		this.taintSubFields = taintSubFields2 || (accessPath != null && accessPath.length() > this.accessPath.length());
	}

	public boolean taintSubFields() {
		return taintSubFields;
	}

	/**
	 * Checks whether the current source or sink is coarser than the given one,
	 * i.e., if all elements referenced by the given source or sink are also
	 * referenced by this one
	 * 
	 * @param other The source or sink with which to compare the current one
	 * @return True if the current source or sink is coarser than the given one,
	 *         otherwise false
	 */
	@Override
	public boolean isCoarserThan(AbstractFlowSinkSource other) {
		return super.isCoarserThan(other) && other instanceof FlowSink && this.taintSubFields;
	}

	@Override
	public Map<String, String> xmlAttributes() {
		Map<String, String> res = super.xmlAttributes();
		res.put(XMLConstants.ATTRIBUTE_TAINT_SUB_FIELDS, taintSubFields() + "");
		return res;
	}

	@Override
	public String toString() {
		if (isGapBaseObject() && getGap() != null)
			return "Gap base for " + getGap().getSignature();

		String gapString = getGap() == null ? "" : "Gap " + getGap().getSignature() + " ";

		if (isParameter())
			return gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath)) + " | "
					+ taintSubFields();

		if (isField())
			return gapString + "Field" + (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath))
					+ " | " + taintSubFields();

		if (isReturn())
			return gapString + "Return" + (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath))
					+ " | " + taintSubFields();

		if (isCustom())
			return "CUSTOM " + gapString + "Parameter " + getParameterIndex()
					+ (accessPath == null ? "" : " " + AccessPathFragment.toString(accessPath));

		return "invalid sink";
	}

	@Override
	public int hashCode() {
		return super.hashCode() + (31 * (taintSubFields ? 1 : 0));
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;

		return this.taintSubFields == ((FlowSink) obj).taintSubFields;
	}

	/**
	 * Validates this flow sink
	 * 
	 * @param methodName The name of the containing method. This will be used to
	 *                   give more context in exception messages
	 */
	public void validate(String methodName) {
		if (getType() == SourceSinkType.GapBaseObject && getGap() == null)
			throw new InvalidFlowSpecificationException(
					"Gap base flows must always be linked with gaps. Offending method: " + methodName, this);
	}

	@Override
	public FlowSink replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowSink(type, parameterIdx, baseType, accessPath, taintSubFields, newGap, matchStrict,
				isConstrained);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowSink(type, parameterIdx, baseType, accessPath, taintSubFields, gap, userData, matchStrict,
				isConstrained);
	}

}
