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
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.sourcesSinks.manager.DefaultSourceSinkManager;

/**
 * contain tests for Taintwrapper and parameters as sources and sinks
 */
public class InFunctionTests extends JUnitTests {

	private static final String SOURCE_STRING_PARAMETER = "@parameter0: java.lang.String";
	private static final String SOURCE_STRING_PARAMETER2 = "@parameter1: java.lang.String";

	private static final String SOURCE_INT_PARAMETER = "@parameter0: int";
	private static final String SOURCE_INT_PARAMETER2 = "@parameter1: int";

	private static final String SINK_STRING_RETURN = "secret";
	private static final String SINK_STRING_RETURN_R5 = "$stack9";

	@Test(timeout = 300000)
	public void inFunctionTest1() {
		IInfoflow infoflow = initInfoflow(true);
		String epoint = "<soot.jimple.infoflow.test.InFunctionCode: java.lang.String infSourceCode1(java.lang.String)>";

		DefaultSourceSinkManager ssm = new DefaultSourceSinkManager(sources, sinks);
		ssm.setParameterTaintMethods(Collections.singletonList(epoint));
		ssm.setReturnTaintMethods(Collections.singletonList(epoint));

		infoflow.computeInfoflow(appPath, libPath, epoint, ssm);
		Assert.assertTrue(infoflow.getResults().isPathBetween(SINK_STRING_RETURN, SOURCE_STRING_PARAMETER));
	}

	@Test(timeout = 300000)
	public void inFunctionTest2() {
		IInfoflow infoflow = initInfoflow(true);
		String epoint = "<soot.jimple.infoflow.test.InFunctionCode: java.lang.String infSourceCode2(java.lang.String)>";

		DefaultSourceSinkManager ssm = new DefaultSourceSinkManager(sources, sinks);
		ssm.setParameterTaintMethods(Collections.singletonList(epoint));
		ssm.setReturnTaintMethods(Collections.singletonList(epoint));

		infoflow.computeInfoflow(appPath, libPath, epoint, ssm);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void inFunctionTest3() {
		IInfoflow infoflow = initInfoflow(true);
		String epoint = "<soot.jimple.infoflow.test.InFunctionCode: java.lang.String infSourceCode3(java.lang.String)>";

		DefaultSourceSinkManager ssm = new DefaultSourceSinkManager(sources, sinks);
		ssm.setParameterTaintMethods(Collections.singletonList(epoint));
		ssm.setReturnTaintMethods(Collections.singletonList(epoint));

		infoflow.computeInfoflow(appPath, libPath, epoint, ssm);
		Assert.assertTrue(infoflow.getResults().isPathBetween(SINK_STRING_RETURN, SOURCE_STRING_PARAMETER));
	}

	@Test(timeout = 300000)
	public void inFunctionTest4() {
		IInfoflow infoflow = initInfoflow(true);
		Assert.assertNotNull(infoflow.getTaintWrapper());

		List<String> epoint = new ArrayList<String>();
		epoint.add("<soot.jimple.infoflow.test.InFunctionCode: void setTmp(java.lang.String)>");
		epoint.add(
				"<soot.jimple.infoflow.test.InFunctionCode: java.lang.String foo(java.lang.String,java.lang.String)>");

		DefaultSourceSinkManager ssm = new DefaultSourceSinkManager(sources, sinks);
		ssm.setParameterTaintMethods(epoint);
		ssm.setReturnTaintMethods(epoint);

		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoint), ssm);
		Assert.assertFalse(infoflow.getResults().isEmpty());
		Assert.assertEquals(3, infoflow.getResults().numConnections());
		Assert.assertTrue(infoflow.getResults().isPathBetween(SINK_STRING_RETURN_R5, SOURCE_STRING_PARAMETER));
		Assert.assertTrue(infoflow.getResults().isPathBetween(SINK_STRING_RETURN_R5, SOURCE_STRING_PARAMETER2));
	}

	@Test(timeout = 300000)
	public void parameterFlowTest() {
		IInfoflow infoflow = initInfoflow(true);
		List<String> epoint = new ArrayList<String>();
		epoint.add("<soot.jimple.infoflow.test.InFunctionCode: int paraToParaFlow(int,int,"
				+ "soot.jimple.infoflow.test.InFunctionCode$DataClass,"
				+ "soot.jimple.infoflow.test.InFunctionCode$DataClass)>");

		DefaultSourceSinkManager ssm = new DefaultSourceSinkManager(sources, sinks);
		ssm.setParameterTaintMethods(epoint);
		ssm.setReturnTaintMethods(epoint);

		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoint), ssm);
		Assert.assertNotNull(infoflow.getResults());
		Assert.assertTrue(infoflow.getResults().isPathBetween("b", SOURCE_INT_PARAMETER2));
		Assert.assertFalse(infoflow.getResults().isPathBetween("b", SOURCE_INT_PARAMETER));
	}

}
