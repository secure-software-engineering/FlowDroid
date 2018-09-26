package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class ParaToParaTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.ParaToParaFlows";

	@Test(timeout = 100000)
	public void array() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: void array(java.lang.Object,java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Parameter, 1, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void arrayRec() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: void arrayRec(java.lang.Object,java.lang.Object[],int)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Parameter, 1, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void list() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: int list(java.util.List,java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 1, new String[] {}, Parameter, 0,
				new String[] { LINKEDLIST_FIRST, LINKEDLIST_ITEM }));
		assertTrue(containsFlow(flow, Parameter, 1, new String[] {}, Parameter, 0,
				new String[] { LINKEDLIST_LAST, LINKEDLIST_ITEM }));
		assertTrue(containsFlow(flow, Parameter, 0, new String[] { LINKEDLIST_LAST }, Parameter, 0,
				new String[] { LINKEDLIST_FIRST }));
		assertEquals(3, flow.size());
	}

	@Ignore
	public void list2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: int list(java.util.List,java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 1, new String[] {}, Parameter, 1,
				new String[] { LINKEDLIST_FIRST, LINKEDLIST_ITEM }));
		assertTrue(containsFlow(flow, Parameter, 1, new String[] {}, Parameter, 1,
				new String[] { LINKEDLIST_LAST, LINKEDLIST_ITEM }));
		assertEquals(2, flow.size());
	}

	@Test(timeout = 100000)
	public void setter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: int setter(java.lang.String,soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Parameter, 1,
				new String[] { DATACLASS_STRING_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void setter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: int setter2(int,soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(
				containsFlow(flow, Parameter, 0, new String[] {}, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void innerClass() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows: void innerClass(java.lang.Object,soot.jimple.infoflow.test.methodSummary.ParaToParaFlows$InnerClass)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Parameter, 1, new String[] {
				"<soot.jimple.infoflow.test.methodSummary.ParaToParaFlows$InnerClass: java.lang.Object o>" }));
		assertEquals(1, flow.size());
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.LinkedList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(true);
		return sg;
	}
}
