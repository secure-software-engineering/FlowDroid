/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.results.InfoflowResults;

public abstract class AndroidSpecificTest extends JUnitTests {

	@Test // (timeout=300000)
	public void runTestApplicationModeling1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/ApplicationModeling1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestDirectLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/DirectLeak1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestInactiveActivity() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/InactiveActivity.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout = 300000)
	public void runTestLibrary2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/Library2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestLogNoLeak() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/LogNoLeak.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout = 300000)
	public void runTestObfuscation1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/Obfuscation1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestParcel1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/Parcel1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestPrivateDataLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/PrivateDataLeak1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestPrivateDataLeak2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/PrivateDataLeak2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	// not supported, would require taint tracking via files
	public void runTestPrivateDataLeak3() throws IOException {
		int expected = 2;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 1;

		InfoflowResults res = analyzeAPKFile("AndroidSpecific/PrivateDataLeak3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	public void runPublicAPIField1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/PublicAPIField1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runPublicAPIField2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/PublicAPIField2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runView1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific/View1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

}
