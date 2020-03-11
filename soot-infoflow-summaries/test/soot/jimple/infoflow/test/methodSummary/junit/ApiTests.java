package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Field;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Parameter;
import static soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType.Return;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class ApiTests extends ApiTestHelper {
	static final String className = "soot.jimple.infoflow.test.methodSummary.ApiClass";

	@Test(timeout = 100000)
	public void standardFlow1() {
		String mSig = "<" + className + ": int standardFlow(int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow11() {
		String mSig = "<" + className + ": int standardFlow(int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow2() {
		String mSig = "<" + className + ": int standardFlow2(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, null));
	}

	@Test(timeout = 100000)
	public void standardFlow2Com() {
		String mSig = "<" + className + ": int standardFlow2Com(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
	}

	@Test(timeout = 100000)
	public void standardFlow22() {
		String mSig = "<" + className + ": int standardFlow2(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow3() {
		String mSig = "<" + className + ": int standardFlow3(" + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, new String[] { DATACLASS_INT_FIELD }, Return, null));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow31() {
		String mSig = "<" + className + ": int standardFlow3(" + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow4() {
		String mSig = "<" + className + ": " + DATA_TYPE + " standardFlow4(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void standardFlow6() {
		String mSig = "<" + className + ": " + DATA_TYPE + " standardFlow6(java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void standardFlow8() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ApiClass: soot.jimple.infoflow.test.methodSummary.Data standardFlow8(soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, new String[] { DATACLASS_OBJECT_FIELD }, Return,
				new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void staticStandardFlow1() {
		String mSig = "<" + className + ": int staticStandardFlow1(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, null));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void staticStandardFlow11() {
		String mSig = "<" + className + ": int staticStandardFlow1(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, null));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void staticStandardFlow2() {
		String mSig = "<" + className + ": " + DATA_TYPE + " staticStandardFlow2(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, new String[] { DATACLASS_OBJECT_FIELD }));
		// assertTrue(containsParaToReturn(res, 0, INT_TYPE, NO_ACCESS_PATH,
		// DATACLASS_INT_FIELD));
		// assertTrue(containsParaToReturn(res, 1, OBJECT_TYPE, NO_ACCESS_PATH,
		// DATACLASS_OBJECT_FIELD));
		// assertTrue(res.size() == 2);
	}

	@Test(timeout = 100000)
	public void staticStandardFlow21() {
		String mSig = "<" + className + ": " + DATA_TYPE + " staticStandardFlow2(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void noFlow() {
		String mSig = "<" + className + ": int noFlow(int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(res == null || res.size() == 0);
	}

	@Test(timeout = 100000)
	public void noFlow2() {
		String mSig = "<" + className + ": int noFlow2(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(res == null || res.size() == 0);
	}

	@Test(timeout = 100000)
	public void noFlow3() {
		String mSig = "<" + className + ": " + DATA_TYPE + " noFlow3(" + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(res == null || res.size() == 0);
	}

	@Test(timeout = 100000)
	public void noFlow4() {
		String mSig = "<" + className + ": " + DATA_TYPE + " noFlow4(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(res == null || res.size() == 0);
	}

	@Test(timeout = 100000)
	public void paraToVar() {
		String mSig = "<" + className + ": int paraToVar(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Field, new String[] { PRIMITIVE_VAR }));
		assertTrue(containsFlow(res, Parameter, 1, null, Field, new String[] { PRIMITIVE_VAR }));
	}

	@Test(timeout = 100000)
	public void paraToVar12() {
		String mSig = "<" + className + ": int paraToVar(int,int)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Field, new String[] { PRIMITIVE_VAR }));
		assertTrue(containsFlow(res, Parameter, 1, null, Field, new String[] { PRIMITIVE_VAR }));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void paraToVar2() {
		String mSig = "<" + className + ": " + DATA_TYPE + " paraToVar2(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(
				containsFlow(res, Parameter, 0, null, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Field,
				new String[] { NON_PRIMITIVE_VAR1, DATACLASS_OBJECT_FIELD }));
		assertEquals(4, res.size());
	}

	@Test(timeout = 100000)
	public void paraToVar21() {
		String mSig = "<" + className + ": " + DATA_TYPE + " paraToVar2(int,java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(
				containsFlow(res, Parameter, 0, null, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Field,
				new String[] { NON_PRIMITIVE_VAR1, DATACLASS_OBJECT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Return, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(4, res.size());
	}

	@Test(timeout = 100000)
	public void paraToparaFlow1WrongSinkSigAccepted() {
		String mSig = "<" + className + ": void paraToparaFlow1(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void paraToparaFlow2() {
		String mSig = "<" + className + ": void paraToparaFlow2(int,java.lang.Object," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(2, res.size());

	}

	@Test(timeout = 100000)
	public void paraToparaFlow3WrongSinkSigAccepted() {
		String mSig = "<" + className + ": void paraToparaFlow3(int,java.lang.Object," + DATA_TYPE + "," + DATA_TYPE
				+ ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 3, new String[] { DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow1WrongSinkSigAccepted() {
		String mSig = "<" + className + ": void staticParaToparaFlow1(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow2WrongSinkSigAccepted() {
		String mSig = "<" + className + ": void staticParaToparaFlow2(int,java.lang.Object," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow3WrongSinkSigAccepted() {
		String mSig = "<" + className + ": void staticParaToparaFlow3(int,java.lang.Object," + DATA_TYPE + ","
				+ DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void mixedFlow1() {
		String mSig = "<" + className + ": " + DATA_TYPE + " mixedFlow1(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 1, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, new String[] { DATACLASS_INT_FIELD }, Field,
				new String[] { PRIMITIVE_VAR }));
		assertEquals(4, res.size());
	}

	@Test(timeout = 100000)
	public void mixedFlow1small() {
		String mSig = "<" + className + ": " + DATA_TYPE + " mixedFlow1small(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 1, null, Return, null));
		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 0, null, Return, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(3, res.size());
	}

	@Test(timeout = 100000)
	public void paraToparaFlow1() {
		String mSig = "<" + className + ": void paraToparaFlow1(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(
				containsFlow(res, Parameter, 0, new String[] {}, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void paraToparaFlow3() {
		String mSig = "<" + className + ": void paraToparaFlow3(int,java.lang.Object," + DATA_TYPE + "," + DATA_TYPE
				+ ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 3, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(3, res.size());
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow1() {
		String mSig = "<" + className + ": void staticParaToparaFlow1(int," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(
				containsFlow(res, Parameter, 0, new String[] {}, Parameter, 1, new String[] { DATACLASS_INT_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow2() {
		String mSig = "<" + className + ": void staticParaToparaFlow2(int,java.lang.Object," + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(2, res.size());
	}

	@Test(timeout = 100000)
	public void staticParaToParaFlow3() {
		String mSig = "<" + className + ": void staticParaToparaFlow3(int,java.lang.Object," + DATA_TYPE + ","
				+ DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Parameter, 2, new String[] { DATACLASS_INT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 2, new String[] { DATACLASS_OBJECT_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, null, Parameter, 3, new String[] { DATACLASS_OBJECT_FIELD }));
		assertEquals(3, res.size());
	}

	@Test(timeout = 100000)
	public void primitivVarToReturn1() {
		String mSig = "<" + className + ": int intParaToReturn()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { PRIMITIVE_VAR }, Return, null));
	}

	@Test(timeout = 100000)
	public void nonPrimitivVarToReturn1() {
		String mSig = "<" + className + ": int intInDataToReturn()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }, Return, null));
	}

	@Test(timeout = 100000)
	public void nonPrimitivVarToReturn11() {
		String mSig = "<" + className + ": int intInDataToReturn()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }, Return, null));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void nonPrimitivVarToReturn2() {
		String mSig = "<" + className + ": int intInDataToReturn2()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }, Return, null));
	}

	@Test(timeout = 100000)
	public void nonPrimitivVarToReturn3() {
		String mSig = "<" + className + ": int intInDataToReturn3()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }, Return, null));
	}

	@Test(timeout = 100000)
	public void nonPrimitivVarToReturn4() {
		String mSig = "<" + className + ": " + DATA_TYPE + " dataFieldToReturn()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1 }, Return, null));
	}

	@Test(timeout = 200000)
	public void nonPrimitivVarToReturn5() {
		String mSig = "<" + className + ": " + DATA_TYPE + " dataFieldToReturn2()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1 }, Return, null));
	}

	@Test(timeout = 100000)
	public void swap() {
		String mSig = "<" + className + ": void swap()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1 }, Field,
				new String[] { NON_PRIMITIVE_VAR2 }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void swap2() {
		String mSig = "<" + className + ": void swap2()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_OBJECT_FIELD }, Field,
				new String[] { NON_PRIMITIVE_VAR2, DATACLASS_OBJECT_FIELD }));
		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR2, DATACLASS_INT_FIELD }, Field,
				new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }));
	}

	@Test(timeout = 100000)
	public void data1ToDate2() {
		String mSig = "<" + className + ": void data1ToDate2()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1 }, Field,
				new String[] { NON_PRIMITIVE_VAR2 }));
	}

	@Test(timeout = 150000)
	public void fieldToPara1() {
		String mSig = "<" + className + ": void fieldToPara(" + DATA_TYPE + ")>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_INT_FIELD }, Parameter, 0,
				new String[] { DATACLASS_INT_FIELD }));
	}

	@Test(timeout = 150000)
	public void ListGetTest() {
		String mSig = "<" + className + ": java.lang.Object get()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] {
				"<soot.jimple.infoflow.test.methodSummary.ApiClass: soot.jimple.infoflow.test.methodSummary.ApiClass$Node first>",
				"<soot.jimple.infoflow.test.methodSummary.ApiClass$Node: java.lang.Object item>" }, Return,
				new String[] {}));
	}

	@Test(timeout = 100000)
	public void fieldToField1() {
		String mSig = "<" + className + ": void fieldToField1()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1 }, Field,
				new String[] { NON_PRIMITIVE_VAR2 }));
	}

	@Test(timeout = 100000)
	public void fieldToField2() {
		String mSig = "<" + className + ": void fieldToField2()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_OBJECT_FIELD }, Field,
				new String[] { OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void fieldToField3() {
		String mSig = "<" + className + ": void fieldToField3()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR2, DATACLASS_INT_FIELD }, Field,
				new String[] { PRIMITIVE_VAR }));
	}

	@Test(timeout = 100000)
	public void fieldToField4() {
		String mSig = "<" + className + ": void fieldToField4()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { OBJECT_FIELD }, Field,
				new String[] { NON_PRIMITIVE_VAR2, DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void fieldToField5() {
		String mSig = "<" + className + ": void fieldToField5()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, new String[] { NON_PRIMITIVE_VAR1, DATACLASS_OBJECT_FIELD }, Field,
				new String[] { NON_PRIMITIVE_VAR2, DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void shiftTest() {
		String mSig = "<" + className
				+ ": java.lang.String shiftTest(soot.jimple.infoflow.test.methodSummary.Data,soot.jimple.infoflow.test.methodSummary.Data)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, new String[] { DATA_STRING_FIELD }, Parameter, 1,
				new String[] { DATA_STRING_FIELD }));
		assertTrue(containsFlow(res, Parameter, 1, new String[] { DATA_STRING_FIELD }, Return, 1, null));
	}

	@Test(timeout = 100000)
	public void storeAliasInGapClass() {
		String mSig = "<" + className
				+ ": java.lang.String storeAliasInGapClass(soot.jimple.infoflow.test.methodSummary.IGapClass,java.lang.String)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Return, -1, new String[] { DATA_STRING_FIELD },
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: soot.jimple.infoflow.test.methodSummary.Data retrieveData()>",
				Return, -1, null, ""));
		assertTrue(containsFlow(res, Parameter, 1, null, "", Return, -1, new String[] { DATA_STRING_FIELD },
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: soot.jimple.infoflow.test.methodSummary.Data retrieveData()>"));
	}

	@Test(timeout = 100000)
	public void setNonPrimitiveData1APL3() {
		String mSig = "<" + className + ": void setNonPrimitiveData1APL3(java.lang.Object)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 0, null, Field, -1,
				new String[] { APICLASS_DATA_FIELD, DATACLASS_OBJECT_FIELD }));
	}

	@Test(timeout = 100000)
	public void getNonPrimitiveData1APL3() {
		String mSig = "<" + className + ": java.lang.Object getNonPrimitiveData1APL3()>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Field, -1, new String[] { APICLASS_DATA_FIELD, DATACLASS_OBJECT_FIELD }, Return,
				-1, null));
	}

	@Test(timeout = 100000)
	public void transferNoStringThroughDataClass() {
		String mSig = "<" + className
				+ ": java.lang.String transferNoStringThroughDataClass(soot.jimple.infoflow.test.methodSummary.IGapClass,java.lang.String)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		final String gapSig = makeGapClassSignature(
				"soot.jimple.infoflow.test.methodSummary.Data dataThroughGap(soot.jimple.infoflow.test.methodSummary.Data)");

		assertTrue(containsFlow(res, Parameter, 1, null, null, Parameter, 0, new String[] { DATACLASS_STRING_FIELD },
				gapSig));
		assertTrue(containsFlow(res, Return, -1, new String[] { DATACLASS_STRING_FIELD2 }, gapSig, Return, -1, null,
				null));
	}

	@Test(timeout = 100000)
	public void makeStringUserCodeClass() {
		String mSig = "<" + className
				+ ": java.lang.String makeStringUserCodeClass(soot.jimple.infoflow.test.methodSummary.IUserCodeClass,java.lang.String)>";
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();

		assertTrue(containsFlow(res, Parameter, 1, null, "", Parameter, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));
		assertTrue(containsFlow(res, Parameter, 0, null, "", SourceSinkType.GapBaseObject, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));
		assertTrue(containsFlow(res, Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Return, -1, null, ""));
	}

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

}
