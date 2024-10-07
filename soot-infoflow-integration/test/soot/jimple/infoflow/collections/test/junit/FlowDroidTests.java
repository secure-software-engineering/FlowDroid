package soot.jimple.infoflow.collections.test.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.strategies.containers.shift.PreciseShift;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.config.PreciseCollectionStrategy;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.tagkit.AnnotationIntElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;

/**
 * Abstract test class used for all river tests
 *
 * @author Tim Lange
 */
public abstract class FlowDroidTests {
	protected static String appPath, libPath;

	protected static List<String> sources = Collections
			.singletonList("<soot.jimple.infoflow.collections.test.Helper: java.lang.String source()>");
	protected static List<String> sinks = Collections
			.singletonList("<soot.jimple.infoflow.collections.test.Helper: void sink(java.lang.Object)>");

	static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
		if (f.exists()) {
			if (sb.length() > 0)
				sb.append(System.getProperty("path.separator"));
			sb.append(f.getCanonicalPath());
		}
	}

	protected static String getCurrentMethod() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();
		commonSetup();
	}

	public static void commonSetup() throws IOException {
		File rootDir = getIntegrationRoot();
		File testSrc = new File(new File(rootDir, "build"), "testclasses");
		if (!testSrc.exists()) {
			Assert.fail("Test aborted - none of the test sources are available");
		}
		appPath = testSrc.toString();

		StringBuilder libPathBuilder = new StringBuilder();
		String javaHomeStr = System.getProperty("java.home");
		boolean found = false;
		if (!javaHomeStr.isEmpty()) {
			// Find the Java 8 rt.jar even when the JVM is of a higher version
			File parentDir = new File(javaHomeStr).getParentFile();
			File[] files = parentDir.listFiles((dir, name) -> name.contains("java-1.8.0-") || name.contains("java-8-"));
			if (files != null) {
				for (File java8Path : files) {
					File rtjar = new File(java8Path, "jre" + File.separator + "lib" + File.separator + "rt.jar");
					if (rtjar.exists()) {
						appendWithSeparator(libPathBuilder, rtjar);
						found = true;
						break;
					}
				}
			}
		}
		if (!found) {
			// Try the default path on ubuntu
			appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
			// Try the default path on fedora
			appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-1.8.0/jre/lib/rt.jar"));
			// Try the default path on Windows
			appendWithSeparator(libPathBuilder, new File("C:\\Program Files\\Java\\java-se-8u41-ri\\jre\\lib\\rt.jar"));
		}
		libPath = libPathBuilder.toString();
		if (libPath.isEmpty())
			throw new RuntimeException("Could not find rt.jar!");
	}

	public static int getExpectedResultsForMethod(String sig) {
		SootMethod sm = Scene.v().grabMethod(sig);
		if (sm == null)
			throw new RuntimeException("Could not find method!");

		for (Tag t : sm.getTags()) {
			if (!(t instanceof VisibilityAnnotationTag))
				continue;

			VisibilityAnnotationTag vat = (VisibilityAnnotationTag) t;
			Optional<AnnotationTag> optTag = vat.getAnnotations().stream().findAny();
			if (!optTag.isPresent())
				continue;

			AnnotationTag testTag = optTag.get();
			if (!testTag.getType().equals("Lsoot/jimple/infoflow/collections/test/junit/FlowDroidTest;"))
				continue;

			int expected = testTag.getElems().stream().filter(e -> e.getName().equals("expected"))
					.map(e -> (AnnotationIntElem) e).findAny().get().getValue();

			return expected;
		}

		throw new RuntimeException("Could not get expected results for method!");
	}

	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			ArrayList<IMethodSummaryProvider> providers = new ArrayList<>();
			providers.add(new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR));
			PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);

			return new SummaryTaintWrapper(sp)
					.setContainerStrategyFactory(m -> new TestConstantStrategy(m, new PreciseShift()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract void setConfiguration(InfoflowConfiguration config);

	protected IInfoflow initInfoflow() {
		AbstractInfoflow result = new Infoflow(null, false, new DefaultBiDiICFGFactory());
		result.getConfig().setPreciseCollectionTracking(PreciseCollectionStrategy.CONSTANT_MAP_SUPPORT);
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
		setupApplication.getConfig().setPerformConstantPropagation(true);
		setupApplication.getConfig().setMergeDexFiles(true);
		setupApplication.setTaintWrapper(getTaintWrapper());
		setConfiguration(setupApplication.getConfig());

		return setupApplication;
	}

	/**
	 * Test that the correct index was leaked
	 *
	 * @param res    results
	 * @param substr substring identifying the correct index
	 * @return true if given substring matches a statement
	 */
	protected boolean containsStmtString(InfoflowResults res, String substr) {
		for (DataFlowResult r : res.getResultSet()) {
			for (Stmt s : r.getSource().getPath()) {
				if (s.toString().contains(substr))
					return true;
			}
		}
		return false;
	}

	/**
	 * Gets the root in which the FlowDroid main project is located
	 * 
	 * @return The directory in which the FlowDroid main project is located
	 */
	public static File getIntegrationRoot() throws IOException {
		File testRoot = new File(".").getCanonicalFile();
		if (!new File(testRoot, "testAPKs").exists())
			testRoot = new File(testRoot, "soot-infoflow-integration");
		if (!new File(testRoot, "testAPKs").exists())
			throw new RuntimeException(String.format("Test root not found in %s", testRoot.getAbsolutePath()));
		return testRoot;
	}

}
