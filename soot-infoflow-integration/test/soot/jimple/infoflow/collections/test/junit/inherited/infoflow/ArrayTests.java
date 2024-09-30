package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.config.PreciseCollectionStrategy;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;

public class ArrayTests extends soot.jimple.infoflow.test.junit.ArrayTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		AbstractInfoflow result = new Infoflow(null, false, new DefaultBiDiICFGFactory());
		result.getConfig().setPreciseCollectionTracking(PreciseCollectionStrategy.CONSTANT_MAP_SUPPORT);
		try {
			ArrayList<IMethodSummaryProvider> providers = new ArrayList<>();
			providers.add(new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR));
			PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);
			result.setTaintWrapper(new SummaryTaintWrapper(sp).setContainerStrategyFactory(TestConstantStrategy::new));
		} catch (Exception e) {
			throw new RuntimeException();
		}

		return result;
	}

	@Test(timeout = 300000)
	public void arrayReadWritePos1Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ArrayTestCode: void concreteWriteReadDiffPosTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise
		negativeCheckInfoflow(infoflow);
	}
}
