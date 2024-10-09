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

import soot.jimple.infoflow.InfoflowConfiguration.CallbackSourceMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.results.InfoflowResults;

public abstract class LifecycleTest extends JUnitTests {

	@Test(timeout = 300000)
	public void runTestActivityEventSequence1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityEventSequence1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityEventSequence2() throws IOException {
		// Quote from Steven's Thesis:
		// "This test case was contributed by a different research group. We were unable
		// to reproduce the data flow
		// claimed in the test case's documentation during our tests with the app. We
		// therefore do not count a false
		// positive even though FLOWDROID does not find the proclaimed leak."
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityEventSequence2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityEventSequence3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityEventSequence3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityLifecycle1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityLifecycle2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityLifecycle2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityLifecycle3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityLifecycle3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivityLifecycle4() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivityLifecycle4.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestActivitySavedState1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ActivitySavedState1.apk", null,
				new AnalysisConfigurationCallback() {

					@Override
					public void configureAnalyzer(InfoflowAndroidConfiguration config) {
						config.getSourceSinkConfig().setEnableLifecycleSources(true);
						config.getSourceSinkConfig().setCallbackSourceMode(CallbackSourceMode.AllParametersAsSources);
					}

				});
		Assert.assertNotNull(res);
		// DroidBench does expect 1 leak here but due to the configuration
		// above, two is actually the ground truth here.
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout = 300000)
	public void runTestApplicationLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ApplicationLifecycle1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestApplicationLifecycle2() throws IOException {
		int expected = 2;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 1;
		InfoflowResults res = analyzeAPKFile("Lifecycle/ApplicationLifecycle2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	public void runTestApplicationLifecycle3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ApplicationLifecycle3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestAsynchronousEventOrdering1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/AsynchronousEventOrdering1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestBroadcastReceiverLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/BroadcastReceiverLifecycle1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestBroadcastReceiverLifecycle2() throws IOException {
		int expected = 1;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 2;
		InfoflowResults res = analyzeAPKFile("Lifecycle/BroadcastReceiverLifecycle2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	public void runTestBroadcastReceiverLifecycle3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/BroadcastReceiverLifecycle3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestEventOrdering1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/EventOrdering1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestFragmentLifecycle1() throws IOException {
		int expected = 0;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 1;
		InfoflowResults res = analyzeAPKFile("Lifecycle/FragmentLifecycle1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	// not supported yet
	public void runTestFragmentLifecycle2() throws IOException {
		int expected = 1;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 0;
		InfoflowResults res = analyzeAPKFile("Lifecycle/FragmentLifecycle2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)

	public void runTestServiceEventSequence1() throws IOException {
		int expected = 1;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 0;
		InfoflowResults res = analyzeAPKFile("Lifecycle/ServiceEventSequence1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	public void runTestServiceEventSequence2() throws IOException {
		int expected = 1;
		if (mode == TestResultMode.FLOWDROID_BACKWARDS || mode == TestResultMode.FLOWDROID_FORWARDS)
			expected = 0;
		InfoflowResults res = analyzeAPKFile("Lifecycle/ServiceEventSequence2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(expected, res.size());
	}

	@Test(timeout = 300000)
	public void runTestServiceEventSequence3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ServiceEventSequence3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestServiceLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ServiceLifecycle1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestServiceLifecycle2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/ServiceLifecycle2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestSharedPreferenceChanged1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle/SharedPreferenceChanged1.apk", null,
				new AnalysisConfigurationCallback() {

					@Override
					public void configureAnalyzer(InfoflowAndroidConfiguration config) {
						config.getSourceSinkConfig().setCallbackSourceMode(CallbackSourceMode.AllParametersAsSources);
					}

				});
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size()); // write to shared preference + leak
											// from there as individual leaks
	}

}
