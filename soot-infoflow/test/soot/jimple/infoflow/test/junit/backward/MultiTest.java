package soot.jimple.infoflow.test.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class MultiTest extends soot.jimple.infoflow.test.junit.MultiTest {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

}
