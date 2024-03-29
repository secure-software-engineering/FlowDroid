package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;
import java.util.ArrayList;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;

public class QueueTests extends soot.jimple.infoflow.test.junit.QueueTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {

		AbstractInfoflow result = new CollectionInfoflow("", false, new DefaultBiDiICFGFactory());
		try {
			ArrayList<IMethodSummaryProvider> providers = new ArrayList<>();
			providers.add(new CollectionSummaryParser("stubdroidBased"));
			providers.add(new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR));
			PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);
			result.setTaintWrapper(new CollectionSummaryTaintWrapper(sp, TestConstantStrategy::new));
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return result;
	}
}
