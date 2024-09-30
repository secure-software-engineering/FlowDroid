package soot.jimple.infoflow.test.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class HeapTests extends soot.jimple.infoflow.test.junit.HeapTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
