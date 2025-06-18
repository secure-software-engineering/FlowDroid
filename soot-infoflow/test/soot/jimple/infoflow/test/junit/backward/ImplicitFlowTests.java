package soot.jimple.infoflow.test.junit.backward;

import org.junit.Ignore;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;

public class ImplicitFlowTests extends soot.jimple.infoflow.test.junit.ImplicitFlowTests {

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		return new BackwardsInfoflow(null, false, null);
	}

	@Ignore("When running backwards, we don't know that the method will be from a "
			+ "different class depending on the instantiation of the object that we don't "
			+ "see until further up in the code, i.e., later in the analysis")
	@Override
	public void dataClassSetterTest() {
		//
	}

}
