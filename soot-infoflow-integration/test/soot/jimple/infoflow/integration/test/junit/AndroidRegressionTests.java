package soot.jimple.infoflow.integration.test.junit;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;
import java.util.*;

/**
 * Tests that uncovered a bug.
 */
public class AndroidRegressionTests extends BaseJUnitTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            return TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Could not initialize taint wrapper!");
        }
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
    }

    /**
     * Tests that the alias analysis correctly stops when an overwrite happens
     */
    @Test
    public void testFlowSensitivityWithOverwrite() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication("testAPKs/flowsensitiveOverwrite.apk");
        InfoflowResults results = app.runInfoflow("../soot-infoflow-android/SourcesAndSinks.txt");
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(2, results.getResultSet().size());
    }

    /**
     * Tests that StubDroid correctly narrows the type when the summary is in a superclass.
     * See also the comment in SummaryTaintWrapper#getSummaryDeclaringClass().
     */
    @Test
    public void testTypeHierarchyFromSummary() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication("testAPKs/TypeHierarchyTest.apk");
        InfoflowResults results = app.runInfoflow("../soot-infoflow-android/SourcesAndSinks.txt");
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1, results.getResultSet().size());
    }

    /**
     * Tests an app that uses the kotlin collections.
     * Expects four leaks:
     *   * From getDeviceId() in onCreate() to Log.d(String, String)
     *     in listFlow(String), mapFlow(String) and setFlow(String).
     *   * From new File in fileFlow() to Log.d(String, String) in fileFlow(String).
     */
    @Test
    public void testKotlinAppWithCollections() throws IOException {
        SetupApplication app = initApplication("testAPKs/KotlinCollectionApp.apk");

        // Make sure we find only one flow per method
        app.addResultsAvailableHandler((cfg, results) -> {
            Set<SootMethod> seenSet = new HashSet<>();
            for (DataFlowResult res : results.getResultSet()) {
                SootMethod sm = cfg.getMethodOf(res.getSink().getStmt());
                Assert.assertFalse(seenSet.contains(sm));
                seenSet.add(sm);
            }
        });

        // Add the sources and sinks
        List<String> ssinks = new ArrayList<>();
        ssinks.add("<android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE -> _SOURCE_");
        ssinks.add("<android.util.Log: int d(java.lang.String,java.lang.String)> -> _SINK_");
        ssinks.add("<kotlin.io.TextStreamsKt: java.util.List readLines(java.io.Reader)> -> _SOURCE_");

        InfoflowResults results = app.runInfoflow(PermissionMethodParser.fromStringList(ssinks));
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(4, results.getResultSet().size());
    }

    /**
     * Tests that the CallToReturnFunction does not pass on taints that were killed
     * by a taint wrapper that marked the method as exclusive.
     */
    @Test
    public void testMapClear() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication("testAPKs/MapClearTest.apk");
        InfoflowResults results = app.runInfoflow("../soot-infoflow-android/SourcesAndSinks.txt");
        Assert.assertEquals(0, results.size());
    }
}
