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

public class ArrayTests extends JUnitTests {

	@Test
	public void arrays1() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays1: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");	
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void arrays2() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays2: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test
	public void arrays3() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays3: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays4() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays4: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays5() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays5: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test
	public void arrays6() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays6: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays7() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays7: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays8() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays8: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays9() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays9: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
	
	@Test
	public void arrays10() {
		List<String> epoints = new ArrayList<String>();
		epoints.add("<securibench.micro.arrays.Arrays10: void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>");
		Infoflow infoflow = initInfoflow(epoints);
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		checkInfoflow(infoflow,1);
	}
}
