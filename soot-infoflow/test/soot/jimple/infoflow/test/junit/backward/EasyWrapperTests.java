package soot.jimple.infoflow.test.junit.backward;

import java.io.IOException;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class EasyWrapperTests extends soot.jimple.infoflow.test.junit.EasyWrapperTests {

	public EasyWrapperTests() throws IOException {
		super();
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

}
