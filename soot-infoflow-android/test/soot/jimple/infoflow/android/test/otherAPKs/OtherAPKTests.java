package soot.jimple.infoflow.android.test.otherAPKs;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.results.InfoflowResults;

public class OtherAPKTests extends JUnitTests {

	@Test
	public void runTest1() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		InfoflowResults res = analyzeAPKFile(new File(rootDir, "testAPKs/9458cfb51c90130938abcef7173c3f6d44a02720.apk"),
				false, false, false);
		Assert.assertNotNull(res);
		Assert.assertTrue(res.size() > 0);
	}

	@Ignore("APK file missing")
	@Test
	public void runTest2() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		InfoflowResults res = analyzeAPKFile(new File(rootDir, "testAPKs/enriched1.apk"), false, false, false);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runReturnParameterTest() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		InfoflowResults res = analyzeAPKFile(new File(rootDir, "testAPKs/ReturnParameterTest.apk"), false, false,
				false);
		Assert.assertTrue(res == null || res.isEmpty());
	}
}
