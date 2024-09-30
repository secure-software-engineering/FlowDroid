package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

/**
 * Tests various combinations of conditional OutputStreams/Writers
 *
 * @author Tim Lange
 */
public class OutputStreamTests extends RiverBaseJUnitTests {
	private ISourceSinkManager getSourceSinkManager(IInfoflow infoflow) {
		try {
			File rootDir = getIntegrationRoot();
			File sourcesSinks = new File(rootDir, "./build/classes/res/OutputStreamAndWriters.xml");
			if (!sourcesSinks.exists())
				sourcesSinks = new File(rootDir, "./res/OutputStreamAndWriters.xml");
			if (!sourcesSinks.exists())
				throw new RuntimeException("Source/sink definition file not found");
			XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile(sourcesSinks.getAbsolutePath());
			return new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			return TaintWrapperFactory.createTaintWrapperFromFiles(Collections
					.singleton(new File(getIntegrationRoot(), "../soot-infoflow-summaries/summariesManual")));
		} catch (IOException | XMLStreamException e) {
			throw new RuntimeException("Could not initialized Taintwrapper:");
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

	@Test(timeout = 300000)
	public void testBufferedOutputStream1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testBufferedOutputStream1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testBufferedOutputStream2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testBufferedOutputStream2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testObjectOutputStream1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testObjectOutputStream1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testObjectOutputStream2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testObjectOutputStream2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testDataOutputStream1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testDataOutputStream1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testDataOutputStream2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testDataOutputStream2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testOutputStreamWriter1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testOutputStreamWriter1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testOutputStreamWriter2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testOutputStreamWriter2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testPrintWriter1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testPrintWriter1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testPrintWriter2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testPrintWriter2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteBufferOutput1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteBufferOutput1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteBufferOutput2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteBufferOutput2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteBufferOutput3() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteBufferOutput3()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteBufferOutput4() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteBufferOutput4()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteBufferOutput5() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteBufferOutput5()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testByteArrayExcludedTest1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testByteArrayExcludedTest1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
		Assert.assertNull(infoflow.getResults().getAdditionalResultSet());
	}

	@Test(timeout = 300000)
	public void testExternalFile1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testExternalFile1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testExternalFile2() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testExternalFile2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testInternalFile1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testInternalFile1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void testPrintWriter3() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testPrintWriter3()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testFileWriter1() {
		IInfoflow infoflow = this.initInfoflow();
		List<String> epoints = new ArrayList<>();
		epoints.add("<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testFileWriter1()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				getSourceSinkManager(infoflow));
		this.checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 30000)
	public void testCastWithSummaryTypeInformation1() {
		IInfoflow infoflow = this.initInfoflow();
		infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
		infoflow.getConfig().setAdditionalFlowsEnabled(false);
		List<String> epoints = new ArrayList<>();
		epoints.add(
				"<soot.jimple.infoflow.integration.test.OutputStreamTestCode: void testCastWithSummaryTypeInformation1()>");
		final String source = "<java.io.FileOutputStream: void <init>(java.lang.String)>";
		final String sink = "<java.io.OutputStream: void write(int)>";
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				Collections.singletonList(source), Collections.singletonList(sink));
		this.checkInfoflow(infoflow, 1);
	}
}
