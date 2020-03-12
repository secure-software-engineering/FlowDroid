package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Field;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class FieldToParaTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.FieldToPara";

	static final String OBJ_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: java.lang.Object obField>";
	static final String OBJ_ARRAY = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: java.lang.Object[] arrayField>";
	static final String INT_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: int intField>";
	static final String LIST_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: java.util.List listField>";
	static final String DATA_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: soot.jimple.infoflow.test.methodSummary.Data dataField>";
	static final String APIINTERNAL_FIELD = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: soot.jimple.infoflow.test.methodSummary.ApiInternalClass>";
	static final String ALIST_DATA = "<java.util.ArrayList: java.lang.Object[] elementData>";
	static final String ALIST_SIZE = "<java.util.ArrayList: int size>";
	static final String INT_ARRAY = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: int[] intArray>";

	static final String INTEGER_VALUE_FIELD = "<java.lang.Integer: int value>";

	@Test(timeout = 100000)
	public void fieldToPara1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter2(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara3() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter3(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, ALIST_DATA }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara3333() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: java.lang.Object dataParameter3333(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, ALIST_DATA }, SourceSinkType.Return, 0, null));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara4() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter4(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara5() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter5(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));

		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara6() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter6(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));

		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToPara7() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameter7(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] { DATACLASS_OBJECT_FIELD }));

		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToParaRec1() {
		for (int i = 0; i < 10; i++) {
			String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void dataParameterRec(soot.jimple.infoflow.test.methodSummary.Data,int)>";
			Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
			assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Parameter, 0,
					new String[] { DATACLASS_OBJECT_FIELD }));
			assertEquals(1, flow.size());
		}
	}

	@Test(timeout = 100000)
	public void fieldToArrayParameter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Parameter, 0, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToArrayParameter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter2(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Parameter, 0, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 250000)
	public void fieldToArrayParameter3() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter3(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, ALIST_DATA }, Parameter, 0, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToArrayParameter4() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter4(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToArrayParameter5() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter5(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void fieldToArrayParameter6() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void objArrayParameter6(java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Parameter, 0, new String[] {}));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter1() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_FIELD }, Parameter, 0,
				new String[] { ALIST_DATA, INTEGER_VALUE_FIELD }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter2(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_FIELD }, Parameter, 0, new String[] { ALIST_DATA }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter3() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter3(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { LIST_FIELD, ALIST_DATA }, Parameter, 0,
				new String[] { ALIST_DATA }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter4() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter4(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { OBJ_ARRAY }, Parameter, 0, new String[] { ALIST_DATA }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter5() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter5(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { INT_ARRAY }, Parameter, 0,
				new String[] { ALIST_DATA, "<java.lang.Integer: int value>" }));
		assertEquals(1, flow.size());
	}

	@Test(timeout = 100000)
	public void listFieldToParameter6() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.FieldToPara: void listParameter6(java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(flow, Field, new String[] { DATA_FIELD, DATACLASS_OBJECT_FIELD }, Parameter, 0,
				new String[] { ALIST_DATA }));
		assertEquals(1, flow.size());
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = super.getSummary();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.ArrayList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(5);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(true);
		return sg;
	}
}
