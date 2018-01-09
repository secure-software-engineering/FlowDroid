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
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class FieldAndObjectSensitivityTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestFieldSensitivity1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/FieldSensitivity1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestFieldSensitivity2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/FieldSensitivity2.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestFieldSensitivity3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/FieldSensitivity3.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestFieldSensitivity4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/FieldSensitivity4.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestInheritedObjects1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/InheritedObjects1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestObjectSensitivity1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/ObjectSensitivity1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestObjectSensitivity2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("FieldAndObjectSensitivity/ObjectSensitivity2.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

}
