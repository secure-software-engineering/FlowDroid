package soot.jimple.infoflow.test.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class SourceSinkTests extends soot.jimple.infoflow.test.junit.SourceSinkTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

}
