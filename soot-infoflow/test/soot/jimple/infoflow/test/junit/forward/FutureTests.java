package soot.jimple.infoflow.test.junit.forward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

@Ignore
public class FutureTests extends soot.jimple.infoflow.test.junit.FutureTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
