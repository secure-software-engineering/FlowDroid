package soot.jimple.infoflow.collections.test.junit.inherited.infoflowSummaries;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.XMLSummaryProvider;
import soot.options.Options;

import javax.xml.stream.XMLStreamException;

public class SummaryTaintWrapperTests extends soot.jimple.infoflow.test.methodSummary.junit.SummaryTaintWrapperTests {
    @Override
    protected AbstractInfoflow createInfoflowInstance() {

        return new CollectionInfoflow("", false, new DefaultBiDiICFGFactory());
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
            ArrayList<IMethodSummaryProvider> providers = new ArrayList();
            providers.add(new CollectionSummaryParser(new File("stubdroidBased")));
            providers.add(new EagerSummaryProvider("../soot-infoflow-summaries/testSummaries/"));
            providers.add(new EagerSummaryProvider("summariesManual"));
            PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);
            result.setTaintWrapper(new CollectionSummaryTaintWrapper(sp, TestConstantStrategy::new));
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return result;
    }

    @Test(timeout = 30000)
    public void iterativeApplyIsOverapproximation() {
        // TODO: move final attribute toward upstream
        testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void iterativeApplyIsOverapproximation()>");
    }
}
