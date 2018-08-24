package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

/**
 * Test cases for making sure that we don't loose context sensitivity somewhere,
 * especially during path reconstruction.
 * 
 * @author Steven Arzt
 *
 */
public class ContextSensitivityTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.ContextSensitivity";

	static final String FIELD_X = "<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: java.lang.String x>";
	static final String FIELD_Y = "<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: java.lang.String y>";
	static final String FIELD_Z = "<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: java.lang.String z>";

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.ArrayList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(5);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(true);
		return sg;
	}

	@Test(timeout = 100000)
	public void contextSensitivity1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: void contextSensitivity1(soot.jimple.infoflow.test.methodSummary.Data,java.lang.String,java.lang.String)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Parameter, 1, null, Parameter, 0, new String[] { DATACLASS_STRING_FIELD }));
		assertTrue(containsFlow(flow, Parameter, 2, null, Parameter, 0, new String[] { DATACLASS_STRING_FIELD2 }));
		assertEquals(2, flow.size());
	}

	@Test(timeout = 100000)
	public void recursionTest1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: java.lang.String recursionTest1(java.lang.String)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		// Field y to field z
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_Y }, SourceSinkType.Field, -1,
				new String[] { FIELD_Z }));
		// Field x to field y
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_Y }, SourceSinkType.Field, -1,
				new String[] { FIELD_X }));
		// Transitivity: Field x to field z
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_X }, SourceSinkType.Field, -1,
				new String[] { FIELD_Z }));

		// On first recursive call: Field y -> str -> Field x (no strong updates on
		// fields)
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_Y }, SourceSinkType.Field, -1,
				new String[] { FIELD_X }));

		// Parameter 0 to x
		assertTrue(containsFlow(flow, Parameter, 0, null, SourceSinkType.Field, -1, new String[] { FIELD_X }));
		// Transitivity: Parameter 0 to y
		assertTrue(containsFlow(flow, Parameter, 0, null, SourceSinkType.Field, -1, new String[] { FIELD_Y }));
		// Transitivity: Parameter 0 to z
		assertTrue(containsFlow(flow, Parameter, 0, null, SourceSinkType.Field, -1, new String[] { FIELD_Z }));

		// Parameter 0 to return value
		assertTrue(containsFlow(flow, Parameter, 0, null, SourceSinkType.Return, null));

		// Fields to return value
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_X }, SourceSinkType.Return, -1,
				null));
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_Y }, SourceSinkType.Return, -1,
				null));
		assertTrue(containsFlow(flow, SourceSinkType.Field, -1, new String[] { FIELD_Z }, SourceSinkType.Return, -1,
				null));

		assertEquals(11, flow.size());
	}

}
