package soot.jimple.infoflow.test.methodSummary;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiClassClient {
	public Object source() {
		return "99";
	}

	public String stringSource() {
		return "99";
	}

	public String stringSource2() {
		return "99";
	}

	public int intSource() {
		return 99;
	}

	public void paraReturnFlow() {
		ApiClass api = new ApiClass();
		Object s = source();
		Object tmp = api.standardFlow(s);
		sink(tmp);
	}

	public void paraFieldFieldReturnFlow() {
		ApiClass api = new ApiClass();
		Object s = source();
		api.paraToVar2(-3, s);
		Object tmp = api.objInDataFieldToReturn();
		sink(tmp);
	}

	public void noFlow1() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		api.setStringField(s);
		api.setStringField(null);
		Object tmp = api.getStringField();
		sink(tmp);
	}

	public void flow1() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		api.setStringField(s);
		Object tmp = api.getStringField();
		sink(tmp);
	}

	public void noFlow2() {
		ApiClass api = new ApiClass();
		int s = intSource();
		api.setPrimitiveVariable(s);
		api.setPrimitiveVariable(0);
		Object tmp = api.getPrimitiveVariable();
		sink(tmp);
	}

	public void flow2() {
		ApiClass api = new ApiClass();
		int s = intSource();
		api.setPrimitiveVariable(s);
		Object tmp = api.getPrimitiveVariable();
		sink(tmp);
	}

	public void paraFieldSwapFieldReturnFlow() {
		ApiClass api = new ApiClass();
		Object s = source();
		api.paraToVar2(-3, s);
		api.swap();
		Object tmp = api.getNonPrimitive2Variable().getData();
		sink(tmp);
	}

	public void paraReturnFlowOverInterface() {
		IApiClass api = new ApiClass();
		Object s = source();
		Object tmp = api.standardFlow(s);
		sink(tmp);
	}

	public void paraFieldFieldReturnFlowOverInterface() {
		IApiClass api = new ApiClass();
		Object s = source();
		api.paraToVar2(-3, s);
		Object tmp = api.objInDataFieldToReturn();
		sink(tmp);
	}

	public void paraFieldSwapFieldReturnFlowOverInterface() {
		IApiClass api = new ApiClass();
		Object s = source();
		api.paraToVar2(-3, s);
		api.swap();
		Object tmp = api.getNonPrimitive2Variable().getData();
		sink(tmp);
	}

	public void paraFieldSwapFieldReturnFlowOverInterface(IApiClass api) {
		Object s = source();
		api.paraToVar2(-3, s);
		api.swap();
		Object tmp = api.getNonPrimitive2Variable().getData();
		sink(tmp);
	}

	public void paraToParaFlow() {
		ApiClass api = new ApiClass();
		Data data = new Data();
		Object s = source();
		api.paraToparaFlow2(3, s, data);
		api.swap();
		Object tmp = data.getData();
		sink(tmp);
	}

	public void fieldToParaFlow() {
		ApiClass api = new ApiClass();
		Data data = new Data();
		Object s = source();
		api.paraToVarX(3, s);
		api.fieldToPara2(data);
		Object tmp = data.getData();
		sink(tmp);
	}

	public void apl3NoFlow() {
		ApiClass api = new ApiClass();
		Object s = source();
		api.setNonPrimitiveData1APL3(s);
		Object tmp = api.getNonPrimitiveData2AP3();
		sink(tmp);
	}

	public void apl3Flow() {
		ApiClass api = new ApiClass();
		Object s = source();
		api.setNonPrimitiveData1APL3(s);
		Object tmp = api.getNonPrimitiveData1APL3();
		sink(tmp);
	}

	public void gapFlow1() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.makeString(new GapClass(), s);
		sink(t);
	}

	public void gapFlow2() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		Data d = new Data();
		api.fillDataObject(new GapClass(), s, d);
		sink(d.stringField);
	}

	public void gapFlowUserCode1() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.makeStringUserCodeClass(new UserCodeClass(), s);
		sink(t);
	}

	public void shiftTest() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		Data d1 = new Data();
		Data d2 = new Data();
		d1.stringField = s;
		String t = api.shiftTest(d1, d2);
		sink(t);
	}

	public static void sink(Object out) {
		System.out.println(out);
	}

	public void transferStringThroughDataClass1() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.transferStringThroughDataClass(new GapClass(), s);
		sink(t);
	}

	public void transferStringThroughDataClass2() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.transferNoStringThroughDataClass(new GapClass(), s);
		sink(t);
	}

	public void storeStringInGapClass() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.storeStringInGapClass(new GapClass(), s);
		sink(t);
	}

	public void storeAliasInGapClass() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.storeAliasInGapClass(new GapClass(), s);
		sink(t);
	}

	public void storeAliasInGapClass2() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		String t = api.storeAliasInGapClass2(new GapClass(), s);
		sink(t);
	}

	public void storeAliasInSummaryClass() {
		ApiClass api = new ApiClass();

		Data d = new Data();
		api.storeData(d);

		String s = stringSource();
		api.retrieveData().stringField = s;

		String t = api.retrieveData().stringField;
		sink(t);
	}

	public void getLength() {
		ApiClass api = new ApiClass();
		char[] array = new char[intSource()];
		int length = api.getLength(array);
		sink(length);
	}

	public void gapToGap() {
		ApiClass api = new ApiClass();
		String s = stringSource();
		sink(api.gapToGap(new UserCodeClass(), s));
	}

	public void callToCall() {
		ApiClass api = new ApiClass();
		sink(api.callToCall(new GapClass(), stringSource()));
	}

	public void objectOutputStream1() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		PutField pf = oos.putFields();
		pf.put("Test", source());
		sink(bos.toByteArray());
	}

	public void objectOutputStream2() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		PutField pf = oos.putFields();
		pf.put("Test", source());
		oos.writeFields();
		sink(bos.toByteArray());
	}

	public void killTaint1() {
		TestCollection collection = new TestCollection();
		collection.add(source());
		sink(collection.get());
	}

	public void killTaint2() {
		TestCollection collection = new TestCollection();
		collection.add(source());
		collection.clear();
		sink(collection.get());
	}

	public void taintedFieldToString() {
		Data d = new Data();
		d.objectField = source();
		// in: d.objectField
		// expected out: str (not str.objectField!)
		String str = d.toString();
		char c = str.charAt(2);
		sink(c);
	}

	public void bigIntegerToString() {
		BigInteger i = new BigInteger(stringSource());
		String str = i.toString();
		char c = str.charAt(2);
		sink(c);
	}

	public void mapToString() {
		Map<String, String> map = new HashMap<>();
		map.put("Secret", stringSource());
		String str = map.toString();
		char c = str.charAt(2);
		sink(c);
	}

	public void iterator() {
		List<String> lst = new ArrayList<>();
		lst.add(stringSource());
		Iterator<String> it = lst.iterator();
		if (it.hasNext())
			sink(it.next());
	}

	private static void overwrite(Data d) {
		d.stringField = "";
	}

	public void noPropagationOverUnhandledCallee() {
		Data d = new Data();
		d.stringField = stringSource();
		overwrite(d);
		sink(d.stringField);
	}

	public void identityIsStillAppliedOnUnhandledMethodButExclusiveClass() {
		Data d = new Data();
		d.stringField = stringSource();
		d.identity();
		sink(d.stringField);
	}

	public void matchGapReturnOnlyWithReturnTaints() {
		Data d = new Data();
		d.stringField = stringSource();
		String ret = d.computeString((s) -> "Untainted string");
		sink(ret);
	}

	public void iterativeApplyIsOverapproximation() {
		Map<String, String> map = new HashMap<>();
		// The summary map.values -> return should only be applied
		// if the incoming taint is map.values. But if the incoming
		// taint is param1, then the iterative approach yields
		// param1 -> map.values -> return, which over-approximates Map.put.
		String oldVal = map.put("XXX", stringSource());
		sink(oldVal);
	}

	public void doubleSetterCall() {
		Data d = new Data();
		d.setData(stringSource());
		d.setData(stringSource2());
		sink(d.getData());
	}

	public void stringAppendCall() {
		Data d = new Data();
		d.appendString(stringSource());
		d.appendString(stringSource2());
		sink(d.getString());
	}

	public void streamWriteRead() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);

		String taintedElement = list.stream().findFirst().orElse("anyOther");
		sink(taintedElement);
	}

	public void streamMapTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.stream().map(s -> sinkAndReturn(s));
	}

	public void streamCollectTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		StringBuilder sb = list.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
		sink(sb.toString());
	}

	public void streamCollectTest2() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add("Hello ");
		list.add(" ");
		list.add("World");
		StringBuilder sb = list.stream().collect(() -> new StringBuilder(tainted), StringBuilder::append,
				StringBuilder::append);
		sink(sb.toString());
	}

	public void streamCollectTest3() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		StringBuilder sb = list.stream().collect(() -> new StringBuilder(), (r, s) -> r.append(s),
				(r, s) -> r.append(s));
		sink(sb.toString());
	}

	public void streamFilterTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		Set<String> strings = list.stream().filter(s -> s.startsWith("x")).collect(Collectors.toSet());
		sink(strings.toString());
	}

	public void streamFilterTest2() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().filter(s -> checkAndLeak(s));
	}

	public void streamForEachTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().forEach(s -> checkAndLeak(s));
	}

	public void streamIterateTest() {
		Stream<String> stream = Stream.iterate("", s -> stringSource());
		stream.forEach(s -> checkAndLeak(s));
	}

	public void streamIterateTest2() {
		Stream<String> stream = Stream.iterate("", s -> stringSource());
		sink(stream.findFirst().get());
	}

	public void streamIterateTest3() {
		Stream<String> stream = Stream.iterate(stringSource(), s -> s);
		sink(stream.findFirst().get());
	}

	public void streamIterateTest4() {
		Stream<String> stream = Stream.iterate(stringSource(), s -> s);
		stream.forEach(s -> checkAndLeak(s));
	}

	private boolean checkAndLeak(String s) {
		sink(s);
		return true;
	}

	private String sinkAndReturn(String s) {
		sink(s);
		return s;
	}

	public void streamMaxTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().max((r, s) -> sinkAndReturnInt(s));
	}

	public void streamMaxTest2() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		sink(list.stream().max((r, s) -> r.compareTo(s)).get());
	}

	private int sinkAndReturnInt(String s) {
		sink(s);
		return 42;
	}

	public void streamNoneMatchTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().noneMatch(s -> sinkAndReturnBoolean(s));
	}

	private boolean sinkAndReturnBoolean(String s) {
		sink(s);
		return true;
	}

	public void streamReduceTest() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().reduce((r, s) -> concatAndLeak(r, s));
	}

	private String concatAndLeak(String r, String s) {
		sink(r);
		return r + s;
	}

	public void streamReduceTest2() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().reduce((r, s) -> concatAndLeak2(r, s));
	}

	public void streamReduceTest3() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(" ");
		list.add("World");
		sink(list.stream().reduce(tainted, (r, s) -> r + s));
	}

	public void streamReduceTest4() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		sink(list.stream().reduce(tainted, (r, s) -> r + s, (t, u) -> t + u));
	}

	public void streamReduceTest5() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().reduce("", (r, s) -> concatAndLeak(r, s), (t, u) -> t + u);
	}

	public void streamReduceTest6() {
		String tainted = stringSource();
		List<String> list = new ArrayList<>();
		list.add(tainted);
		list.add(" ");
		list.add("World");
		list.stream().reduce("", (t, u) -> t + u, (r, s) -> concatAndLeak(r, s));
	}

	private String concatAndLeak2(String r, String s) {
		sink(s);
		return r + s;
	}

}
