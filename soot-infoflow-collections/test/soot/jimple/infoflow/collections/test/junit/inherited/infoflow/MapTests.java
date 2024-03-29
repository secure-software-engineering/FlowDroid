package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;

public class MapTests extends soot.jimple.infoflow.test.junit.MapTests {
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

	@Test(timeout = 300000)
	public void mapPos0Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MapTestCode: void writeReadPos0Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise \o/
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void concreteMapPos1Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadPos1Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise \o/
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void concreteNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadNegativeTest()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
}