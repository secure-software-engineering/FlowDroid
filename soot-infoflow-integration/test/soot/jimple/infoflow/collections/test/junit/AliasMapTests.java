package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;

public class AliasMapTests extends FlowDroidTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.AliasMapTestCode";

    @Test(timeout = 30000)
    public void testMapPutGet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        IEntryPointCreator epc = new SequentialEntryPointCreator(Collections.singleton(epoint));
        infoflow.computeInfoflow(appPath, libPath, epc, sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testMapPutGet3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet5() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet6() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet7() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapClear1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testAccessAlias1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test //(timeout = 30000)
    public void testAccessAlias2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testAccessAlias3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testAccessAlias4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testNoAccessAsInactive1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
