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
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;

/**
 * Class for testing whether context sensitivity is kept during the analysis
 * 
 * @author Steven Arzt
 * 
 */
public class ContextSensitivityTests extends JUnitTests {

	@Test(timeout = 300000)
	public void contextSensitivityTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ContextSensitivityTestCode: void contextSensitivityTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
	}

	@Test(timeout = 300000)
	public void contextSensitivityTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ContextSensitivityTestCode: void contextSensitivityTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
		Assert.assertFalse(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
	}

	@Test(timeout = 300000)
	public void multipleCallSiteTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ContextSensitivityTestCode: void multipleCallSiteTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

	@Test(timeout = 300000)
	public void multipleExitTest1() {
		IInfoflow infoflow = initInfoflow(false);
		infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ContextSensitivityTestCode: void multipleExitTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
	}

}
