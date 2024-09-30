package soot.jimple.infoflow.test.junit.backward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class HeapTests extends soot.jimple.infoflow.test.junit.HeapTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

	@Override
	@Ignore("PTS-Based aliasing")
	public void aliasPerformanceTestFIS() {
	}

}
