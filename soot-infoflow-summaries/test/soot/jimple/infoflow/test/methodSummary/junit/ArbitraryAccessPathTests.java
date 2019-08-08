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

public class ArbitraryAccessPathTests extends TestHelper {

	private static final String CLASS_NAME = "soot.jimple.infoflow.test.methodSummary.ArbitraryAccessPath";

	private static final String NULL_FIELD = "<soot.jimple.infoflow.test.methodSummary.ArbitraryAccessPath: soot.jimple.infoflow.test.methodSummary.Data nullData>";
	private static final String NULL_FIELD2 = "<soot.jimple.infoflow.test.methodSummary.ArbitraryAccessPath: soot.jimple.infoflow.test.methodSummary.Data nullData2>";

	private static final String _D = "<soot.jimple.infoflow.test.methodSummary.Data: soot.jimple.infoflow.test.methodSummary.Data next>";
	private static final String _O = "<soot.jimple.infoflow.test.methodSummary.Data: java.lang.Object objectField>";
	private static final String DATA_FIELD = "<soot.jimple.infoflow.test.methodSummary.ArbitraryAccessPath: soot.jimple.infoflow.test.methodSummary.Data data>";
	private static final String DATA_FIELD2 = "<soot.jimple.infoflow.test.methodSummary.ArbitraryAccessPath: soot.jimple.infoflow.test.methodSummary.Data data2>";

	@Test(timeout = 100000)
	public void getNullData() {
		String mSig = mSig(DATACLASS_SIG, "getNullData", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { NULL_FIELD }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getData() {
		String mSig = mSig(DATACLASS_SIG, "getData", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { DATA_FIELD }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getNullData2() {
		String mSig = mSig(DATACLASS_SIG, "getNullData2", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { NULL_FIELD, _D }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getData2() {
		String mSig = mSig(DATACLASS_SIG, "getData2", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { DATA_FIELD, _D }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getNullData3() {
		String mSig = mSig(DATACLASS_SIG, "getNullData3", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(
				containsFlow(res, Field, new String[] { NULL_FIELD, _D, _D }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getData3() {
		String mSig = mSig(DATACLASS_SIG, "getData3", "");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(
				containsFlow(res, Field, new String[] { DATA_FIELD, _D, _D }, SourceSinkType.Return, new String[] {}));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setData() {
		String mSig = mSig("void", "setData", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, null, Field, new String[] { DATA_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setData2() {
		String mSig = mSig("void", "setData2", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, new String[] { _D }, Field, new String[] { DATA_FIELD, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setData3() {
		String mSig = mSig("void", "setData3", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(
				containsFlow(res, Parameter, 0, new String[] { _D, _D }, Field, new String[] { DATA_FIELD, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setNullData() {
		String mSig = mSig("void", "setNullData", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, null, Field, new String[] { NULL_FIELD }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setNullData2() {
		String mSig = mSig("void", "setNullData2", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, new String[] { _D }, Field, new String[] { NULL_FIELD, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setNullData3() {
		String mSig = mSig("void", "setNullData3", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(
				containsFlow(res, Parameter, 0, new String[] { _D, _D }, Field, new String[] { NULL_FIELD, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void setObject() {
		String mSig = mSig("void", "setObject", OBJECT_TYPE);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, null, Field, new String[] { DATA_FIELD, _D, _D, _D, _O }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getDataViaParameter() {
		String mSig = mSig("void", "getDataViaParameter", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { DATA_FIELD, _D, _D, _D }, Parameter, 0,
				new String[] { _D, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void getNullDataViaParameter() {
		String mSig = mSig("void", "getNullDataViaParameter", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { NULL_FIELD, _D, _D, _D }, Parameter, 0,
				new String[] { _D, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void fieldToField() {
		String mSig = mSig("void", "fieldToField");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { DATA_FIELD, _D, _D, _D }, Field,
				new String[] { DATA_FIELD2, _D, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void fieldToField2() {
		String mSig = mSig("void", "fieldToField2");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { DATA_FIELD }, Field, new String[] { DATA_FIELD2, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void nullFieldToField() {
		String mSig = mSig("void", "nullFieldToField");
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Field, new String[] { NULL_FIELD, _D, _D, _D }, Field,
				new String[] { NULL_FIELD2, _D, _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void parameterToParameter() {
		String mSig = mSig("void", "parameterToParameter", DATACLASS_SIG, DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, new String[] { _D, _D, _D }, Parameter, 1, new String[] { _D, _D }));
		assertEquals(1, res.size());
	}

	@Test(timeout = 100000)
	public void parameterToReturn() {
		String mSig = mSig(DATACLASS_SIG, "parameterToReturn", DATACLASS_SIG);
		Set<MethodFlow> res = createSummaries(mSig).getAllFlows();
		assertTrue(containsFlow(res, Parameter, 0, new String[] { _D, _D, _D }, SourceSinkType.Return,
				new String[] { _D, _D }));
		assertEquals(1, res.size());
	}

	private String mSig(String rTyp, String mName) {
		return "<" + CLASS_NAME + ": " + rTyp + " " + mName + "()>";
	}

	private String mSig(String rTyp, String mName, String pTyp) {
		return "<" + CLASS_NAME + ": " + rTyp + " " + mName + "(" + pTyp + ")>";
	}

	private String mSig(String rTyp, String mName, String pTyp, String pTyp2) {
		return "<" + CLASS_NAME + ": " + rTyp + " " + mName + "(" + pTyp + "," + pTyp2 + ")>";
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.ArrayList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		sg.getConfig().getAccessPathConfiguration().setUseSameFieldReduction(false);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(6);
		return sg;
	}

}
