package soot.jimple.infoflow.collections.test.junit;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import soot.Value;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;

public class AndroidTests extends FlowDroidTests {

	@Override
	protected void setConfiguration(InfoflowConfiguration config) {

	}

	@Test
	public void testResourceResolving() throws IOException {
		File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/Collections/StringResourcesTest.apk"));
		app.getConfig().getPathConfiguration()
				.setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(1, results.size());
		Assert.assertEquals(1, results.getResultSet().size());

		DataFlowResult res = results.getResultSet().stream().findFirst().get();
		for (Stmt stmt : res.getSource().getPath()) {
			if (stmt.containsInvokeExpr()) {
				String subSig = stmt.getInvokeExpr().getMethod().getSubSignature();
				if ((subSig.contains("get") || subSig.contains("put")) && stmt.getInvokeExpr().getArgCount() > 0) {
					Value arg0 = stmt.getInvokeExpr().getArg(0);
					Assert.assertTrue(arg0 instanceof Constant);
				}
			}
		}
	}

	@Test
	public void testConstants() throws IOException {
		File rootDir = getIntegrationRoot();
		SetupApplication app = initApplication(new File(rootDir, "testAPKs/Collections/AppWithConstantFields.apk"));
		app.getConfig().getPathConfiguration()
				.setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
		InfoflowResults results = app.runInfoflow(new File(rootDir, "../soot-infoflow-android/SourcesAndSinks.txt"));
		Assert.assertEquals(3, results.size());
		Assert.assertEquals(3, results.getResultSet().size());
	}

}
