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

public class ArrayTests extends soot.jimple.infoflow.test.junit.ArrayTests {
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
