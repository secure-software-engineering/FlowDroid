package soot.jimple.infoflow.test.methodSummary.junit;

import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public interface TaintWrapperFactory {
	public ITaintPropagationWrapper newTaintWrapper() throws Exception;
}
