package soot.jimple.infoflow.test.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class FutureTests extends soot.jimple.infoflow.test.junit.FutureTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow("", false, null);
	}

}
