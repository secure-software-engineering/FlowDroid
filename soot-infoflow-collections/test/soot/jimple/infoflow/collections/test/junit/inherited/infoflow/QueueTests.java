package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;

public class QueueTests extends soot.jimple.infoflow.test.junit.QueueTests {
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
}
