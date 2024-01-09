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

/**
 * test the overwrite behavior of tainted variables, fields and static variables
 */
public abstract class OverwriteTests extends JUnitTests {

	@Test(timeout = 300000)
	public void varOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void varOverwrite()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void staticFieldOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void staticFieldOverwrite()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void fieldOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void fieldOverwrite()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest5() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void returnOverwriteTest6() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void returnOverwrite6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void loopTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void loopOverwrite()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void loopTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void loopOverwrite2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void overwriteAliasTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void overwriteAlias()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().size());
	}

	@Test(timeout = 300000)
	public void simpleOverwriteAliasTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void simpleOverwriteAliasTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void overwriteBaseValueTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.OverwriteTestCode: void overwriteBaseValueTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

}
