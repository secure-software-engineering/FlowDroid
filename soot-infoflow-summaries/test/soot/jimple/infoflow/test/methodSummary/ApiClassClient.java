package soot.jimple.infoflow.test.methodSummary;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;

public class ApiClassClient {
	public Object source() {
		return "99";
	}

	public String stringSource() {
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

	public void sink(Object out) {
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

}
