package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.options.Options;

public class SummaryTaintWrapperTests {
	private static String appPath, libPath;

	private String[] source = new String[] {
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: java.lang.Object source()>",
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: int intSource()>",
			"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: java.lang.String stringSource()>" };
	private String sink = "<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void sink(java.lang.Object)>";
	private ITaintPropagationWrapper summaryWrapper;

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();
	}

	@Ignore("kill flow")
	@Test(timeout = 30000)
	public void noFlow1() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void noFlow1()>");
	}

	@Ignore("kill flow")
	@Test(timeout = 30000)
	public void noFlow2() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void noFlow2()>");
	}

	@Test(timeout = 30000)
	public void flow1() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void flow1()>");
	}

	@Test(timeout = 30000)
	public void flow2() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void flow2()>");
	}

	@Test(timeout = 30000)
	public void paraReturnFlow() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraReturnFlow()>");
	}

	@Test(timeout = 30000)
	public void paraFieldSwapFieldReturnFlow() {
		testFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraFieldSwapFieldReturnFlow()>");
	}

	@Test(timeout = 30000)
	public void paraFieldFieldReturnFlow() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraFieldFieldReturnFlow()>");
	}

	@Test(timeout = 30000)
	public void paraReturnFlowInterface() {
		testFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraReturnFlowOverInterface()>");
	}

	@Test(timeout = 30000)
	public void paraFieldSwapFieldReturnFlowInterface() {
		testFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraFieldSwapFieldReturnFlowOverInterface()>");
	}

	@Test // (timeout = 30000)
	public void paraFieldFieldReturnFlowInterface() {
		testFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraFieldFieldReturnFlowOverInterface()>");
	}

	@Test(timeout = 30000)
	public void paraToParaFlow() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void paraToParaFlow()>");
	}

	@Test(timeout = 30000)
	public void fieldToParaFlow() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void fieldToParaFlow()>");
	}

	@Test(timeout = 30000)
	public void apl3NoFlow() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void apl3NoFlow()>");
	}

	@Test(timeout = 30000)
	public void apl3Flow() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void apl3Flow()>");
	}

	@Test(timeout = 30000)
	public void gapFlow1() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void gapFlow1()>");
	}

	@Test(timeout = 30000)
	public void gapFlow2() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void gapFlow2()>");
	}

	@Test(timeout = 30000)
	@Ignore // there is no ordering of same-level flows
	public void shiftTest() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void shiftTest()>");
	}

	@Test(timeout = 30000)
	public void gapFlowUserCode1() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void gapFlowUserCode1()>");
	}

	@Test(timeout = 30000)
	public void transferStringThroughDataClass1() {
		testFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void transferStringThroughDataClass1()>");
	}

	@Test(timeout = 30000)
	public void transferStringThroughDataClass2() {
		testNoFlowForMethod(
				"<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void transferStringThroughDataClass2()>");
	}

	@Test(timeout = 30000)
	public void storeStringInGapClass() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void storeStringInGapClass()>");
	}

	@Test(timeout = 30000)
	public void storeAliasInGapClass() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void storeAliasInGapClass()>");
	}

	@Test(timeout = 30000)
	public void storeAliasInGapClass2() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void storeAliasInGapClass2()>");
	}

	@Test(timeout = 30000)
	public void storeAliasInSummaryClass() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void storeAliasInSummaryClass()>");
	}

	@Test(timeout = 30000)
	public void getLength() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void getLength()>");
	}

	@Test(timeout = 30000)
	public void gapToGap() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void gapToGap()>");
	}

	@Test(timeout = 30000)
	public void callToCall() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void callToCall()>");
	}

	@Test(timeout = 30000)
	public void objectOutputStream1() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void objectOutputStream1()>");
	}

	@Test(timeout = 30000)
	public void objectOutputStream2() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void objectOutputStream2()>");
	}

	@Test(timeout = 30000)
	public void killTaint1() {
		testFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void killTaint1()>");
	}

	@Test(timeout = 30000)
	public void killTaint2() {
		testNoFlowForMethod("<soot.jimple.infoflow.test.methodSummary.ApiClassClient: void killTaint2()>");
	}

	@Test
	public void testAllSummaries() throws URISyntaxException, IOException {
		EagerSummaryProvider provider = new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR);
		assertFalse(provider.hasLoadingErrors());
	}

	private void testFlowForMethod(String m) {
		Infoflow iFlow = null;
		try {
			iFlow = initInfoflow();
			iFlow.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
			iFlow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(Collections.singletonList(m)),
					Arrays.asList(source), Collections.singletonList(sink));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		checkInfoflow(iFlow, 1);
	}

	private void testNoFlowForMethod(String m) {
		Infoflow iFlow = null;
		try {
			iFlow = initInfoflow();
			iFlow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(Collections.singletonList(m)),
					Arrays.asList(source), Collections.singletonList(sink));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		checkNoInfoflow(iFlow);
	}

	private void checkNoInfoflow(Infoflow infoflow) {
		assertTrue(!infoflow.isResultAvailable() || infoflow.getResults().size() == 0);
	}

	private void checkInfoflow(Infoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();

			assertTrue(map.containsSinkMethod(sink));
			assertTrue(map.isPathBetweenMethods(sink, source[0]) || map.isPathBetweenMethods(sink, source[1])
					|| map.isPathBetweenMethods(sink, source[2]));
			assertEquals(resultCount, map.size());
		} else {
			fail("result is not available");
		}
	}

	protected Infoflow initInfoflow() throws FileNotFoundException, XMLStreamException {
		Infoflow result = new Infoflow();
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

		Set<String> summaryFiles = new HashSet<String>();
		summaryFiles.add("./testSummaries/soot.jimple.infoflow.test.methodSummary.ApiClass.xml");
		summaryFiles.add("./testSummaries/soot.jimple.infoflow.test.methodSummary.GapClass.xml");
		summaryFiles.add("./testSummaries/soot.jimple.infoflow.test.methodSummary.Data.xml");
		summaryFiles.add("./testSummaries/soot.jimple.infoflow.test.methodSummary.TestCollection.xml");
		summaryFiles.add("./summariesManual");

		summaryWrapper = TaintWrapperFactory.createTaintWrapper(summaryFiles);
		result.setTaintWrapper(summaryWrapper);
		return result;
	}

	@BeforeClass
	public static void setUp() throws IOException {
		File f = new File(".");
		File testSrc1 = new File(f, "bin");
		File testSrc2 = new File(f, "testBin");
		File testSrc3 = new File(f, "build" + File.separator + "classes");
		File testSrc4 = new File(f, "build" + File.separator + "testclasses");

		if (!(testSrc1.exists() || testSrc2.exists() || testSrc3.exists() || testSrc4.exists())) {
			fail("Test aborted - none of the test sources are available");
		}

		StringBuilder appPathBuilder = new StringBuilder();
		appendWithSeparator(appPathBuilder, testSrc1);
		appendWithSeparator(appPathBuilder, testSrc2);
		appendWithSeparator(appPathBuilder, testSrc3);
		appendWithSeparator(appPathBuilder, testSrc4);
		appPath = appPathBuilder.toString();

		libPath = System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
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

}
