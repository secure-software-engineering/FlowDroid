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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.ConfigSecuriBench;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.test.base.AbstractJUnitTests;

public abstract class JUnitTests extends AbstractJUnitTests {

	protected static String appPath, libPath;

	protected static List<String> sources;
	protected static List<String> sinks;
	protected static final String[] sinkArray = new String[] { "<java.io.PrintWriter: void println(java.lang.String)>",
			"<java.io.PrintWriter: void println(java.lang.Object)>",
			"<java.sql.Connection: java.sql.PreparedStatement prepareStatement(java.lang.String)>",
			"<java.sql.Statement: boolean execute(java.lang.String)>",
			"<java.sql.Statement: int executeUpdate(java.lang.String)>",
			"<java.sql.Statement: int executeUpdate(java.lang.String,int)>",
			"<java.sql.Statement: int executeUpdate(java.lang.String,java.lang.String[])>",
			"<java.sql.Statement: java.sql.ResultSet executeQuery(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletResponse: void sendRedirect(java.lang.String)>",
			"<java.io.File: void <init>(java.lang.String)>", "<java.io.FileWriter: void <init>(java.lang.String)>",
			"<java.io.FileInputStream: void <init>(java.lang.String)>" };

	protected static final String[] sourceArray = new String[] {
			"<jakarta.servlet.ServletRequest: java.lang.String getParameter(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>",
			"<jakarta.servlet.ServletRequest: java.lang.String[] getParameterValues(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String[] getParameterValues(java.lang.String)>",
			"<jakarta.servlet.ServletRequest: java.util.Map getParameterMap()>",
			"<jakarta.servlet.http.HttpServletRequest: java.util.Map getParameterMap()>",
			"<jakarta.servlet.ServletConfig: java.lang.String getInitParameter(java.lang.String)>",
			"<soot.jimple.infoflow.test.securibench.supportClasses.DummyServletConfig: java.lang.String getInitParameter(java.lang.String)>",
			"<jakarta.servlet.ServletConfig: java.util.Enumeration getInitParameterNames()>",
			"<jakarta.servlet.ServletContext: java.lang.String getInitParameter(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String[] getParameterValues(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.util.Map getParameterMap()>",
			"<jakarta.servlet.http.HttpServletRequest: javax.servlet.http.Cookie[] getCookies()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getHeader(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.util.Enumeration getHeaders(java.lang.String)>",
			"<jakarta.servlet.http.HttpServletRequest: java.util.Enumeration getHeaderNames()>",
			"<jakarta.servlet.ServletRequest: java.lang.String getProtocol()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getProtocol()>",
			"<jakarta.servlet.ServletRequest: java.lang.String getScheme()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getScheme()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getAuthType()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getQueryString()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.String getRemoteUser()>",
			"<jakarta.servlet.http.HttpServletRequest: java.lang.StringBuffer getRequestURL()>",
			"<jakarta.servlet.http.HttpServletRequest: javax.servlet.ServletInputStream getInputStream()>",
			"<jakarta.servlet.ServletRequest: javax.servlet.ServletInputStream getInputStream()>",
			"<org.springframework.web.multipart.MultipartHttpServletRequest: java.lang.String getParameter(java.lang.String)>" };

	protected static boolean taintWrapper = false;
	protected static boolean substituteCallParams = true;

	protected IEntryPointCreator entryPointCreator = null;

	@BeforeClass
	public static void setUp() throws IOException {
		File f = new File(".");
		StringBuilder appPathBuilder = new StringBuilder();
		addTestPathes(f, appPathBuilder);
		appPath = appPathBuilder.toString();

		StringBuilder libPathBuilder = new StringBuilder();
		addRtJarPath(libPathBuilder);
		libPath = libPathBuilder.toString();

		sources = Arrays.asList(sourceArray);
		sinks = Arrays.asList(sinkArray);
	}

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();

	}

	protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			boolean containsSink = false;
			List<String> actualSinkStrings = new LinkedList<String>();
			assertEquals(resultCount, map.size());
			for (String sink : sinkArray) {
				if (map.containsSinkMethod(sink)) {
					containsSink = true;
					actualSinkStrings.add(sink);
				}
			}

			assertTrue(containsSink);
			boolean onePathFound = false;
			for (String sink : actualSinkStrings) {
				boolean hasPath = false;
				for (String source : sourceArray) {
					if (map.isPathBetweenMethods(sink, source)) {
						hasPath = true;
						break;
					}
				}
				if (hasPath) {
					onePathFound = true;
				}
			}
			assertTrue(onePathFound);

		} else {
			fail("result is not available");
		}
	}

	protected void negativeCheckInfoflow(IInfoflow infoflow) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			for (String sink : sinkArray) {
				if (map.containsSinkMethod(sink)) {
					fail("sink is reached: " + sink);
				}
			}
			assertEquals(0, map.size());
		} else {
			fail("result is not available");
		}
	}

	protected IInfoflow initInfoflow(List<String> entryPoints) {
		List<String> substClasses = new LinkedList<String>();
		substClasses.add("soot.jimple.infoflow.test.securibench.supportClasses.DummyHttpRequest");
		substClasses.add("soot.jimple.infoflow.test.securibench.supportClasses.DummyHttpResponse");

		DefaultEntryPointCreator entryPointCreator = new DefaultEntryPointCreator(entryPoints);
		entryPointCreator.setSubstituteCallParams(substituteCallParams);
		entryPointCreator.setSubstituteClasses(substClasses);
		this.entryPointCreator = entryPointCreator;

//		BackwardsInfoflow result = new BackwardsInfoflow();
		Infoflow result = new Infoflow();

		result.setSootConfig(new ConfigSecuriBench());
		result.getConfig().setInspectSinks(false);
		if (taintWrapper) {
			EasyTaintWrapper easyWrapper;
			try {
				easyWrapper = new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt"));
				result.setTaintWrapper(easyWrapper);
			} catch (IOException e) {
				System.err.println("Could not initialize Taintwrapper:");
				e.printStackTrace();
			}

		}
		return result;
	}

}
