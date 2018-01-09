package soot.jimple.infoflow.android.test.sourceToSinks;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class SourceToSinksTest extends JUnitTests{
	
	InfoflowResults res;
	
	/**
	 * very simple testcase 
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest1()  throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink1.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	/**
	 * testing with different parameter in sinks
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest2() throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink2.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(5, res.size());
	}
	
	/**
	 * testing with different parameter in sources
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest3() throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink3.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	/**
	 * testing with a base in sinks
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest4() throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink4.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	/**
	 * testing with a base in sources
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest5() throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink5.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}
	
	/**
	 * a more complex testcase
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout=300000)
	public void runSourceToSinkTest6() throws IOException, XmlPullParserException {
		res = null;
		res = analyzeAPKFile("testAPKs/SourceSinkDefinitions/SourceToSink6.apk",
				"testAPKs/SourceSinkDefinitions/sourcesAndSinks.xml");
		Assert.assertNotNull(res);
		Assert.assertEquals(6, res.size());
	}
}
