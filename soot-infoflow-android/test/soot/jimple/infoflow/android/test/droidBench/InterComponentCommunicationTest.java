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
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class InterComponentCommunicationTest extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestActivityCommunication1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication4.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestActivityCommunication5() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication5.apk", 
				"iccta_testdata_ic3_results/edu.mit.icc_intent_component_name_1.txt");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication6() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication6.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication7() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication7.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestActivityCommunication8() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication8.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestBroadcastTaintAndLeak1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/BroadcastTaintAndLeak1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestComponentNotInManifest1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ComponentNotInManifest1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestEventOrdering1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/EventOrdering1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestIntentSink1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/IntentSink1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore("startActivity() is no longer a sink")
	public void runTestIntentSink2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/IntentSink2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	@Ignore
	public void runTestIntentSource1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/IntentSource1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestServiceCommunication1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ServiceCommunication1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestSharedPreferences1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/SharedPreferences1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore		// we do not support interleaved component executions yet
	public void runTestSingletons1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/Singletons1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestUnresolvableIntent1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/UnresolvableIntent1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

}
