package soot.jimple.infoflow.methodSummary.data.sourceSink;

import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;

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

	public FlowClear(SourceSinkType type, String baseType, String[] fields, String[] fieldTypes) {
		super(type, -1, baseType, fields, fieldTypes, false);
	}

	public FlowClear(SourceSinkType type, String baseType, String[] fields, String[] fieldTypes, GapDefinition gap) {
		super(type, -1, baseType, fields, fieldTypes, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType) {
		super(type, parameterIdx, baseType, null, null, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, GapDefinition gap) {
		super(type, parameterIdx, baseType, null, null, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes) {
		super(type, parameterIdx, baseType, fields, fieldTypes, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes,
			GapDefinition gap) {
		super(type, parameterIdx, baseType, fields, fieldTypes, gap, false);
	}

	public FlowClear(SourceSinkType type, int parameterIdx, String baseType, String[] fields, String[] fieldTypes,
			GapDefinition gap, Object userData) {
		super(type, parameterIdx, baseType, fields, fieldTypes, gap, userData, false);
	}

	@Override
	public FlowClear replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (gap == null)
			return this;
		GapDefinition newGap = replacementMap.get(gap.getID());
		if (newGap == null)
			return this;
		return new FlowClear(type, parameterIdx, baseType, accessPath, accessPathTypes, newGap);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FlowClear(type, parameterIdx, baseType, accessPath, accessPathTypes, gap, userData);
	}

}
