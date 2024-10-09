package soot.jimple.infoflow.android.test.sourceToSinks;

import java.io.File;
import java.io.IOException;

import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.test.BaseJUnitTests;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

/**
 * provides methods for the test cases to run the analyze
 *
 */
public class JUnitTests extends BaseJUnitTests {

	/**
	 * Analyzes the given APK file for data flows with a given xml file
	 * 
	 * @param apkFile The full path and file name of the APK file to analyze
	 * @param xmlFile The full path and file name of the xml file where sources and
	 *                sinks are defined
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required file
	 *                     could not be found
	 */
	public InfoflowResults analyzeAPKFile(File apkFile, File xmlFile) throws IOException {
		return analyzeAPKFile(apkFile, xmlFile, false, false, false);
	}

	/**
	 * Analyzes the given APK file for data flows with a given xml file
	 * 
	 * @param apkFile             The full path and file name of the APK file to
	 *                            analyze
	 * @param xmlFile             The full path and file name of the xml file where
	 *                            sources and sinks are defined
	 * @param enableImplicitFlows True if implicit flows shall be tracked, otherwise
	 *                            false
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required file
	 *                     could not be found
	 */
	public InfoflowResults analyzeAPKFile(File apkFile, File xmlFile, boolean enableImplicitFlows,
			boolean enableStaticFields, boolean flowSensitiveAliasing) throws IOException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new SetupApplication(new File(androidJars), apkFile);
		setupApplication.setTaintWrapper(EasyTaintWrapper.getDefault());
		setupApplication.getConfig().setImplicitFlowMode(
				enableImplicitFlows ? ImplicitFlowMode.AllImplicitFlows : ImplicitFlowMode.NoImplicitFlows);
		setupApplication.getConfig().setStaticFieldTrackingMode(
				enableStaticFields ? StaticFieldTrackingMode.ContextFlowSensitive : StaticFieldTrackingMode.None);
		setupApplication.getConfig().setFlowSensitiveAliasing(flowSensitiveAliasing);

//		setupApplication.getConfig().setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);

		return setupApplication.runInfoflow(xmlFile);
	}
}
