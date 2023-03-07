package soot.jimple.infoflow.integration.test.junit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.util.Chain;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class AndroidRiverTests extends RiverJUnitTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            return TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Could not initialized Taintwrapper:");
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        commonSetup();
    }

    @Test(timeout = 300000)
    public void conditionalTestApk() throws IOException {
        // The test apk has two java.io.OutputStream: void write(byte[]) sinks.
        // One is located in the KeepFlow activity and is a stream to the internet.
        // The other one is in the DiscardFlow activity and is a ByteArrayOutputStream.
        SetupApplication app = initApplication("testAPKs/conditionalTest.apk");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(1, results.size());

        // Check that the flow is in the right activity
        SootMethod sm = Scene.v().grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void onCreate(android.os.Bundle)>");
        Chain<Unit> units = sm.getActiveBody().getUnits();
        for (DataFlowResult result : results.getResultSet())
            Assert.assertTrue(Arrays.stream(result.getSource().getPath()).allMatch(units::contains));
    }
}
