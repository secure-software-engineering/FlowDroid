package soot.jimple.infoflow.android.test.sourceToSinks;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

/**
 * provides methods for the test cases to run the analyze
 *
 */
public class JUnitTests {

	/**
	 * Analyzes the given APK file for data flows with a given xml file
	 * 
	 * @param apkFileName The full path and file name of the APK file to analyze
	 * @param xmlFileName The full path and file name of the xml file where sources
	 *                    and sinks are defined
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String apkFileName, String xmlFileName)
			throws IOException, XmlPullParserException {
		return analyzeAPKFile(apkFileName, xmlFileName, false, false, false);
	}

	/**
	 * Analyzes the given APK file for data flows with a given xml file
	 * 
	 * @param apkFileName         The full path and file name of the APK file to
	 *                            analyze
	 * @param xmlFileName         The full path and file name of the xml file where
	 *                            sources and sinks are defined
	 * @param enableImplicitFlows True if implicit flows shall be tracked, otherwise
	 *                            false
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String apkFileName, String xmlFileName, boolean enableImplicitFlows,
			boolean enableStaticFields, boolean flowSensitiveAliasing) throws IOException, XmlPullParserException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new SetupApplication(androidJars, apkFileName);
		setupApplication.setTaintWrapper(new EasyTaintWrapper());
		setupApplication.getConfig().setImplicitFlowMode(
				enableImplicitFlows ? ImplicitFlowMode.AllImplicitFlows : ImplicitFlowMode.NoImplicitFlows);
		setupApplication.getConfig().setStaticFieldTrackingMode(
				enableStaticFields ? StaticFieldTrackingMode.ContextFlowSensitive : StaticFieldTrackingMode.None);
		setupApplication.getConfig().setFlowSensitiveAliasing(flowSensitiveAliasing);
		return setupApplication.runInfoflow(xmlFileName);
	}
}
