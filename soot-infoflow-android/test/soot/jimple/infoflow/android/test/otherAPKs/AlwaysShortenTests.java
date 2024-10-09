package soot.jimple.infoflow.android.test.otherAPKs;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.results.InfoflowResults;

public class AlwaysShortenTests extends soot.jimple.infoflow.android.test.droidBench.JUnitTests {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}

	@Test(timeout = 300000)
	public void runTestAnonymousClass1Insensitive() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks/AnonymousClass1.apk", null, config -> {
			config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.FlowInsensitive);
		});

		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size()); // loc + lat, but single parameter
		Assert.assertEquals(2, res.getResultSet().size());
	}
}
