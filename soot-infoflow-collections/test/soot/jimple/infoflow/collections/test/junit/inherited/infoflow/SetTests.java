package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;

import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;

public class SetTests extends soot.jimple.infoflow.test.junit.SetTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {

		AbstractInfoflow result = new CollectionInfoflow("", false, new DefaultBiDiICFGFactory());
		try {
			CollectionSummaryParser sp = new CollectionSummaryParser(new File("stubdroidBased"));
			sp.loadAdditionalSummaries("summariesManual");
			result.setTaintWrapper(new CollectionSummaryTaintWrapper(sp, TestConstantStrategy::new));
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return result;
	}

	@Test(timeout = 600000)
	public void containsTest() {
		// no implicit flows
	}

	@Test(timeout = 600000)
	public void concreteTreeSetPos0Test() {
		// No TreeSet.last() summary
	}
}
