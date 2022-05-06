package soot.jimple.infoflow.test.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class StaticTests extends soot.jimple.infoflow.test.junit.StaticTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow("", false, null);
	}

}
