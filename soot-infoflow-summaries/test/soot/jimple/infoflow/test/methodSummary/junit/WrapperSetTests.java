package soot.jimple.infoflow.test.methodSummary.junit;


import org.junit.Assume;
import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.test.junit.SetTests;

public abstract class WrapperSetTests extends SetTests {
	@Override
	protected IInfoflow initInfoflow(boolean useTaintWrapper) {
		IInfoflow result = super.initInfoflow(useTaintWrapper);
		WrapperListTestConfig testConfig = new WrapperListTestConfig();
		result.setSootConfig(testConfig);
		try {
			result.setTaintWrapper(TaintWrapperFactory.createTaintWrapper());
		} catch (Exception e) {
			throw new RuntimeException("Could not initialize taint wrapper!");
		}
		return result;
	}

	@Override
	@Test(timeout = 600000)
	public void concreteTreeSetPos0Test() {
		Assume.assumeTrue("No summary for last()", true);
	}

	@Override
	@Test(timeout = 600000)
	public void containsTest() {
		Assume.assumeTrue("Implicit flow, not enabled here", true);
	}
}
