package soot.jimple.infoflow.collections.data;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.summary.ImplicitLocation;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;

public class IndexConstraint extends FlowConstraint {
	private final ImplicitLocation loc;

	public IndexConstraint(SourceSinkType type, int paramIdx, String baseType, AccessPathFragment accessPathFragment,
			ImplicitLocation loc) {
		super(type, paramIdx, baseType, accessPathFragment);
		this.loc = loc;
	}

	@Override
	public boolean isIndexBased() {
		return true;
	}

	@Override
	public ImplicitLocation getImplicitLocation() {
		assert getType() == SourceSinkType.Implicit;
		return loc;
	}
}
