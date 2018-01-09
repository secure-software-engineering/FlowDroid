package soot.jimple.infoflow.test.methodSummary;

import heros.solver.Pair;


public class ApiClass implements IApiClass {
	private int primitiveVariable;

	private String stringField;
	private Object objectField;
	private Data dataField = new Data();
	private Data dataField2 = new Data();
	@SuppressWarnings("unused")
	private static int staticIntField;
	@SuppressWarnings("unused")
	private static Data staticDataField = new Data();

	Node first ;
	private static class Node {
		Object item;
	}
	
	public Object get() {
		return first.item;
	}
	
	public void set(Node data) {
		first = data;
	}
	public Data getNonPrimitiveVariable() {
		return dataField;
	}

	public Object getNonPrimitive1Data() {
		return getNonPrimitive2Variable().objectField;
	}

	// standard flow: Source Paramter -> Sink return X
	public int standardFlow(int i) {
		return i;
	}

	public Object standardFlow(Object i) {
		return i;
	}

	public int standardFlow2(int i, int j) {
		return i + j;
	}

	public int standardFlow2Com(int i, int j) {
		int tmp1 = i + 3;
		int tmp2 = j + 7;
		int tmp3 = tmp1 * 15;
		int tmp4 = tmp2 - 10 / 2;
		return tmp4 + tmp3;
	}

	public int standardFlow3(Data data) {
		return data.value;
	}

	public Data standardFlow5(Object o) {
		Data data = new Data();
		data.setData(o);
		return data;
	}

	public Data standardFlow6(Object o) {
		Data data = new Data();
		data.objectField = o;
		return data;
	}

	public Data standardFlow7(Data o) {
		if (o.value > 3) {
			return o;
		} else {
			Data data = new Data();
			// data.data = o.data;
			return data;
		}
	}

	public Data standardFlow8(Data o) {
		Data data = new Data();
		data.objectField = o.objectField;
		return data;
	}

	public Data standardFlow9(Data d, Object o) {
		Data data = d;
		data.objectField = o;
		return data;
	}

	public Data standardFlow10(Data d) {
		Data data = d;
		return data;
	}

	public Data standardFlow11(Data d) {
		Data data = new Data();
		data.objectField = d.objectField;
		return data;
	}

	public Data standardFlow4(int i, Object o) {
		if (i > 3)
			return new Data(o, i);
		if (i == -3)
			return new Data(o, i);
		return new Data(new Object(), 3);
	}

	// standard static flow: Source Paramter -> Sink return X (but static
	// method)
	public static int staticStandardFlow1(int i, int j) {
		return i + j;
	}

	public static Data staticStandardFlow2(int i, Object o) {
		if (i > 3)
			return new Data(o, i);
		if (i == -3)
			return new Data(o, i);
		return new Data(new Object(), 3);
	}

	// Some no flow methods
	public int noFlow(int i) {
		return 3;
	}

	public int noFlow2(int i, int j) {
		int a = i + j;
		a = 3;
		return a;
	}

	public Data noFlow3(Data data) {
		return new Data();
	}

	public Data noFlow4(int i, Object o) {
		if (i > 3)
			return new Data();
		return new Data(new Object(), 3);
	}

	// paraToVar Flow: Source Para -> Sink global Var
	public int paraToVar(int i, int j) {
		primitiveVariable = i + j;
		return 3;
	}

	public Data paraToVar2(int i, Object o) {
		if (i > 3)
			return new Data(o, i);
		if (i == -3)
			dataField = new Data(o, i);
		return new Data(new Object(), 3);
	}

	public void paraToField2(int i, Object o) {
		dataField = new Data(o, i);

	}

	public void paraToField(int i) {
		dataField.setValue(i);
	}

	public Data paraToVarX(int i, Object o) {
		dataField = new Data(o, i);
		return new Data(o, i);

	}

	private Object[] objs = new Object[100];

	public void paraToVarY(int i, Object o) {
		objs[i] = o;
	}

	// static paraToVar Flow: Source Para -> Sink global Var (static method)
	public static int staticParaToVar(int i, int j) {
		staticIntField = i + j;
		return 3;
	}

	public static Data staticParaToVar2(int i, Object o) {
		if (i < 3)
			return new Data(o, i);
		if (i == -3)
			staticDataField = new Data(o, i);
		return new Data(new Object(), 3);
	}

