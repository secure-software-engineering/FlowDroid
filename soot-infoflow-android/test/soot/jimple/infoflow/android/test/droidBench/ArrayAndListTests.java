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

public class ArrayAndListTests extends JUnitTests {
		
	@Test(timeout=300000)
	@Ignore
	public void runTestArrayAccess1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayAccess1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestArrayAccess2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayAccess2.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestArrayAccess3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayAccess3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestArrayAccess4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayAccess4.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestArrayAccess5() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayAccess5.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestArrayCopy1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayCopy1.apk");
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestArrayToString1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ArrayToString1.apk");
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	@Ignore
	public void runTestHashMapAccess1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/HashMapAccess1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	@Ignore
	public void runTestListAccess1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/ListAccess1.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestMultidimensionalArray1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists/MultidimensionalArray1.apk");
		Assert.assertEquals(1, res.size());
	}

}
