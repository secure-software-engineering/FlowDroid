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

import soot.jimple.infoflow.results.InfoflowResults;

@Ignore("Buggy, call graph problem")
public abstract class ReflectionICCTest extends JUnitTests {

	@Test(timeout = 300000)
	public void runTestActivityCommunication2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/ActivityCommunication2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestAllReflection() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/AllReflection.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyIntent() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyIntent.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyIntentReceive() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyIntentReceive.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlySMS() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlySMS.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyTelephony() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyTelephony.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyTelephony_Dynamic() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyTelephony_Dynamic.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyTelephony_Reverse() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyTelephony_Reverse.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestOnlyTelephony_Substring() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/OnlyTelephony_Substring.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout = 300000)
	public void runTestSharedPreferences1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Reflection_ICC/SharedPreferences1.apk");
		Assert.assertEquals(1, res.size());
	}
}
