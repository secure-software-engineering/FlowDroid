package soot.jimple.infoflow.test.junit.backward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class ExceptionTests extends soot.jimple.infoflow.test.junit.ExceptionTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

	@Override
	@Ignore("Exceptional unit graph incomplete for runtime exceptions")
	public void callMethodParamReturnTest2b() {
		// ignored
	}

}
