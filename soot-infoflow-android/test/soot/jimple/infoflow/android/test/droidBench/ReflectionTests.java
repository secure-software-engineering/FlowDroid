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

import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.results.InfoflowResults;

public class ReflectionTests extends JUnitTests {
	
	private AnalysisConfigurationCallback enableReflectionCallback = new AnalysisConfigurationCallback() {
		
		@Override
		public void configureAnalyzer(InfoflowAndroidConfiguration config) {
			config.setEnableReflection(true);
		}
		
	};
	
	@Test(timeout=300000)
	public void runTestReflection1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestReflection2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection2.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestReflection3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection3.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestReflection4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection4.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestReflection5() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection5.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestReflection6() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection6.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	@Ignore
	public void runTestReflection7() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection7.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestReflection8() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection8.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestReflection9() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("Reflection/Reflection9.apk", null,
				enableReflectionCallback);
		Assert.assertEquals(1, res.size());
	}

}
