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

import soot.jimple.infoflow.InfoflowConfiguration.CallbackSourceMode;
import soot.jimple.infoflow.InfoflowConfiguration.LayoutMatchingMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.results.InfoflowResults;

public class CallbackTest extends JUnitTests {

	@Test(timeout = 300000)
	public void runTestAnonymousClass1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/AnonymousClass1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size()); // loc + lat, but single parameter
	}

	@Test(timeout = 300000)
	public void runTestButton1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Button1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestButton2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Button2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(4, res.size()); // 3 + (strong alias update not
											// supported)
	}

	@Test(timeout = 300000)
	public void runTestButton3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Button3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestButton4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Button4.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestButton5() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Button5.apk", null, new AnalysisConfigurationCallback() {

			@Override
			public void configureAnalyzer(InfoflowAndroidConfiguration config) {
				config.getSourceSinkConfig().setLayoutMatchingMode(LayoutMatchingMode.MatchAll);
				config.getSourceSinkConfig().setCallbackSourceMode(CallbackSourceMode.AllParametersAsSources);
			}
		});

		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestLocationLeak1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/LocationLeak1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout = 300000)
	public void runTestLocationLeak2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/LocationLeak2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout = 300000)
	public void runTestLocationLeak3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/LocationLeak3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size()); // loc + lat, but single parameter
	}

	@Test(timeout = 300000)
	public void runTestMethodOverride1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/MethodOverride1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestMultiHandlers1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/MultiHandlers1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOrdering1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Ordering1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout = 300000)
	public void runTestRegisterGlobal1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/RegisterGlobal1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestRegisterGlobal2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/RegisterGlobal2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	@Ignore // Unregistering callbacks is not supported
	public void runTestUnregister1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Callbacks/Unregister1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

}
