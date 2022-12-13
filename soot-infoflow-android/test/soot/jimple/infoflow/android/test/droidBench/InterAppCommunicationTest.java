package soot.jimple.infoflow.android.test.droidBench;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.results.InfoflowResults;

import java.io.IOException;

@Ignore("Buggy, call graph problem")
public abstract class InterAppCommunicationTest extends JUnitTests {
    @Test(timeout=300000)
    public void runTestEchoer() throws IOException, XmlPullParserException {
        int expected = 3;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Echoer.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestSendSMS() throws IOException, XmlPullParserException {
        int expected = 3;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 2;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/SendSMS.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestStartActivityForResult1() throws IOException, XmlPullParserException {
        int expected = 6;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 2;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/StartActivityForResult1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestCollector() throws IOException, XmlPullParserException {
        int expected = 2;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Collector/Collector.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestDeviceId_Broadcast1() throws IOException, XmlPullParserException {
        int expected = 1;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Device Id leakage/DeviceId_Broadcast1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestDeviceId_contentProvider1() throws IOException, XmlPullParserException {
        int expected = 1;
        // TODO: where are the false positives from?https://github.com/secure-software-engineering/DroidBench

        if (mode == TestResultMode.FLOWDROID_BACKWARDS)
            expected = 2;
        else if (mode == TestResultMode.FLOWDROID_FORWARDS) {
            expected = 3;
        }
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Device Id leakage/DeviceId_contentProvider1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestDeviceId_OrderedIntent1() throws IOException, XmlPullParserException {
        int expected = 1;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Device Id leakage/DeviceId_OrderedIntent1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestDeviceId_Service() throws IOException, XmlPullParserException {
        int expected = 1;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Device Id leakage/DeviceId_Service1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestLocation1() throws IOException, XmlPullParserException {
        int expected = 2;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Location leakage/Location1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestLocation1_Broadcast() throws IOException, XmlPullParserException {
        int expected = 2;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Location leakage/Location_Broadcast1.apk");
        Assert.assertEquals(expected, res.size());
    }

    @Test(timeout=300000)
    public void runTestLocation_Service1() throws IOException, XmlPullParserException {
        int expected = 2;
        if (mode != TestResultMode.DROIDBENCH)
            expected = 0;
        InfoflowResults res = analyzeAPKFile("InterAppCommunication/Location leakage/Location_Service1.apk");
        Assert.assertEquals(expected, res.size());
    }
}
