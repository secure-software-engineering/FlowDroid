package soot.jimple.infoflow.test.junit.forward;

import java.io.IOException;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class EasyWrapperTests extends soot.jimple.infoflow.test.junit.EasyWrapperTests {

	public EasyWrapperTests() throws IOException {
		super();
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
