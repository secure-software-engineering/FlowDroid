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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

/**
 * Test class for implicit flows
 * 
 * @author Steven Arzt
 */
public abstract class ImplicitFlowTests extends JUnitTests {

	@Override
	protected IInfoflow initInfoflow() {
		IInfoflow infoflow = super.initInfoflow();
		infoflow.getConfig().setImplicitFlowMode(ImplicitFlowMode.AllImplicitFlows);
		return infoflow;
	}

	@Test(timeout = 300000)
	public void simpleTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void simpleNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void simpleOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleOverwriteTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void switchTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void switchTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void convertTest() {
		long timeBefore = System.nanoTime();
		System.out.println("Starting convertTest...");

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(1);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setStaticFieldTrackingMode(StaticFieldTrackingMode.None);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void convertTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);

		System.out.println("convertTest took " + (System.nanoTime() - timeBefore) / 1E9 + " seconds");
	}

	@Test(timeout = 300000)
	public void sinkTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void sinkTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void returnTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void returnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeCallTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void negativeCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void recursionTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void recursionTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void recursionTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void exceptionTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void exceptionTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void exceptionTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void fieldTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void fieldTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticFieldTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticFieldTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticFieldTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticFieldTest4() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticFieldTest5() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void integerClassTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void integerClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void stringClassTest() {
		long timeBefore = System.nanoTime();
		System.out.println("Starting stringClassTest...");

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(1);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setStaticFieldTrackingMode(StaticFieldTrackingMode.None);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void stringClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);

		System.out.println("stringClassTest took " + (System.nanoTime() - timeBefore) / 1E9 + " seconds");
	}

	@Test(timeout = 300000)
	@Ignore
	public void conditionalExceptionTest() {
		// not yet supported
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalExceptionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void passOverTest() {
		// not yet supported
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void passOverTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void passOverTest2() {
		// not yet supported
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void passOverTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callToReturnTest() throws IOException {
		// not yet supported
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.setTaintWrapper(EasyTaintWrapper.getDefault());
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callToReturnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callToReturnTest2() throws IOException {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.setTaintWrapper(EasyTaintWrapper.getDefault());
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callToReturnTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void createAliasInFunctionTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void createAliasInFunctionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void createAliasInFunctionTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void createAliasInFunctionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void implicitFlowTaintWrapperTest() throws IOException {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.setTaintWrapper(EasyTaintWrapper.getDefault());
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void implicitFlowTaintWrapperTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void implicitFlowTaintWrapperNegativeTest() throws IOException {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.setTaintWrapper(new EasyTaintWrapper(Collections.<String, Set<String>>emptyMap()));
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void implicitFlowTaintWrapperTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void hierarchicalCallSetTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void hierarchicalCallSetTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void conditionalAliasingTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void conditionalAliasingTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void conditionalAliasingTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testStringConvert() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setStaticFieldTrackingMode(StaticFieldTrackingMode.None);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConvert()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void afterCallNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void afterCallNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void ifInCalleeTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void ifInCalleeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationConditionalTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void activationConditionalTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void classTypeTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void classTypeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void conditionalReturnTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalReturnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void stringObfuscationTest1() {
		IInfoflow infoflow = initInfoflow(true);
		infoflow.getConfig().setInspectSinks(false);

		((EasyTaintWrapper) infoflow.getTaintWrapper()).addMethodForWrapping("soot.jimple.infoflow.test.android.Base64",
				"java.lang.String encodeToString(byte[])");
		((EasyTaintWrapper) infoflow.getTaintWrapper()).addIncludePrefix("soot.jimple.infoflow.test.android.");

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void stringObfuscationTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void arrayIndexTest1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void arrayIndexTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void exceptionTest1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void exceptionTest4() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void userCodeTest1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void userCodeTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void userCodeTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setEnableExceptionTracking(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void userCodeTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	@Ignore("does not match Soot's model of exceptional unit graphs")
	public void userCodeTest2b() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void userCodeTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void nestedIfTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void nestedIfTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
}
