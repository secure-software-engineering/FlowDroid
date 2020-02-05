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
package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.results.InfoflowResults;

/**
 * contain various tests with more than one source, conditional statements,
 * loops and java-internal functions on tainted objects
 */
public class MultiTest extends JUnitTests {

	private static final String SOURCE_STRING_PWD = "<soot.jimple.infoflow.test.android.AccountManager: java.lang.String getPassword()>";

	@Test(timeout = 300000)
	public void multiTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSourceCode()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
	}

	@Test(timeout = 300000)
	public void multiTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSourceCode2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
	}

	@Test(timeout = 300000)
	public void ifPathTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void ifPathTestCode1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
		Assert.assertEquals(2, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void ifPathTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void ifPathTestCode2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void ifPathTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void ifPathTestCode3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void ifPathTest4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void ifPathTestCode4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void loopPathTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void loopPathTestCode1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void hashTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void hashTestCode1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void shiftTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void shiftTestCode1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, SOURCE_STRING_PWD));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void intMultiTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void intMultiTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, sourceIMEI));
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, sourceIMSI));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void intMultiTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void intMultiTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, sourceIMEI));
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, sourceIMSI));
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test // (timeout=300000)
	public void sameSourceMultiTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void sameSourceMultiTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
		Assert.assertEquals(1, infoflow.getResults().size());
		Assert.assertEquals(2, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void stopAfterFirstKFlowsTest0() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setStopAfterFirstFlow(true);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSourceCode()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void stopAfterFirstKFlowsTest1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setStopAfterFirstFlow(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSourceCode()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void stopAfterFirstKFlowsTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setStopAfterFirstKFlows(1);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSourceCode()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiSinkTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MultiTestCode: void multiSinkTest1()>");

		final String streamSource = "<java.io.ByteArrayOutputStream: byte[] toByteArray()>";
		final String publishSink = "<soot.jimple.infoflow.test.android.ConnectionManager: void publish(java.lang.String)>";

		List<String> testSources = new ArrayList<>(sources);
		testSources.add(streamSource);

		infoflow.computeInfoflow(appPath, libPath, epoints, testSources, sinks);
		InfoflowResults results = infoflow.getResults();

		Assert.assertNotNull(results);
		Assert.assertTrue(results.isPathBetweenMethods(publishSink, streamSource));
		Assert.assertEquals(2, results.numConnections());
	}

}
