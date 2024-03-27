package soot.jimple.infoflow.collections.test.junit;

import java.io.File;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.data.ContainerContext;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class ListAddAllItselfTests extends FlowDroidTests {


    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            CollectionSummaryParser sp = new CollectionSummaryParser(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            CollectionSummaryTaintWrapper sbtw = new CollectionSummaryTaintWrapper(sp, TestConstantStrategy::new) {
                @Override
                public void initialize(InfoflowManager manager) {
                    super.initialize(manager);
                    this.containerStrategy = new TestConstantStrategy(manager) {
                        @Override
                        public ContainerContext getNextPosition(Value value, Stmt stmt) {
                            if (stmt.toString().contains("addAll("))
                                return new IntervalContext(3);

                            if (stmt.toString().contains("add("))
                                return new IntervalContext(0);

                            return UnknownContext.v();
                        }
                    };
                }
            };

            return sbtw;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.ListAddAllItselfTestCode";

    @Test(timeout = 30000)
    public void testListAllAllItselfFiniteLoop1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
