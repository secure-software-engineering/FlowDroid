package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class AndroidRiverTests extends RiverBaseJUnitTests {

	@Override
	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			return TaintWrapperFactory.createTaintWrapperFromFiles(Collections
					.singletonList(new File(getIntegrationRoot(), "../soot-infoflow-summaries/summariesManual")));
		} catch (IOException | XMLStreamException e) {
			throw new RuntimeException("Could not initialized Taintwrapper:");
		}
	}

	@Test(timeout = 300000)
	public void conditionalTestApk() throws IOException {
		// The test apk has two java.io.OutputStream: void write(byte[]) sinks.
		// One is located in the KeepFlow activity and is a stream to the internet.
		// The other one is in the DiscardFlow activity and is a ByteArrayOutputStream.
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/ConditionalFlowTest.apk"));
		XMLSourceSinkParser parser = XMLSourceSinkParser
				.fromFile(new File(rootDir, "./build/classes/res/AndroidRiverSourcesAndSinks.xml"));
		InfoflowResults results = app.runInfoflow(parser);
		Assert.assertEquals(2, results.size());

		// Check that the flow is in the right activity
		SootMethod sm1 = Scene.v()
				.grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void onCreate(android.os.Bundle)>");
		SootMethod sm2 = Scene.v()
				.grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void leakToInternet(byte[])>");
		SootMethod sm3 = Scene.v()
				.grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void leakToExternalFile(byte[])>");
		Set<Unit> units = new HashSet<>();
		units.addAll(sm1.getActiveBody().getUnits());
		;
		units.addAll(sm2.getActiveBody().getUnits());
		;
		units.addAll(sm3.getActiveBody().getUnits());
		for (DataFlowResult result : results.getResultSet())
			Assert.assertTrue(Arrays.stream(result.getSource().getPath()).allMatch(units::contains));
	}

	@Test(timeout = 300000)
	public void externalFileWithNativeNameApk() throws IOException {
		// The test apk logs to an external file that is constructed with
		// getExternalDir(null). The flow looks as follows:
		// path = getExternalDir(null).getAbsolutePath()
		// f = new File(path + jniCall);
		// FileWriter fw = new FileWriter(f);
		// BufferedWriter bw = new BufferedWriter(fw);
		// fw.append(tainted);
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/ExternalFileWithNativeName.apk"));
		XMLSourceSinkParser parser = XMLSourceSinkParser
				.fromFile(new File(rootDir, "./build/classes/res/AndroidRiverSourcesAndSinks.xml"));
		InfoflowResults results = app.runInfoflow(parser);
		Assert.assertEquals(1, results.size());
	}

	@Test(timeout = 300000)
	public void printWriterTestApk() throws IOException {
		// Also see OutputStreamTestCode#testPrintWriter3 but this time in Android
		// because Soot generates different jimple for Android and Java.
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/PrintWriterTest.apk"));
		app.getConfig().setWriteOutputFiles(true);
		XMLSourceSinkParser parser = XMLSourceSinkParser
				.fromFile(new File(rootDir, "./build/classes/res/AndroidRiverSourcesAndSinks.xml"));
		InfoflowResults results = app.runInfoflow(parser);
		Assert.assertEquals(1, results.size());
	}

	@Test
	public void externalCacheDirTest() throws IOException {
		// Test flow with getExternalCacheDir wrapped in another File constructor
		final File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/ExternalCacheDirTest.apk"));
		XMLSourceSinkParser parser = XMLSourceSinkParser
				.fromFile(new File(rootDir, "./build/classes/res/AndroidRiverSourcesAndSinks.xml"));
		InfoflowResults results = app.runInfoflow(parser);
		Assert.assertEquals(1, results.size());
	}

}
