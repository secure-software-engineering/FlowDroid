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
package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.ConfigForTest;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

/**
 * abstract super class of all test cases which handles initialization, keeps
 * track of sources and sinks and allows to customize the tests (taintWrapper,
 * debug)
 *
 */
//TODO remove (it is just a copy of the Class in the infoflow project with a minor modification)
public abstract class JUnitTests {

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

	@BeforeClass
	public static void setUp() throws IOException {
		File f = new File(".");
		File testSrc1 = new File(f, "bin");
		File testSrc4 = new File(f, "testBin");
		File testSrc2 = new File(f, "build" + File.separator + "classes");
		File testSrc3 = new File(f, "build" + File.separator + "testclasses");
		File testSrc5 = new File(f,
				".." + File.separator + "soot-infoflow" + File.separator + "build" + File.separator + "testclasses");

		if (!(testSrc1.exists() || testSrc2.exists() || testSrc3.exists() || testSrc5.exists())) {
			fail("Test aborted - none of the test sources are available");
		}

		StringBuilder appPathBuilder = new StringBuilder();
		appendWithSeparator(appPathBuilder, testSrc1);
		appendWithSeparator(appPathBuilder, testSrc2);
		appendWithSeparator(appPathBuilder, testSrc3);
		appendWithSeparator(appPathBuilder, testSrc4);
		appendWithSeparator(appPathBuilder, testSrc5);
		appPath = appPathBuilder.toString();

		libPath = System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";

		sources = new ArrayList<String>();
		sources.add(sourcePwd);
		sources.add(sourceUserData);
		sources.add(sourceDeviceId);
		sources.add(sourceIMEI);
		sources.add(sourceIMSI);
		sources.add(sourceBundleGet);
		sources.add(sourceLongitude);

		sinks = new ArrayList<String>();
		sinks.add(sink);
		sinks.add(sinkInt);
		sinks.add(sinkBoolean);
		sinks.add(sinkDouble);
	}

	/**
	 * Appends the given path to the given {@link StringBuilder} if it exists
	 * 
	 * @param sb The {@link StringBuilder} to which to append the path
	 * @param f  The path to append
	 * @throws IOException
	 */
	private static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
		if (f.exists()) {
			if (sb.length() > 0)
				sb.append(System.getProperty("path.separator"));
			sb.append(f.getCanonicalPath());
		}
	}

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();

	}

	protected void checkInfoflow(Infoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			assertEquals(resultCount, map.size());
			assertTrue(map.containsSinkMethod(sink) || map.containsSinkMethod(sinkInt)
					|| map.containsSinkMethod(sinkBoolean) || map.containsSinkMethod(sinkDouble));
			assertTrue(map.isPathBetweenMethods(sink, sourceDeviceId) || map.isPathBetweenMethods(sink, sourceIMEI) // implicit
																													// flows
					|| map.isPathBetweenMethods(sink, sourcePwd) || map.isPathBetweenMethods(sink, sourceBundleGet)
					|| map.isPathBetweenMethods(sinkInt, sourceDeviceId)
					|| map.isPathBetweenMethods(sinkInt, sourceIMEI) || map.isPathBetweenMethods(sinkInt, sourceIMSI)
					|| map.isPathBetweenMethods(sinkBoolean, sourceDeviceId)
					|| map.isPathBetweenMethods(sinkDouble, sourceLongitude));
		} else {
			fail("result is not available");
		}

	}

	protected void negativeCheckInfoflow(Infoflow infoflow) {
		// If the result is available, it must be empty. Otherwise, it is
		// implicitly ok since we don't expect to find anything anyway.
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			assertEquals(0, map.size());
			assertFalse(map.containsSinkMethod(sink));
			assertFalse(map.containsSinkMethod(sinkInt));
		}
	}

	protected Infoflow initInfoflow() {
		return initInfoflow(false);
	}

	protected Infoflow initInfoflow(boolean useTaintWrapper) {
		Infoflow result = new Infoflow();
		ConfigForTest testConfig = new ConfigForTest();
		result.setSootConfig(testConfig);
		if (useTaintWrapper) {
			EasyTaintWrapper easyWrapper;
			try {
				easyWrapper = new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt"));
				result.setTaintWrapper(easyWrapper);
			} catch (IOException e) {
				System.err.println("Could not initialized Taintwrapper:");
				e.printStackTrace();
			}

		}
		return result;
	}

}
