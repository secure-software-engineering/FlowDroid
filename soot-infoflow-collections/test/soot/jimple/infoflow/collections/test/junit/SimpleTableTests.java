package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;

public class SimpleTableTests extends FlowDroidTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.SimpleTableTestCode";

    @Test(timeout = 30000)
    public void testTablePutGet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutGet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutGet3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutAll1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutAll2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableCellSet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableCellSet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
