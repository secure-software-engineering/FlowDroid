package soot.jimple.infoflow.test.methodSummary.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperVectorTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperVectorTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}
}
