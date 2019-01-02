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
package soot.jimple.infoflow.android.test.otherAPKs;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class JUnitTests {

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName The full path and file name of the APK file to analyze
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName) throws IOException, XmlPullParserException {
		return analyzeAPKFile(fileName, false, true, false);
	}

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName              The full path and file name of the APK file to
	 *                              analyze
	 * @param enableImplicitFlows   True if implicit flows shall be tracked,
	 *                              otherwise false
	 * @param enableStaticFields    True if taints in static fields shall be
	 *                              tracked, otherwise false
	 * @param flowSensitiveAliasing True if a flow-sensitive alias analysis shall be
	 *                              used, otherwise false
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName, boolean enableImplicitFlows, boolean enableStaticFields,
			boolean flowSensitiveAliasing) throws IOException, XmlPullParserException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new SetupApplication(androidJars, fileName);

		// Find the taint wrapper file
		File taintWrapperFile = new File("EasyTaintWrapperSource.txt");
		if (!taintWrapperFile.exists())
			taintWrapperFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
		setupApplication.setTaintWrapper(new EasyTaintWrapper(taintWrapperFile));

		// Configure the analysis
		setupApplication.getConfig().setImplicitFlowMode(
				enableImplicitFlows ? ImplicitFlowMode.AllImplicitFlows : ImplicitFlowMode.NoImplicitFlows);
		setupApplication.getConfig().setStaticFieldTrackingMode(
				enableStaticFields ? StaticFieldTrackingMode.ContextFlowSensitive : StaticFieldTrackingMode.None);
		setupApplication.getConfig().setFlowSensitiveAliasing(flowSensitiveAliasing);
		return setupApplication.runInfoflow("SourcesAndSinks.txt");
	}

}
