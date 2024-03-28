package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

/**
 * Definition of a point at which an existing data flow is killed
 * 
 * @author Steven Arzt
 *
 */
public class FlowClear extends AbstractFlowSinkSource implements Cloneable {

	public FlowClear(SourceSinkType type, String baseType, ConstraintType isConstrained) {
		super(type, -1, baseType, null, null, false, isConstrained);
	}

	public FlowClear(SourceSinkType type, String baseType, GapDefinition gap, ConstraintType isConstrained) {
		super(type, -1, baseType, null, null, gap, false, isConstrained);
	}

	public FlowClear(SourceSinkType type, String baseType, AccessPathFragment accessPath, ConstraintType isConstrained) {
		super(type, -1, baseType, accessPath, false, isConstrained);
	}

	public FlowClear(SourceSinkType type, String baseType, AccessPathFragment accessPath, GapDefinition gap, ConstraintType isConstrained) {
		super(type, -1, baseType, accessPath, gap, false, isConstrained);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
					 GapDefinition gap, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, accessPath, gap, false, isConstrained);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
					 GapDefinition gap, Object userData, ConstraintType isConstrained) {
		super(type, parameterIdx, baseType, accessPath, gap, userData, false, isConstrained);
	}

	@Override
	public FlowClear replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowClear(type, parameterIdx, baseType, accessPath, newGap, isConstrained);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowClear(type, parameterIdx, baseType, accessPath, gap, userData, isConstrained);
	}

}
