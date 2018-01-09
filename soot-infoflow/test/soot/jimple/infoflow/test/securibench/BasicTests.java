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
package soot.jimple.infoflow.test.securibench;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.Infoflow;

public class BasicTests extends JUnitTests {

	/**
	 * custom test to check whether there is an edge to string methods or not on
	 * different configurations
	 */
	@Test(timeout = 300000)
	public void basic0() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic0: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic1() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic1: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic2() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic2: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic3() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic3: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic4() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic4: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic5() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic5: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 3);
	}

	@Test(timeout = 300000)
	public void basic6() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic6: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic7() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic7: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic8() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic8: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic9() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic9: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic10() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic10: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic11() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic11: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void basic12() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic12: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void basic13() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic13: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic14() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic14: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic15() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic15: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic16() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic16: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic17() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic17: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic18() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic18: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic19() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic19: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic20() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic20: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic21() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic21: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 4);
	}

	@Test(timeout = 300000)
	public void basic22() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic22: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic23() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic23: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 3);
	}

	@Test(timeout = 300000)
	public void basic24() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic24: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic25() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic25: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic26() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic26: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic27() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic27: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic28() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic28: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void basic29() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic29: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void basic30() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic30: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic31() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic31: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 3);
	}

	@Test(timeout = 300000)
	public void basic32() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic32: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic33() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic33: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic34() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic34: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void basic35() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic35: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 6);
	}

	@Test(timeout = 300000)
	public void basic36() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic36: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic37() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic37: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic38() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic38: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic39() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic39: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic40() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic40: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
		// TODO: this test fails at the moment!
	}

	@Test(timeout = 300000)
	public void basic41() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic41: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void basic42() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.basic.Basic42: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.getConfig().setEnableStaticFieldTracking(false);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
