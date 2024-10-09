package soot.jimple.infoflow.android.test.sourceToSinks;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.results.InfoflowResults;

public class SourceToSinksTest extends JUnitTests {

	private final File sourcesSinksFile = new File(getInfoflowAndroidRoot(),
			"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");

	/**
	 * very simple testcase
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest1() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink1.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	/**
	 * testing with different parameter in sinks
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest2() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink2.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(5, res.size());
	}

	/**
	 * testing with different parameter in sources
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest3() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink3.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	/**
	 * testing with a base in sinks
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest4() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink4.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	/**
	 * testing with a base in sources
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest5() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink5.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	/**
	 * a more complex testcase
	 * 
	 * @throws IOException
	 */
	@Test(timeout = 300000)
	public void runSourceToSinkTest6() throws IOException {
		InfoflowResults res = analyzeAPKFile(
				new File(getInfoflowAndroidRoot(), "testAPKs/SourceSinkDefinitions/SourceToSink6.apk"),
				sourcesSinksFile);
		Assert.assertNotNull(res);
		Assert.assertEquals(6, res.size());
	}
}
