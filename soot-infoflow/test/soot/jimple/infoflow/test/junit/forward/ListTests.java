package soot.jimple.infoflow.test.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class ListTests extends soot.jimple.infoflow.test.junit.ListTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
