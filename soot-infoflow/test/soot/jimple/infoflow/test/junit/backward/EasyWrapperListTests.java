package soot.jimple.infoflow.test.junit.backward;

import java.io.IOException;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class EasyWrapperListTests extends soot.jimple.infoflow.test.junit.EasyWrapperListTests {

	public EasyWrapperListTests() throws IOException {
		super();
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

}
