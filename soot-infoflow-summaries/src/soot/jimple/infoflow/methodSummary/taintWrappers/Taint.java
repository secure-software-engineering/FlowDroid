package soot.jimple.infoflow.methodSummary.taintWrappers;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;

/**
 * Class representing a tainted item during propagation
 * 
 * @author Steven Arzt
 *
 */
public class Taint extends FlowSink implements Cloneable {

	public Taint(SourceSinkType type, int paramterIdx, String baseType, boolean taintSubFields) {
		super(type, paramterIdx, baseType, taintSubFields);
	}

	public Taint(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields) {
		super(type, paramterIdx, baseType, accessPath, taintSubFields);
	}

	public Taint(SourceSinkType type, int paramterIdx, String baseType, AccessPathFragment accessPath,
			boolean taintSubFields, GapDefinition gap) {
		super(type, paramterIdx, baseType, accessPath, taintSubFields, gap);
	}

	@Override
	public Taint clone() {
		return new Taint(type, parameterIdx, baseType, accessPath, taintSubFields);
	}

}