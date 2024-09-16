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

package soot.jimple.infoflow.test.base;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServlet;

/**
 * Abstract base class for JUnit tests
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractJUnitTests {

	/**
	 * Appends the given path to the given {@link StringBuilder} if it exists
	 * 
	 * @param sb The {@link StringBuilder} to which to append the path
	 * @param f  The path to append
	 * @throws IOException
	 */
	protected static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
		if (f.exists()) {
			if (sb.length() > 0)
				sb.append(System.getProperty("path.separator"));
			sb.append(f.getCanonicalPath());
		}
	}

	/**
	 * Adds the test paths to the given class path based on the given root directory
	 * 
	 * @param rootDir        The root directory relative to which to resolve the
	 *                       test directories
	 * @param appPathBuilder The {@link StringBuilder} used to build the classpath
	 *                       for the application classes
	 * @throws IOException
	 */
	protected static void addTestPathes(File rootDir, StringBuilder appPathBuilder) throws IOException {
		File testSrc1 = new File(rootDir, "bin");
		File testSrc2 = new File(rootDir, "build" + File.separator + "classes");
		File testSrc3 = new File(rootDir, "build" + File.separator + "testclasses");

		if (!(testSrc1.exists() || testSrc2.exists() || testSrc3.exists())) {
			fail(String.format("Test aborted - none of the test sources are available at root %s",
					rootDir.getCanonicalPath()));
		}

		appendWithSeparator(appPathBuilder, testSrc1);
		appendWithSeparator(appPathBuilder, testSrc2);
		appendWithSeparator(appPathBuilder, testSrc3);
	}

	/**
	 * Adds the path to the <code>rt.jar</code> file to the library classpath
	 * 
	 * @param libPathBuilder The {@link StringBuilder} used to construct the library
	 *                       classpath
	 * @throws IOException
	 */
	protected static void addRtJarPath(StringBuilder libPathBuilder) throws IOException {
		final String javaBaseDir = System.getProperty("java.home") + File.separator + "lib" + File.separator;

		appendWithSeparator(libPathBuilder, new File(javaBaseDir + "rt.jar"));
		appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
		appendWithSeparator(libPathBuilder, new File("C:\\Program Files\\Java\\java-se-8u41-ri\\jre\\lib\\rt.jar"));

		String jakartaJAR = HttpServlet.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		appendWithSeparator(libPathBuilder, new File(jakartaJAR));

		String springJAR = MultipartHttpServletRequest.class.getProtectionDomain().getCodeSource().getLocation()
				.getPath();
		appendWithSeparator(libPathBuilder, new File(springJAR));
	}

}
