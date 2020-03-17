package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Parameter;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Return;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class ParaToReturnTests extends TestHelper {
	protected static Map<String, Set<MethodFlow>> flows;
	static final String className = "soot.jimple.infoflow.test.methodSummary.ParaToReturn";
	static boolean executeSummary = true;

	@Test(timeout = 100000)
	public void primitiv() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: int return1(int)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void primitivRec() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: int returnRec(int,int)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void object() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return2(java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void list() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.util.List return3(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void list2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return31(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] { LINKEDLIST_FIRST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertTrue(containsFlow(flow, Parameter, 0, new String[] { LINKEDLIST_LAST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertEquals(2, flow.size());
	}

	@Test(timeout = 100000)
	public void list3() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return31(java.util.LinkedList)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] { LINKEDLIST_FIRST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertTrue(containsFlow(flow, Parameter, 0, new String[] { LINKEDLIST_LAST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertEquals(2, flow.size());
	}

	@Test(timeout = 100000)
	public void array1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return4(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void array2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object[] return5(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] {}, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void data1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return6(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] { DATACLASS_OBJECT_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void data2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToReturn: java.lang.Object return7(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Parameter, 0, new String[] { DATACLASS_OBJECT_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = super.getSummary();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.LinkedList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(-1);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(true);
		return sg;
	}
}
