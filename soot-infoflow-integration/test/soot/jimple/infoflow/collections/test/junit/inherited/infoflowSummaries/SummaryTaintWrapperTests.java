package soot.jimple.infoflow.collections.test.junit.inherited.infoflowSummaries;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.config.PreciseCollectionStrategy;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.options.Options;

public class SummaryTaintWrapperTests extends soot.jimple.infoflow.test.methodSummary.junit.SummaryTaintWrapperTests {
	@Override
	protected AbstractInfoflow createInfoflowInstance() {

		Infoflow inf = new Infoflow(null, false, new DefaultBiDiICFGFactory());
		inf.getConfig().setPreciseCollectionTracking(PreciseCollectionStrategy.CONSTANT_MAP_SUPPORT);
		return inf;
	}

	@Override
	protected IInfoflow initInfoflow() throws FileNotFoundException, XMLStreamException {
		IInfoflow result = createInfoflowInstance();
		result.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		IInfoflowConfig testConfig = new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				List<String> excludeList = new ArrayList<>();
				excludeList.add("soot.jimple.infoflow.test.methodSummary.ApiClass");
				excludeList.add("soot.jimple.infoflow.test.methodSummary.GapClass");
				Options.v().set_exclude(excludeList);

				List<String> includeList = new ArrayList<>();
				includeList.add("soot.jimple.infoflow.test.methodSummary.UserCodeClass");
				Options.v().set_include(includeList);

				Options.v().set_no_bodies_for_excluded(true);
				Options.v().set_allow_phantom_refs(true);
				Options.v().set_ignore_classpath_errors(true);
			}

		};
		result.setSootConfig(testConfig);

		try {
			ArrayList<IMethodSummaryProvider> providers = new ArrayList<>();
			providers.add(new EagerSummaryProvider(new File("../soot-infoflow-summaries/testSummaries")));
			providers.add(new EagerSummaryProvider(new File("../soot-infoflow-summaries/summariesManual")));
			PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);
			result.setTaintWrapper(new SummaryTaintWrapper(sp).setContainerStrategyFactory(TestConstantStrategy::new));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return result;
	}

	@Test(timeout = 30000)
	public void iterativeApplyIsOverapproximation() {
		// TODO: move final attribute toward upstream
		testNoFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void iterativeApplyIsOverapproximation()>");
	}

	@Override
	@Ignore("Not supported with the auto-generated method summaries")
	public void streamWriteReadTest() {
		//
	}

}