	public int paraToStaticVar1(int i, int j) {
		staticIntField = i + j;
		return 3;
	}

	public Data paraToStaticVar2(int i, Object o) {
		if (i > 3)
			return new Data(o, i);
		if (i == -3)
			staticDataField = new Data(o, i);
		return new Data(new Object(), 3);
	}

	// paraToparaFlow: Source Para -> Sink para
	public void paraToparaFlow1(int i, Data o) {
		o.setValue(i);
	}

	public void paraToparaFlow2(int i, Object o, Data data) {
		data.value = i;
		data.objectField = o;
	}

	public void paraToparaFlow3(int i, Object o, Data data, Data data2) {
		data.setValue(i);
		data.setData(o);
		data2.setData(o);
	}

	// staticParaToparaFlow: Source Para -> Sink para
	public static void staticParaToparaFlow1(int i, Data o) {
		o.setValue(i);
	}

	public static void staticParaToparaFlow2(int i, Object o, Data data) {
		data.setValue(i);
		data.setData(o);
	}

	public static void staticParaToparaFlow3(int i, Object o, Data data, Data data2) {
		data.setValue(i);
		data.setData(o);
		data2.setData(o);
	}

	// mix tests
	public Data mixedFlow1(int i, Data data) {
		
		if (data.value > 43) {
			primitiveVariable = data.value;
		} else {
			staticIntField = 3;
		}
		data.value = i;
		return data;
	}

	public Data mixedFlow1small(int i, Data data) {
		data.value = i;
		return data;
	}

	public int intParaToReturn() {
		return primitiveVariable;
	}

	public int intInDataToReturn() {
		return dataField.value;
	}

	public int intInDataToReturn2() {
		return getNonPrimitiveVariable().value;
	}

	public int intInDataToReturn3() {
		return getNonPrimitiveVariable().getValue();
	}

	public Data dataFieldToReturn() {
		return dataField;
	}

	public Object objInDataFieldToReturn() {
		return dataField.getData();
	}

	public Data dataFieldToReturn2() {
		return getNonPrimitiveVariable();
	}

	public Data getNonPrimitive2Variable() {
		return dataField2;
	}

	public void swap() {
		Data t = dataField2;
		dataField2 = dataField;
		dataField = t;
	}

	public void swap2() {
		Data t = dataField2;
		dataField2.objectField = dataField.objectField;
		dataField.value = t.value;
	}

	public void data1ToDate2() {
		dataField2 = dataField;
	}

	public void fieldToPara(Data d) {
		d.value = dataField.value;
	}
	public void fieldToPara2(Data d) {
		d.objectField = dataField.objectField;
	}
	

	public int getPrimitiveVariable() {
		return primitiveVariable;
	}

	public void setPrimitiveVariable(int primitiveVariable) {
		this.primitiveVariable = primitiveVariable;
	}

	public String getStringField() {
		return stringField;
	}

	public void setStringField(String aString) {
		this.stringField = aString;
	}

	public Object getObjectField() {
		return objectField;
	}

	public void setObjectField(Object aObject) {
		this.objectField = aObject;
	}

	public int noThisFlow() {
		return 3;
	}

	public Object noThisFlow2(Data d) {
		d.value = dataField.value;
		return null;
	}

	public Object noThisFlow3() {
		Data t = dataField2;
		dataField2 = dataField;
		dataField = t;
		return new Object();
	}

	public Object mutipleSources() {
		Data data = new Data();
		data.objectField = dataField;
		return new Pair<Object, Object>(data.objectField, dataField.objectField);
	}

	//TODO write test for the following methods
	public Pair<ApiClass, Object> thisToReturn() {
		Pair<ApiClass, Object> t = new Pair<ApiClass, Object>(this, new Object());
		return t;
	}
	
	public Pair<ApiClass, Object> thisAndFieldToReturn() {
		Pair<ApiClass, Object> t = new Pair<ApiClass, Object>(this, dataField);
		return t;
	}
	
	public Pair<Object, Pair<Object, ApiClass>> thisAndFieldToReturn1() {
		Pair<Object, Pair<Object, ApiClass>> t = new Pair<Object, Pair<Object, ApiClass>>(new Object(),
				new Pair<Object, ApiClass>(dataField2.objectField, this));
		return t;
	}

