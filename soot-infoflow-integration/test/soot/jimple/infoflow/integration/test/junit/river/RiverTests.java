package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import soot.Local;
import soot.Scene;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.river.AdditionalFlowInfoSpecification;
import soot.jimple.infoflow.river.IConditionalFlowManager;
import soot.jimple.infoflow.river.IUsageContextProvider;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.toolkits.scalar.Pair;

/**
 * Contains tests derived from the example in the paper and some more.
 *
 * @author Tim Lange
 */
public abstract class RiverTests extends RiverBaseJUnitTests {
	protected static List<String> sources;
	protected static final String localSource = "<soot.jimple.infoflow.integration.test.RiverTestCode: java.lang.String source()>";
	protected static final String localIntSource = "<soot.jimple.infoflow.integration.test.RiverTestCode: int intSource()>";

	protected static List<String> primarySinks;
	protected static final String osWrite = "<java.io.OutputStream: void write(byte[])>";
	protected static final String osWriteInt = "<java.io.OutputStream: void write(int)>";
	protected static final String writerWrite = "<java.io.Writer: void write(java.lang.String)>";
	protected static final String bufosWrite = "<java.io.FilterOutputStream: void write(byte[])>";

	protected static final String sendToUrl = "<soot.jimple.infoflow.integration.test.RiverTestCode: void sendToUrl(java.net.URL,java.lang.String)>";
	protected static final String uncondSink = "<soot.jimple.infoflow.integration.test.RiverTestCode: void unconditionalSink(java.lang.String)>";

	protected static final String urlInit = "<java.net.URL: void <init>(java.lang.String)>";

	{
		sources = new ArrayList<String>();
		sources.add(localSource);
		sources.add(localIntSource);

		primarySinks = new ArrayList<String>();
		primarySinks.add(osWrite);
		primarySinks.add(osWriteInt);
		primarySinks.add(writerWrite);
		primarySinks.add(sendToUrl);
		primarySinks.add(uncondSink);
		primarySinks.add(bufosWrite);
	}

	protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			Assert.assertEquals(resultCount, map.size());
			Assert.assertTrue(primarySinks.stream().anyMatch(map::containsSinkMethod));
			Assert.assertTrue(
					primarySinks.stream().flatMap(sink -> sources.stream().map(source -> new Pair<>(sink, source)))
							.anyMatch(p -> map.isPathBetweenMethods(p.getO1(), p.getO2())));
		} else {
			Assert.fail("result is not available");
		}
	}

	protected void negativeCheckInfoflow(IInfoflow infoflow) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			Assert.assertEquals(0, map.size());
			Assert.assertTrue(primarySinks.stream().noneMatch(map::containsSinkMethod));
		}
	}

	private ISourceSinkManager getSourceSinkManager(IInfoflow infoflow) {
		try {
			XMLSourceSinkParser parser = XMLSourceSinkParser
					.fromFile(new File(getIntegrationRoot(), "./build/classes/res/RiverSourcesAndSinks.xml"));
			return new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// Test condition met
	@Test(timeout = 300000)
	public void riverTest1() throws InterruptedException {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	// Test condition not met
	@Test(timeout = 300000)
	public void riverTest2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	// Test that we accept all conditional sinks if additional flows are disabled
	@Test(timeout = 300000)
	public void riverTest2b() {
		IInfoflow infoflow = this.initInfoflow();
		infoflow.getConfig().setAdditionalFlowsEnabled(false);
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	// Test condition met and the conditional sink is in a superclass
	@Test // (timeout = 300000)
	public void riverTest3() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest3()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	// Test condition not met and the conditional sink is in a superclass
	@Test(timeout = 300000)
	public void riverTest4() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest4()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	// Test condition met
	@Test(timeout = 300000)
	public void riverTest5() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest5()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	// Test condition not met
	@Test(timeout = 300000)
	public void riverTest6() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest6()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	// Example from the paper
	@Test(timeout = 300000)
	public void riverTest7() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest7()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
	}

	// Test Usage Contexts
	@Test(timeout = 300000)
	public void riverTest8() {
		IInfoflow infoflow = this.initInfoflow();

		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest8()>");
		ISourceSinkManager ssm = getSourceSinkManager(infoflow);
		infoflow.addPreprocessor(new PreAnalysisHandler() {
			@Override
			public void onBeforeCallgraphConstruction() {

			}

			@Override
			public void onAfterCallgraphConstruction() {
				((IConditionalFlowManager) ssm)
						.registerSecondarySink(Scene.v().grabMethod("<java.net.URL: void <init>(java.lang.String)>"));
			}
		});

		infoflow.setUsageContextProvider(new IUsageContextProvider() {
			@Override
			public Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt, Set<Abstraction> taints) {
				if (stmt.containsInvokeExpr()) {
					String sig = stmt.getInvokeExpr().getMethod().getSignature();
					if (sig.equals(sendToUrl)) {
						Local local = (Local) stmt.getInvokeExpr().getArg(0);
						return Collections.singleton(new AdditionalFlowInfoSpecification(local, stmt));
					}
				}
				return Collections.emptySet();
			}

			@Override
			public boolean isStatementWithAdditionalInformation(Stmt stmt, Abstraction abs) {
				return false;
			}
		});

		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
		this.checkInfoflow(infoflow, 1);

		// Check that the usage context was found
		Assert.assertTrue(infoflow.getResults().getAdditionalResultSet().stream()
				.anyMatch(dfResult -> dfResult.getSource().getStmt().containsInvokeExpr()
						&& dfResult.getSource().getStmt().getInvokeExpr().getMethod().getSignature().equals(sendToUrl)
						&& dfResult.getSink().getStmt().containsInvokeExpr()
						&& dfResult.getSink().getStmt().getInvokeExpr().getMethod().getSignature().equals(urlInit)));
	}

	@Test(timeout = 300000)
	public void riverTest8b() {
		IInfoflow infoflow = this.initInfoflow();

		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest8()>");

		infoflow.setUsageContextProvider(new IUsageContextProvider() {
			@Override
			public Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt, Set<Abstraction> taints) {
				if (stmt.containsInvokeExpr()) {
					String sig = stmt.getInvokeExpr().getMethod().getSignature();
					if (sig.equals(sendToUrl)) {
						Local local = (Local) stmt.getInvokeExpr().getArg(0);
						return Collections.singleton(new AdditionalFlowInfoSpecification(local, stmt));
					}
				}
				return Collections.emptySet();
			}

			@Override
			public boolean isStatementWithAdditionalInformation(Stmt stmt, Abstraction abs) {
				if (!stmt.containsInvokeExpr())
					return false;

				return stmt.getInvokeExpr().getMethod().getSignature().contains("java.net.URL: void <init>");
			}
		});

		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);

		// Check that the usage context was found
		Assert.assertTrue(infoflow.getResults().getAdditionalResultSet().stream()
				.anyMatch(dfResult -> dfResult.getSource().getStmt().containsInvokeExpr()
						&& dfResult.getSource().getStmt().getInvokeExpr().getMethod().getSignature().equals(sendToUrl)
						&& dfResult.getSink().getStmt().containsInvokeExpr()
						&& dfResult.getSink().getStmt().getInvokeExpr().getMethod().getSignature().equals(urlInit)));
	}

	// Test that unconditional sinks still work
	@Test(timeout = 300000)
	public void riverTest9() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest9()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	// Test className not on path
	@Test(timeout = 300000)
	public void riverTest10() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest10()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	// Test className on path
	@Test(timeout = 300000)
	public void riverTest11() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.RiverTestCode: void riverTest11()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

}
