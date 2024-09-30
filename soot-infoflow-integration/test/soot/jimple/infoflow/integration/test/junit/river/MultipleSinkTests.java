package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Tests the merging and separating of SourceSinkDefinitions referecing the same
 * statement or method with path reconstruction enabled
 */
public class MultipleSinkTests extends RiverBaseJUnitTests {
	private ISourceSinkManager getSourceSinkManager(IInfoflow infoflow) {
		try {
			XMLSourceSinkParser parser = XMLSourceSinkParser
					.fromFile(new File(getIntegrationRoot(), "./build/classes/res/MultipleSinkDefs.xml"));
			// Hacky way to get the access paths in the source sink xml working
			InfoflowAndroidConfiguration aconfig = new InfoflowAndroidConfiguration();
			aconfig.merge(infoflow.getConfig());
			return new AccessPathBasedSourceSinkManager(parser.getSources(), parser.getSinks(), aconfig);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			EasyTaintWrapper easyWrapper = EasyTaintWrapper.getDefault();
			easyWrapper.addIncludePrefix("soot.jimple.infoflow.integration.test.MultipleSinkTestCode$MyClass");
			// Add methods used in the test cases
			easyWrapper.addMethodForWrapping("soot.jimple.infoflow.integration.test.MultipleSinkTestCode$MyClass",
					"void conditionalSink(java.lang.String)");
			easyWrapper.addMethodForWrapping("soot.jimple.infoflow.integration.test.MultipleSinkTestCode$MyClass",
					"void contextOne()");
			easyWrapper.addMethodForWrapping("soot.jimple.infoflow.integration.test.MultipleSinkTestCode$MyClass",
					"void contextTwo()");
			return easyWrapper;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			Assert.assertEquals(resultCount, map.size());
		} else {
			Assert.fail("result is not available");
		}
	}

	protected void negativeCheckInfoflow(IInfoflow infoflow) {
		if (infoflow.isResultAvailable()) {
			InfoflowResults map = infoflow.getResults();
			Assert.assertEquals(0, map.size());
		}
	}

	enum SourceOrSink {
		SOURCE, SINK, BOTH
	}

	protected void containsCategoriesOnce(Set<DataFlowResult> results, String[] categoriesArray, SourceOrSink sos) {
		Set<String> categories = new HashSet<>();
		for (String category : categoriesArray)
			categories.add(category);
		Set<String> seenCategories = new HashSet<>(categories.size());

		for (DataFlowResult res : results) {
			String categoryID;
			if (sos == SourceOrSink.SOURCE)
				categoryID = res.getSourceCategoryID();
			else if (sos == SourceOrSink.SINK)
				categoryID = res.getSinkCategoryID();
			else if (sos == SourceOrSink.BOTH) {
				Assert.assertEquals(res.getSinkCategoryID(), res.getSourceCategoryID());
				categoryID = res.getSinkCategoryID();
			} else {
				Assert.fail();
				categoryID = ""; // silence error
			}

			if (seenCategories.contains(categoryID))
				Assert.fail("Category " + categoryID + " is duplicate");

			seenCategories.add(categoryID);
		}

		categories.removeAll(seenCategories);
		Assert.assertEquals(0, categories.size());
	}

	@Test(timeout = 300000)
	public void testNoCondition() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testNoCondition()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void testOneCondition() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testOneCondition()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK" }, SourceOrSink.SINK);
	}

	@Test(timeout = 300000)
	public void testBothCondition() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testBothConditions()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.SINK);
	}

	@Test(timeout = 300000)
	public void testParam1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testParam1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "VOIP" }, SourceOrSink.SINK);
	}

	@Test(timeout = 300000)
	public void testParam2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testParam2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK" }, SourceOrSink.SINK);
	}

	@Test(timeout = 300000)
	public void testBothParams() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testBothParams()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.SINK);
	}

	@Test(timeout = 300000)
	public void testReturn1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testReturn1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK" }, SourceOrSink.BOTH);
	}

	@Test(timeout = 300000)
	public void testReturn2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testReturn2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "VOIP" }, SourceOrSink.BOTH);
	}

	@Test(timeout = 300000)
	public void testBothReturns() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testBothReturns()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.SINK);
	}

	@Test(timeout = 30000)
	public void testMatchingAccessPaths() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testMatchingAccessPaths()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.BOTH);
	}

	@Test(timeout = 30000)
	public void testMismatchingAccessPaths() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testMismatchingAccessPaths()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void testParamAsSource() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testParamAsSource()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.BOTH);
	}

	@Test(timeout = 300000)
	public void testParamAsSink() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.MultipleSinkTestCode: void testParamAsSink()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 2);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().size());
		containsCategoriesOnce(infoflow.getResults().getResultSet(), new String[] { "NETWORK", "VOIP" },
				SourceOrSink.SINK);
	}
}
