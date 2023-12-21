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
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

/**
 * contain several tests that cannot be assigned to the other categories - often
 * they are added due to findings in real-world applications, including negative
 * tests and tests for lifecycle handling
 */
public abstract class OtherTests extends JUnitTests {

	@Test(timeout = 300000)
	public void fieldTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void testWithField()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void defaultlifecycleTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.utilclasses.D1static: boolean start()>");
		epoints.add("<soot.jimple.infoflow.test.utilclasses.D1static: boolean taintIt()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void ConstructorFinalClassTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void genericsfinalconstructorProblem()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void ptsTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void testPointsToSet()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void easyNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void mailTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void methodTainted()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void mailNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void methodNotTainted()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void mail2Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void mail2TestNegative() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method2NotTainted()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void mail3Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void mail3NegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method3NotTainted()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void mail4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void mail5() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void mail6() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void method6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void innerClassTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void innerClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiCallTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void multiCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void passOverTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void passOverTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void overwriteTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void overwriteTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void loopTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void loopTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void dataObjectTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void dataObjectTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void paramTransferTest() {
		IInfoflow infoflow = initInfoflow(true);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void paramTransferTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void objectSensitiveTest1() {
		IInfoflow infoflow = initInfoflow(true);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void objectSensitiveTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void accessPathTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void accessPathTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void pathSkipTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
		Assert.assertEquals(1, infoflow.getResults().getResults().values().size());
	}

	@Test(timeout = 300000)
	public void pathSkipTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void pathSkipTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void pathSkipTest4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void pathSkipTest5() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void pathSkipTest6() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void pathSkipTest6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void recursionTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void recursionTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void noPathsTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void noPathsTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void doPrivilegedTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void doPrivilegedTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void doPrivilegedTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void doPrivilegedTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void doPrivilegedTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void doPrivilegedTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void doPrivilegedTest3_NoJDK() {
		IInfoflow infoflow = initInfoflow();
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void doPrivilegedTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void multiSinkTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void multiSinkTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void multiSinkTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void multiSinkTest2()>");
		infoflow.getConfig().setPathAgnosticResults(false);
		infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
		Assert.assertEquals(2, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void multiSinkTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void multiSinkTest3()>");
		infoflow.getConfig().setPathAgnosticResults(false);
		infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sinkInt, sourceIMEI));
		Assert.assertEquals(2, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void skipOverObjectTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void skipOverObjectTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void skipOverObjectTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void skipOverObjectTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}


	@Test(timeout = 300000)
	public void testNeighbors1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void testNeighbors1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(3, infoflow.getResults().getResultSet().size());
	}
}
