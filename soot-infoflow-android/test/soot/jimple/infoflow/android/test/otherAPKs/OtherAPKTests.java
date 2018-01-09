package soot.jimple.infoflow.android.test.otherAPKs;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class OtherAPKTests extends JUnitTests {
	
	@Test
	public void runTest1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile
				("testAPKs/9458cfb51c90130938abcef7173c3f6d44a02720.apk", false, false, false);
		Assert.assertNotNull(res);
		Assert.assertTrue(res.size() > 0);
	}

	@Test
	public void runTest2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile
				("testAPKs/enriched1.apk", false, false, false);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test
	public void runReturnParameterTest() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile
				("testAPKs/ReturnParameterTest.apk", false, false, false);
		Assert.assertTrue(res == null || res.isEmpty());
	}

}
