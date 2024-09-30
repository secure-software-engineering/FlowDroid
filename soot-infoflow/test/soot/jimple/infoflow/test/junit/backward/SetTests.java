package soot.jimple.infoflow.test.junit.backward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class SetTests extends soot.jimple.infoflow.test.junit.SetTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

	@Ignore("timeout")
	public void containsTest() {
	}

	@Ignore("timeout")
	public void concreteLinkedSetPos0Test() {
	}

	@Ignore("timeout")
	public void concreteLinkedSetPos1Test() {
	}

	@Ignore("timeout")
	public void setIteratorTest() {
	}

	@Ignore("timeout")
	public void concreteHashSetTest() {
	}

	@Ignore("timeout")
	public void setTest() {
	}

}
