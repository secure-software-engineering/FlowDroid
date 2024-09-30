package soot.jimple.infoflow.test.methodSummary.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class WrapperSetTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperSetTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}
}
