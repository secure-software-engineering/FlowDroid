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

	public FlowClear(SourceSinkType type, String baseType) {
		super(type, -1, baseType, null, null, false);
	}

	public FlowClear(SourceSinkType type, String baseType, GapDefinition gap) {
		super(type, -1, baseType, null, null, gap, false);
	}

	public FlowClear(SourceSinkType type, String baseType, AccessPathFragment accessPath) {
		super(type, -1, baseType, accessPath, false);
	}

	public FlowClear(SourceSinkType type, String baseType, AccessPathFragment accessPath, GapDefinition gap) {
		super(type, -1, baseType, accessPath, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType) {
		super(type, parameterIdx, baseType, null, null, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, GapDefinition gap) {
		super(type, parameterIdx, baseType, null, null, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath) {
		super(type, parameterIdx, baseType, accessPath, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
			GapDefinition gap) {
		super(type, parameterIdx, baseType, accessPath, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, AccessPathFragment accessPath,
			GapDefinition gap, Object userData) {
		super(type, parameterIdx, baseType, accessPath, gap, userData, false);
	}

	@Override
	public FlowClear replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowClear(type, parameterIdx, baseType, accessPath, newGap);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowClear(type, parameterIdx, baseType, accessPath, gap, userData);
	}

}
