package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class FieldSourceTest extends JUnitTests {

	@Test
	public void runTestFlowSensitivity1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldSource/FieldSourceTest.apk");
		Assert.assertNotNull(res);
	}
}
