package soot.jimple.infoflow.android.test;

import java.io.File;

/**
 * Abstract base class for JUnit tests in the Android plugin
 * 
 * @author Steven Arzt
 *
 */
public abstract class BaseJUnitTests {

	/**
	 * Gets the root in which the FlowDroid Android project is located
	 * 
	 * @return The directory in which the FlowDroid Android project is located
	 */
	public static File getInfoflowAndroidRoot() {
		File testRoot = new File(".");
		if (!new File(testRoot, "src").exists())
			testRoot = new File(testRoot, "soot-infoflow-android");
		if (!new File(testRoot, "src").exists())
			throw new RuntimeException(String.format("Test root not found in %s", testRoot.getAbsolutePath()));
		return testRoot;
	}

}
