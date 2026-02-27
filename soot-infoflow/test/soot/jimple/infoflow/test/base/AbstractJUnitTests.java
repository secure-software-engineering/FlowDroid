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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServlet;

/**
 * Abstract base class for JUnit tests
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractJUnitTests {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractJUnitTests.class);

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
		appendWithSeparator(libPathBuilder, new File("C:\\Program Files\\Java\\jre-1.8\\lib\\rt.jar"));

		String jakartaJAR = HttpServlet.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		appendWithSeparator(libPathBuilder, new File(jakartaJAR));

		String springJAR = MultipartHttpServletRequest.class.getProtectionDomain().getCodeSource().getLocation()
				.getPath();
		appendWithSeparator(libPathBuilder, new File(springJAR));
	}

	/**
	 * Gets the root of the current project from a reference class located in that
	 * project
	 * 
	 * @param referenceClass The reference class
	 * @param moduleName     The name of the FlowDroid module for which to get the
	 *                       root folder
	 * @return The root folder of the project
	 * @throws IOException
	 */
	public static File getInfoflowRoot(Class<?> referenceClass, String moduleName) throws IOException {
		File classFile = new File(referenceClass.getProtectionDomain().getCodeSource().getLocation().getPath());
		File f = classFile;
		if (f.exists()) {
			while (!f.getName().equals(moduleName) && f.getParentFile() != null)
				f = f.getParentFile();

			// The project root must exist and must not be the file system root
			if (f.exists() && f.getParentFile() != null)
				return f;

			logger.warn("Finding project root from class file {} failed", classFile);
		} else
			logger.warn("Class file {} does not exist", classFile);
		return getInfoflowRoot(moduleName);
	}

	/**
	 * Gets the root in which the FlowDroid main project is located
	 * 
	 * @param moduleName The name of the FlowDroid module for which to get the root
	 *                   folder
	 * @return The directory in which the FlowDroid main project is located
	 */
	public static File getInfoflowRoot(String moduleName) throws IOException {
		File testRoot = new File(".").getCanonicalFile();

		if (!new File(testRoot, "src").exists()) {
			// Try a subfolder
			File subFolder = new File(testRoot, moduleName);
			if (subFolder.exists())
				testRoot = subFolder;
			else {
				// Try a sibling folder
				testRoot = new File(testRoot.getParentFile(), moduleName);
			}
		}

		if (!new File(testRoot, "src").exists())
			throw new RuntimeException(String.format("Test root not found in %s", testRoot.getAbsolutePath()));
		return testRoot;
	}

}
