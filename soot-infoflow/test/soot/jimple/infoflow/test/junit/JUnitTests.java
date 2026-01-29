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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.solver.Pair;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;
import soot.jimple.infoflow.FlowDroidLocalSplitter.SplittedLocal;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.ConfigForTest;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.test.base.AbstractJUnitTests;
import soot.util.MultiMap;

/**
 * abstract super class of all test cases which handles initialization, keeps
 * track of sources and sinks and allows to customize the tests (taintWrapper,
 * debug)
 *
 */
public abstract class JUnitTests extends AbstractJUnitTests {

	protected static final Logger logger = LoggerFactory.getLogger(JUnitTests.class);

	protected static String appPath, libPath;

	protected static List<String> sinks;

	protected static final String sink = "<soot.jimple.infoflow.test.android.ConnectionManager: void publish(java.lang.String)>";
	protected static final String sinkInt = "<soot.jimple.infoflow.test.android.ConnectionManager: void publish(int)>";
	protected static final String sinkBoolean = "<soot.jimple.infoflow.test.android.ConnectionManager: void publish(boolean)>";
	protected static final String sinkDouble = "<soot.jimple.infoflow.test.android.ConnectionManager: void publish(java.lang.Double)>";

	protected static List<String> sources;
	protected static final String sourceDeviceId = "<soot.jimple.infoflow.test.android.TelephonyManager: java.lang.String getDeviceId()>";
	protected static final String sourceIMEI = "<soot.jimple.infoflow.test.android.TelephonyManager: int getIMEI()>";
	protected static final String sourceIMSI = "<soot.jimple.infoflow.test.android.TelephonyManager: int getIMSI()>";
	protected static final String sourcePwd = "<soot.jimple.infoflow.test.android.AccountManager: java.lang.String getPassword()>";
	protected static final String sourceUserData = "<soot.jimple.infoflow.test.android.AccountManager: java.lang.String[] getUserData(java.lang.String)>";
	protected static final String sourceBundleGet = "<soot.jimple.infoflow.test.android.Bundle: java.lang.Object get(java.lang.String)>";
	protected static final String sourceLongitude = "<soot.jimple.infoflow.test.android.LocationManager: double getLongitude()>";
	protected static final String sourceLocation = "<soot.jimple.infoflow.test.android.LocationManager: soot.jimple.infoflow.test.android.Location getLastKnownLocation()>";

	@BeforeClass
	public static void setUp() throws IOException {
		File f = getInfoflowRoot(JUnitTests.class);
		StringBuilder appPathBuilder = new StringBuilder();
		addTestPathes(f, appPathBuilder);

		File fi = new File(f, "../soot-infoflow");
		if (!fi.getCanonicalFile().equals(f.getCanonicalFile())) {
			addTestPathes(fi, appPathBuilder);
		}
		fi = new File("soot-infoflow");
		if (fi.exists()) {
			addTestPathes(fi, appPathBuilder);
		}
		appPath = appPathBuilder.toString();

		StringBuilder libPathBuilder = new StringBuilder();
		addRtJarPath(libPathBuilder);
		libPath = libPathBuilder.toString();
		if (libPath.isEmpty())
			throw new RuntimeException("Could not find rt.jar!");

		initializeSourceSinks();
	}

