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
import soot.jimple.infoflow.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionInfoflow;
import soot.jimple.infoflow.collections.CollectionsSetupApplication;
import soot.jimple.infoflow.collections.strategies.containers.shift.PreciseShift;
import soot.jimple.infoflow.collections.taintWrappers.CollectionSummaryTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionSummaryParser;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.taintWrappers.PrioritizingMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
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
		File testSrc = new File("build" + File.separator + "testclasses");
		if (!testSrc.exists()) {
			Assert.fail("Test aborted - none of the test sources are available");
		}
		appPath = testSrc.toString();

		StringBuilder libPathBuilder = new StringBuilder();
		appendWithSeparator(libPathBuilder,
				new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
		appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
		libPath = libPathBuilder.toString();
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
			providers.add(new CollectionSummaryParser("stubdroidBasedWA"));
			providers.add(new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR));
			PrioritizingMethodSummaryProvider sp = new PrioritizingMethodSummaryProvider(providers);
			return new CollectionSummaryTaintWrapper(sp, m -> new TestConstantStrategy(m, new PreciseShift()));
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	protected abstract void setConfiguration(InfoflowConfiguration config);

	protected IInfoflow initInfoflow() {
		AbstractInfoflow result = new CollectionInfoflow("", false, new DefaultBiDiICFGFactory());
		result.setThrowExceptions(true);
		result.setTaintWrapper(getTaintWrapper());
		setConfiguration(result.getConfig());
		return result;
	}

	protected SetupApplication initApplication(String fileName) {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new CollectionsSetupApplication(androidJars, fileName);
		setupApplication.getConfig().setMergeDexFiles(true);
		setupApplication.setTaintWrapper(getTaintWrapper());
		setConfiguration(setupApplication.getConfig());

		return setupApplication;
	}


	/**
	 * Test that the correct index was leaked
	 *
	 * @param res results
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
}
