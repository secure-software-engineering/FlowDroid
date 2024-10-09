package soot.jimple.infoflow.integration.test.junit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.dexpler.DalvikThrowAnalysis;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.integration.test.junit.river.BaseJUnitTests;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraphFactory;

/**
 * Tests that uncovered a bug.
 */
public class AndroidRegressionTests extends BaseJUnitTests {
	@Override
	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			return TaintWrapperFactory.createTaintWrapperFromFiles(Collections
					.singleton(new File(getIntegrationRoot(), "../soot-infoflow-summaries/summariesManual")));
		} catch (IOException | XMLStreamException e) {
			throw new RuntimeException("Could not initialize taint wrapper!");
		}
	}

	@Override
	protected void setConfiguration(InfoflowConfiguration config) {
	}

	/**
	 * Tests that the alias analysis correctly stops when an overwrite happens
	 */
	@Test
	public void testFlowSensitivityWithOverwrite() throws IOException {
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/flowsensitiveOverwrite.apk"));
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(2, results.size());
		Assert.assertEquals(2, results.getResultSet().size());
	}

	/**
	 * Tests that StubDroid correctly narrows the type when the summary is in a
	 * superclass. See also the comment in
	 * SummaryTaintWrapper#getSummaryDeclaringClass().
	 */
	@Test
	public void testTypeHierarchyFromSummary() throws IOException {
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/TypeHierarchyTest.apk"));
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(1, results.size());
		Assert.assertEquals(1, results.getResultSet().size());
	}

	/**
	 * Tests an app that uses the kotlin collections. Expects four leaks: * From
	 * getDeviceId() in onCreate() to Log.d(String, String) in listFlow(String),
	 * mapFlow(String) and setFlow(String). * From new File in fileFlow() to
	 * Log.d(String, String) in fileFlow(String).
	 */
	@Test
	public void testKotlinAppWithCollections() throws IOException {
		SetupApplication app = initApplication(new File(getIntegrationRoot(), "testAPKs/KotlinCollectionApp.apk"));

		// Make sure we find only one flow per method
		app.addResultsAvailableHandler((cfg, results) -> {
			Set<SootMethod> seenSet = new HashSet<>();
			for (DataFlowResult res : results.getResultSet()) {
				SootMethod sm = cfg.getMethodOf(res.getSink().getStmt());
				Assert.assertFalse(seenSet.contains(sm));
				seenSet.add(sm);
			}
		});

		// Add the sources and sinks
		List<String> ssinks = new ArrayList<>();
		ssinks.add(
				"<android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE -> _SOURCE_");
		ssinks.add("<android.util.Log: int d(java.lang.String,java.lang.String)> -> _SINK_");
		ssinks.add("<kotlin.io.TextStreamsKt: java.util.List readLines(java.io.Reader)> -> _SOURCE_");

		InfoflowResults results = app.runInfoflow(PermissionMethodParser.fromStringList(ssinks));
		Assert.assertEquals(4, results.size());
		Assert.assertEquals(4, results.getResultSet().size());
	}

	/**
	 * Tests that the CallToReturnFunction does not pass on taints that were killed
	 * by a taint wrapper that marked the method as exclusive.
	 */
	@Test
	public void testMapClear() throws IOException {
		SetupApplication app = initApplication(new File(getIntegrationRoot(), "testAPKs/MapClearTest.apk"));
		InfoflowResults results = app
				.runInfoflow(new File(getIntegrationRoot(), "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(0, results.size());
	}

	/**
	 * Tests that the SummaryTaintWrapper correctly applies identity on methods
	 * which have no explicitly defined flow but are in an exclusive class.
	 */
	@Test
	public void testIdentityOverObjectInit() throws IOException, URISyntaxException {
		SetupApplication app = initApplication(new File(getIntegrationRoot(), "testAPKs/identityOverObjectInit.apk"));
		app.setIcfgFactory(new DefaultBiDiICFGFactory() {
			protected BiDiInterproceduralCFG<Unit, SootMethod> getBaseCFG(boolean enableExceptions) {
				// Force Object.<init> to have no callee to prevent that the SkipSystemClassRule
				// adds the incoming taint to the outgoing set
				return new JimpleBasedInterproceduralCFG(enableExceptions, true) {

					protected DirectedGraph<Unit> makeGraph(Body body) {
						return enableExceptions
								? ExceptionalUnitGraphFactory.createExceptionalUnitGraph(body,
										DalvikThrowAnalysis.interproc(), true)
								: new BriefUnitGraph(body);
					}

					@Override
					public Collection<SootMethod> getCalleesOfCallAt(Unit u) {
						Stmt stmt = (Stmt) u;
						if (stmt.containsInvokeExpr() && stmt.getInvokeExpr().getMethod().getSignature()
								.equals("<java.lang.Object: void <init>()>g"))
							return Collections.emptySet();
						return super.getCalleesOfCallAt(u);
					}
				};
			}
		});
		SummaryTaintWrapper tw = new SummaryTaintWrapper(
				new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR) {
					@Override
					public boolean mayHaveSummaryForMethod(String subsig) {
						// Force the issue
						return false;
					}
				});
		app.setTaintWrapper(tw);

		List<String> ssinks = new ArrayList<>();
		ssinks.add(
				"<android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE -> _SOURCE_");
		ssinks.add("<android.util.Log: int i(java.lang.String,java.lang.String)> -> _SINK_");

		InfoflowResults results = app.runInfoflow(PermissionMethodParser.fromStringList(ssinks));
		Assert.assertEquals(1, results.size());
	}

	/**
	 * Tests that button callbacks declared in the XML file are correctly added to
	 * the lifecycle model when the app is compiled with newer Android API versions.
	 */
	@Test
	public void XMLCallbackAPI33() throws IOException {
		SetupApplication app = initApplication(new File(getIntegrationRoot(), "testAPKs/XMLCallbackAPI33.apk"));

		List<String> ssinks = new ArrayList<>();
		ssinks.add("<java.util.Locale: java.lang.String getCountry()> -> _SOURCE_");
		ssinks.add("<android.util.Log: int i(java.lang.String,java.lang.String)> -> _SINK_");
		InfoflowResults results = app.runInfoflow(PermissionMethodParser.fromStringList(ssinks));
		Assert.assertEquals(1, results.size());
	}

	@Test
	public void testThreadRunnable() throws IOException {
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/ThreadRunnable.apk"));
		// TODO: add support for parameter mismatch/virtualedges.xml
		Assume.assumeTrue("There is no mechanism to fixup access paths at return edges",
				app.getConfig().getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Forwards);
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(1, results.size());
	}

	@Test
	public void testThreadRunnableIndirect() throws IOException {
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/ThreadRunnableIndirect.apk"));
		// TODO: add support for parameter mismatch/virtualedges.xml
		Assume.assumeTrue("There is no mechanism to fixup access paths at return edges",
				app.getConfig().getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Forwards);
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(1, results.size());
	}
}
