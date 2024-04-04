package soot.jimple.infoflow.android.test.otherAPKs;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.results.InfoflowResults;

import java.io.IOException;

public class AlwaysShortenTests extends soot.jimple.infoflow.android.test.droidBench.JUnitTests {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_FORWARDS;
    }

    @Test(timeout = 300000)
    public void runTestAnonymousClass1Insensitive() throws IOException, XmlPullParserException {
        InfoflowResults res = analyzeAPKFile("Callbacks/AnonymousClass1.apk", null, config -> {
            config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.FlowInsensitive);
        });

        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size()); // loc + lat, but single parameter
        Assert.assertEquals(2, res.getResultSet().size());
    }
}
