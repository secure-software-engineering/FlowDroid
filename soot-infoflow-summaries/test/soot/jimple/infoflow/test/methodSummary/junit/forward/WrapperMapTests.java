package soot.jimple.infoflow.test.methodSummary.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperMapTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperMapTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}
}
