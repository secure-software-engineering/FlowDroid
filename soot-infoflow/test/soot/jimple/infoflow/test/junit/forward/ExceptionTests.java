package soot.jimple.infoflow.test.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class ExceptionTests extends soot.jimple.infoflow.test.junit.ExceptionTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
