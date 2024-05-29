package soot.jimple.infoflow.test.methodSummary.junit;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.test.junit.ListTests;

public abstract class WrapperListTests extends ListTests {

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

}