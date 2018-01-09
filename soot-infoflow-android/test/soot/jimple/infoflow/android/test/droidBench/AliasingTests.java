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

public class AliasingTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestFlowSensitivity1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Aliasing/FlowSensitivity1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}
	
	@Test(timeout=300000)
	@Ignore // not yet supported
	public void runTestMerge1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Aliasing/Merge1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestSimpleAliasing1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Aliasing/SimpleAliasing1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestStrongUpdate1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Aliasing/StrongUpdate1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

}
