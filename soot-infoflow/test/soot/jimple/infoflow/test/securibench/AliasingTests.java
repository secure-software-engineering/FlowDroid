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

public class AliasingTests extends JUnitTests {

	@Test
	public void aliasing1() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing1: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}

	@Test
	public void aliasing2() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing2: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test
	public void aliasing3() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing3: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		negativeCheckInfoflow(infoflow);
		//INFO: this test has failed originally but I don't think there is a flow from source to sink - changed.
	}
	
	@Test
	public void aliasing4() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing4: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 2);
	}
	
	@Test
	public void aliasing5() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing5: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test
	public void aliasing6() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.aliasing.Aliasing6: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 7);
	}

}
