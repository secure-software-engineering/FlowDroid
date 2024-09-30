package soot.jimple.infoflow.test.junit.forward;

import java.io.IOException;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class EasyWrapperListTests extends soot.jimple.infoflow.test.junit.EasyWrapperListTests {

	public EasyWrapperListTests() throws IOException {
		super();
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