	protected static void initializeSourceSinks() {
		sources = new ArrayList<String>();
		sources.add(sourcePwd);
		sources.add(sourceUserData);
		sources.add(sourceDeviceId);
		sources.add(sourceIMEI);
		sources.add(sourceIMSI);
		sources.add(sourceBundleGet);
		sources.add(sourceLongitude);
		sources.add(sourceLocation);

		sinks = new ArrayList<String>();
		sinks.add(sink);
		sinks.add(sinkInt);
		sinks.add(sinkBoolean);
		sinks.add(sinkDouble);
	}

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();

	}

	protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
		checkCodeForNoSplitLocals();
		//check that there are no splitted locals left
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			checkInfoflowResultsForNoSplitLocals(map);

			assertEquals(resultCount, map.size());
			assertTrue(map.containsSinkMethod(sink) || map.containsSinkMethod(sinkInt)
					|| map.containsSinkMethod(sinkBoolean) || map.containsSinkMethod(sinkDouble));
			assertTrue(map.isPathBetweenMethods(sink, sourceDeviceId) || map.isPathBetweenMethods(sink, sourceIMEI) // implicit
																													// flows
					|| map.isPathBetweenMethods(sinkDouble, sourceIMEI) || map.isPathBetweenMethods(sink, sourcePwd)
					|| map.isPathBetweenMethods(sink, sourceBundleGet)
					|| map.isPathBetweenMethods(sinkInt, sourceDeviceId)
					|| map.isPathBetweenMethods(sinkInt, sourceIMEI) || map.isPathBetweenMethods(sinkInt, sourceIMSI)
					|| map.isPathBetweenMethods(sinkInt, sourceLongitude)
					|| map.isPathBetweenMethods(sinkBoolean, sourceDeviceId)
					|| map.isPathBetweenMethods(sinkDouble, sourceLongitude)
					|| map.isPathBetweenMethods(sinkDouble, sourceLocation));
		} else {
			fail("result is not available");
		}
	}

	private void checkCodeForNoSplitLocals() {
		for (SootClass sc : Scene.v().getClasses()) {
			for (SootMethod m : sc.getMethods()) {
				if (m.hasActiveBody()) {
					Body b = m.getActiveBody();
					for (Local l : b.getLocals()) {
						if (l instanceof SplittedLocal) {
							fail(String.format("Found split local in %s: %s", m.getSignature(), l.getName()));
						}
					}

					Iterator<ValueBox> it = b.getUseAndDefBoxesIterator();
					while (it.hasNext()) {
						ValueBox vb = it.next();
						Value v = vb.getValue();
						if (v instanceof SplittedLocal) {
							fail(String.format("Found split local in %s: %s", m.getSignature(), v));
						}
					}
				}
			}
		}
	}

	private void checkInfoflowResultsForNoSplitLocals(InfoflowResults map) {
		MultiMap<ResultSinkInfo, ResultSourceInfo> res = map.getResults();
		if (res != null) {
			for (Pair<ResultSinkInfo, ResultSourceInfo> pair : res) {
				ResultSinkInfo sink = pair.getO1();
				ResultSourceInfo source = pair.getO2();
				checkAccessPathForSplitLocals(sink.getAccessPath());
				checkAccessPathForSplitLocals(source.getAccessPath());
				AccessPath[] app = source.getPathAccessPaths();
				if (app != null) {
					for (AccessPath i : app) {
						checkAccessPathForSplitLocals(i);
					}
				}
			}
		}
	}

	private void checkAccessPathForSplitLocals(AccessPath ap) {
		Local l = ap.getPlainValue();
		if (l instanceof SplittedLocal) {
			fail("Split local found in " + ap);
		}
	}

	protected void negativeCheckInfoflow(IInfoflow infoflow) {
		// If the result is available, it must be empty. Otherwise, it is
		// implicitly ok since we don't expect to find anything anyway.
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			assertEquals(0, map.size());
			assertFalse(map.containsSinkMethod(sink));
			assertFalse(map.containsSinkMethod(sinkInt));
		}
	}

	protected IInfoflow initInfoflow() {
		return initInfoflow(false);
	}

	protected IInfoflow initInfoflow(boolean useTaintWrapper) {
		AbstractInfoflow result = createInfoflowInstance();
		result.setThrowExceptions(true);
		ConfigForTest testConfig = new ConfigForTest();
		result.setSootConfig(testConfig);
		if (useTaintWrapper) {
			EasyTaintWrapper easyWrapper;
			try {
				easyWrapper = EasyTaintWrapper.getDefault();
				result.setTaintWrapper(easyWrapper);
			} catch (IOException e) {
				System.err.println("Could not initialized Taintwrapper:");
				e.printStackTrace();
			}

		}
		//		result.getConfig().setLogSourcesAndSinks(true);

		return result;
	}

	protected abstract AbstractInfoflow createInfoflowInstance();

	/**
	 * Tells that the test should only be run on backwards analysis
	 *
	 * @param infoflow infoflow object
	 * @param message  message shown in console
	 */
	protected void onlyBackwards(IInfoflow infoflow, String message) {
		Assume.assumeTrue("Test is only applicable on backwards analysis: " + message,
				infoflow instanceof BackwardsInfoflow);
	}

	/**
	 * Tells that the test should only be run on forwards analysis
	 *
	 * @param infoflow infoflow object
	 */
	protected void onlyForwards(IInfoflow infoflow, String message) {
		Assume.assumeTrue("Test is only applicable on forwards analysis: " + message, infoflow instanceof Infoflow);
	}

	/**
	 * Gets the root of the current project from a reference class located in that
	 * project
	 * 
	 * @param referenceClass The reference class
	 * @return The root folder of the project
	 * @throws IOException
	 */
	public static File getInfoflowRoot(Class<?> referenceClass) throws IOException {
		return getInfoflowRoot(referenceClass, "soot-infoflow");
	}

	/**
	 * Gets the root in which the FlowDroid main project is located
	 * 
	 * @return The directory in which the FlowDroid main project is located
	 */
	public static File getInfoflowRoot() throws IOException {
		return getInfoflowRoot("soot-infoflow");
	}

}
