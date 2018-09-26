package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Field;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Return;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class FieldToReturnTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.FieldToReturn";

	static final String INT_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int intField>";
	static final String OBJ_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object obField>";
	static final String LIST_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.util.LinkedList listField>";
	static final String OBJ_ARRAY = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object[] arrayField>";
	static final String DATA_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: soot.jimple.infoflow.test.methodSummary.Data dataField>";
	static final String INT_ARRAY = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int[] intArray>";

	@Test(timeout = 100000)
	public void fieldToReturn1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int fieldToReturn()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object fieldToReturn2()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn3() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.util.List fieldToReturn3()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn4() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object fieldToReturn4()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, LINKEDLIST_FIRST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, LINKEDLIST_LAST, LINKEDLIST_ITEM }, Return,
				new String[] {}));
		assertEquals(2, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn5() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object fieldToReturn5()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn5Rec() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object fieldToReturn5Rec(int)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn6() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object[] fieldToReturn6()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn7() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int fieldToReturn7()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_ARRAY }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn8() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int[] fieldToReturn8()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_ARRAY }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn9() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: int fieldToReturn9()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(
				containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_INT_FIELD }, Return, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn10() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.Object fieldToReturn10()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Return,
				new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn11() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: java.lang.String fieldToReturn11()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_STRING_FIELD }, Return,
				new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn12() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: " + DATACLASS_SIG
				+ " fieldToReturn12()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_INT_FIELD }, Return,
				new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn13() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: " + DATACLASS_SIG
				+ " fieldToReturn13()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Return,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToReturn14() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToReturn: " + DATACLASS_SIG
				+ " fieldToReturn14()>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_FIELD }, Return, new String[] { DATACLASS_INT_FIELD }));
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
