package soot.jimple.infoflow.test.junit.backward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

@Ignore
public class FutureTests extends soot.jimple.infoflow.test.junit.FutureTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

}
