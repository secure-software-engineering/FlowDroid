package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.BeforeClass;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.options.Options;

public abstract class BaseSummaryTaintWrapperTests {
	private static String appPath, libPath;

	private String[] source = new String[] {
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: java.lang.Object source()>",
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: int intSource()>",
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: java.lang.String stringSource()>",
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: java.lang.String stringSource2()>" };
	private String sink = "<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void sink(java.lang.Object)>";
	private ITaintPropagationWrapper summaryWrapper;

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();
	}

	protected void testFlowForMethod(String m) {
		testFlowForMethod(m, 1);
	}

	protected void testFlowForMethod(String m, int count) {
		IInfoflow iFlow = null;
		try {
			iFlow = initInfoflow();
			iFlow.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
			iFlow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(Collections.singletonList(m)),
					Arrays.asList(source), Collections.singletonList(sink));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		checkInfoflow(iFlow, count);
	}

	protected void testNoFlowForMethod(String m) {
		IInfoflow iFlow = null;
		try {
			iFlow = initInfoflow();
			iFlow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(Collections.singletonList(m)),
					Arrays.asList(source), Collections.singletonList(sink));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		checkNoInfoflow(iFlow);
	}

	private void checkNoInfoflow(IInfoflow infoflow) {
		assertTrue(!infoflow.isResultAvailable() || infoflow.getResults().size() == 0);
	}

	private void checkInfoflow(IInfoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();

			assertTrue(map.containsSinkMethod(sink));
			assertTrue(map.isPathBetweenMethods(sink, source[0]) || map.isPathBetweenMethods(sink, source[1])
					|| map.isPathBetweenMethods(sink, source[2]) || map.isPathBetweenMethods(sink, source[3]));
			assertEquals(resultCount, infoflow.getResults().numConnections());
		} else {
			fail("result is not available");
		}
	}

	protected abstract AbstractInfoflow createInfoflowInstance();

	protected IInfoflow initInfoflow() throws IOException, XMLStreamException {
		IInfoflow result = createInfoflowInstance();
		result.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		IInfoflowConfig testConfig = new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				List<String> excludeList = new ArrayList<>();
				excludeList.add("soot.jimple.infoflow.test.methodSummary.ApiClass");
				excludeList.add("soot.jimple.infoflow.test.methodSummary.GapClass");
				Options.v().set_exclude(excludeList);

				List<String> includeList = new ArrayList<>();
				includeList.add("soot.jimple.infoflow.test.methodSummary.UserCodeClass");
				Options.v().set_include(includeList);

				Options.v().set_no_bodies_for_excluded(true);
				Options.v().set_allow_phantom_refs(true);
				Options.v().set_ignore_classpath_errors(true);
			}

		};
		result.setSootConfig(testConfig);

		File testRoot = getTestRoot();
		Set<File> summaryFiles = new HashSet<>();
		summaryFiles.add(new File(testRoot, "./testSummaries/soot.jimple.infoflow.test.methodSummary.ApiClass.xml"));
		summaryFiles.add(new File(testRoot, "./testSummaries/soot.jimple.infoflow.test.methodSummary.GapClass.xml"));
		summaryFiles.add(new File(testRoot, "./testSummaries/soot.jimple.infoflow.test.methodSummary.Data.xml"));
		summaryFiles
				.add(new File(testRoot, "./testSummaries/soot.jimple.infoflow.test.methodSummary.TestCollection.xml"));
		summaryFiles.add(new File(testRoot, "./summariesManual"));

		summaryWrapper = TaintWrapperFactory.createTaintWrapperFromFiles(summaryFiles);
		result.setTaintWrapper(summaryWrapper);
		return result;
	}

	@BeforeClass
	public static void setUp() throws IOException {
		StringBuilder appPathBuilder = new StringBuilder();
		File f = getTestRoot();
		addTestPathes(f, appPathBuilder);

		File fi = new File(f, "../soot-infoflow");
		if (!fi.getCanonicalFile().equals(f.getCanonicalFile())) {
			addTestPathes(fi, appPathBuilder);
		}
		fi = new File(f, "../soot-infoflow-summaries");
		if (!fi.getCanonicalFile().equals(f.getCanonicalFile())) {
			addTestPathes(fi, appPathBuilder);
		}
		appPath = appPathBuilder.toString();

		StringBuilder libPathBuilder = new StringBuilder();
		appendWithSeparator(libPathBuilder,
				new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
		appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
		libPath = libPathBuilder.toString();
	}

	private static void addTestPathes(File f, StringBuilder appPathBuilder) throws IOException {
		File testSrc1 = new File(f, "bin");
		File testSrc2 = new File(f, "testBin");
		File testSrc3 = new File(f, "build" + File.separator + "classes");
		File testSrc4 = new File(f, "build" + File.separator + "testclasses");

		if (!(testSrc1.exists() || testSrc2.exists() || testSrc3.exists() || testSrc4.exists())) {
			fail("Test aborted - none of the test sources are available");
		}
		appendWithSeparator(appPathBuilder, testSrc1);
		appendWithSeparator(appPathBuilder, testSrc2);
		appendWithSeparator(appPathBuilder, testSrc3);
		appendWithSeparator(appPathBuilder, testSrc4);
	}

	/**
	 * Appends the given path to the given {@link StringBuilder} if it exists
	 * 
	 * @param sb The {@link StringBuilder} to which to append the path
	 * @param f  The path to append
	 * @throws IOException
	 */
	private static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
		if (f.exists()) {
			if (sb.length() > 0)
				sb.append(System.getProperty("path.separator"));
			sb.append(f.getCanonicalPath());
		}
	}

	/**
	 * Gets the root in which the StubDroid project is located
	 * 
	 * @return The directory in which StubDroid is located
	 */
	public static File getTestRoot() throws IOException {
		File testRoot = new File(".").getCanonicalFile();
		if (!new File(testRoot, "testSummaries").exists()) {
			// Try a subfolder
			File subFolder = new File(testRoot, "soot-infoflow-summaries");
			if (subFolder.exists())
				testRoot = subFolder;
			else {
				// Try a sibling folder
				testRoot = new File(testRoot.getParentFile(), "soot-infoflow-summaries");
			}
		}
		if (!new File(testRoot, "testSummaries").exists())
			throw new RuntimeException(String.format("Test root not found in %s", testRoot.getAbsolutePath()));
		return testRoot;
	}

}