	public Pair<Object, Pair<Object, ApiClass>> thisAndFieldToReturn2() {
		Pair<Object, Pair<Object, ApiClass>> t = new Pair<Object, Pair<Object, ApiClass>>(
				dataField2.objectField, new Pair<Object, ApiClass>(new Object(), this));
		return t;
	}
	
	public void paraToSamePara(Data d){
		d.switchData();
	}
	public void paraToSamePara2(Data d){
		d.switchSwitch();
	}
	public void paraToParaT(Data d, Object o){
		d.next.next.next.objectField = o;
	}
	public void paraToParaT2(Data d, Data d2){
		d.next = d2;
	}
	public void paraToParaT3(Data d, Data d2){
		d.next.next = d2;
	}
	public void paraToParaT4(Data d, Object d2){
		d.next.objectField = d2;
	}
	public void paraToFieldT1(Data d){
		dataField2.next = d;
	}
	public void paraToFieldT2(Data d){
		dataField2.next.next = d;
	}
	public void paraToFieldT22(Data d){
		dataField2.next = d.next;
	}
	public void paraToFieldT3(Data d){
		dataField2.next.next.next = d;
	}
	public void paraToFieldT33(Data d){
		dataField2.next = d.next.next;
	}
	public void paraToParaArray(Object []o ){
		o[3] = o[2];
	}
	
	public void setNonPrimitiveData1APL3(Object d){
		dataField.next.objectField = d;
	}
	public Object getNonPrimitiveData2AP3(){
		return dataField.next.objectField2;
	}
	public Object getNonPrimitiveData1APL3(){
		return dataField.next.objectField;
	}

	
	public void fieldToField1(){
		dataField2 = dataField;
	}
	public void fieldToField2(){
		objectField = dataField.objectField;
	}
	public void fieldToField3(){
		primitiveVariable = dataField2.value;
	}
	public void fieldToField4(){
		dataField2.objectField= objectField;
	}
	
	public void fieldToField5(){
		dataField2.objectField= dataField.objectField;
	}
	
	public String makeString(IGapClass d, String in) {
		return d.callTheGap(in);
	}
	
	public void fillDataObject(IGapClass gap, String in, Data d) {
		gap.fillDataString(in, d);
	}
	
	@Override
	public String toString() {
		return stringField;
	}
	
	public String makeStringUserCodeClass(IUserCodeClass d, String in) {
		return d.callTheGap(in);
	}

	public String shiftTest(Data d1, Data d2) {
		String data = d2.stringField;
		d2.stringField = d1.stringField;
		return data;
	}
	
	@Override
	public String transferStringThroughDataClass(IGapClass gap, String in) {
		Data d = new Data();
		d.stringField = in;
		Data d2 = gap.dataThroughGap(d);
		return d2.stringField;
	}
	
	@Override
	public String transferNoStringThroughDataClass(IGapClass gap, String in) {
		Data d = new Data();
		d.stringField = in;
		Data d2 = gap.dataThroughGap(d);
		return d2.stringField2;
	}

	@Override
	public String storeStringInGapClass(IGapClass gap, String in) {
		gap.storeString(in);
		return gap.retrieveString();
	}

	@Override
	public String storeAliasInGapClass(IGapClass gap, String in) {
		gap.storeData(new Data());
		Data d = gap.retrieveData();
		d.stringField = in;
		Data d2 = gap.retrieveData();
		return d2.stringField;
	}
	
	@Override
	public String storeAliasInGapClass2(IGapClass gap, String in) {
		gap.storeData(new Data());
		Data d = gap.retrieveData();
		d.stringField = in;
		Data d2 = gap.retrieveData();
		d2.stringField2 = d.stringField;
		return d2.stringField;
	}
	
	public void storeData(Data data) {
		this.dataField = data;
	}
	
	public Data retrieveData() {
		return this.dataField;
	}
	
	public int getLength(char[] array) {
		return array.length;
	}
	
	@Override
	public String gapToGap(IUserCodeClass gap, String in) {
		String str = gap.callTheGap(in);
		String str2 = gap.callTheGap(str);
		return str2;
	}
	
	public String callToCall(IGapClass gap, String in) {
		String data = gap.retrieveString();
		System.out.println(data);
		return gap.callTheGap(in);
	}

}
