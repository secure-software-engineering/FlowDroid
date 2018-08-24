package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class InheritanceTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.InheritanceTests";

	@Test(timeout = 100000)
	public void abstractClassTest1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.InheritanceTests: java.lang.String abstractClassTest1(java.lang.String)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		// Flow from parameter to return value
		assertTrue(containsFlow(flow, SourceSinkType.Parameter, 0, null, "", SourceSinkType.Return, -1, null, ""));

		assertEquals(1, flow.size());
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.LinkedList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		return sg;
	}

}
