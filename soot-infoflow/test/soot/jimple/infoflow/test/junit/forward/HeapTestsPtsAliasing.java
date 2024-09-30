package soot.jimple.infoflow.test.junit.forward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class HeapTestsPtsAliasing extends soot.jimple.infoflow.test.junit.HeapTestsPtsAliasing {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new Infoflow(null, false, null);
	}

}
