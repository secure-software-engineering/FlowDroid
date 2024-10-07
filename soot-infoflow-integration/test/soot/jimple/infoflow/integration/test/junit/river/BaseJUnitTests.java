package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;

import soot.SootMethod;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.test.junit.JUnitTests;

/**
 * Abstract test class used for all river tests
 *
 * @author Tim Lange
 */
public abstract class BaseJUnitTests extends JUnitTests {
	static class SimpleSourceSinkManager extends BaseSourceSinkManager {
		public SimpleSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
				Collection<? extends ISourceSinkDefinition> sinks, InfoflowConfiguration config) {
			super(sources, sinks, config);
		}

		@Override
		protected boolean isEntryPointMethod(SootMethod method) {
			return false;
		}
	}

	@BeforeClass
	public static void setUp() throws IOException {
		File f = getIntegrationRoot();
		StringBuilder appPathBuilder = new StringBuilder();
		addTestPathes(f, appPathBuilder);

		File fi = new File(f, "../soot-infoflow");
		if (!fi.getCanonicalFile().equals(f.getCanonicalFile())) {
			addTestPathes(fi, appPathBuilder);
		}
		fi = new File(f, "../soot-infoflow-summaries");
		if (!fi.getCanonicalFile().equals(f.getCanonicalFile())) {
			addTestPathes(fi, appPathBuilder);
		}
		fi = new File("soot-infoflow");
		if (fi.exists()) {
			addTestPathes(fi, appPathBuilder);
		}
		fi = new File("soot-infoflow-summaries");
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

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();

	}

	protected abstract ITaintPropagationWrapper getTaintWrapper();

	protected abstract void setConfiguration(InfoflowConfiguration config);

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		AbstractInfoflow result = new Infoflow(null, false, new DefaultBiDiICFGFactory());
		result.setThrowExceptions(true);
		result.setTaintWrapper(getTaintWrapper());
		setConfiguration(result.getConfig());
		return result;
	}

	protected SetupApplication initApplication(File apkFile) {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new SetupApplication(new File(androidJars), apkFile);

		setupApplication.getConfig().setMergeDexFiles(true);
		setupApplication.setTaintWrapper(getTaintWrapper());
		setConfiguration(setupApplication.getConfig());

		return setupApplication;
	}

	/**
	 * Gets the root in which the FlowDroid main project is located
	 * 
	 * @return The directory in which the FlowDroid main project is located
	 */
	public static File getIntegrationRoot() {
		File testRoot = new File(".");
		if (!new File(testRoot, "testAPKs").exists())
			testRoot = new File(testRoot, "soot-infoflow-integration");
		if (!new File(testRoot, "testAPKs").exists())
			throw new RuntimeException(String.format("Test root not found in %s", testRoot.getAbsolutePath()));
		return testRoot;
	}

}
