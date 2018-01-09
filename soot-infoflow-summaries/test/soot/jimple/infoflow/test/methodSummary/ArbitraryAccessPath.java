package soot.jimple.infoflow.test.methodSummary;

public class ArbitraryAccessPath {
	Data nullData ;
	Data nullData2 ;
	Data data = new Data();
	Data data2 = new Data();
	Test1 sourceT1 = new Test1();
	Test1 sinkT1 = new Test1();
	class Test1{
		Test2 t2 = new Test2();
		Object o = new Object();
	}
	class Test2{
		Test3 t3 = new Test3();
		Object o = new Object();
	}
	class Test3{
		Object o = new Object();
		Test4 t4 = new Test4();
	}
	class Test4{
		Object o  = new Object();
	}
	
	
	public ArbitraryAccessPath(){
		data.next = new Data();
		data.next.next = new Data();
		data.next.next.next = new Data();
		data2.next = new Data();
		data2.next.next = new Data();
		data2.next.next.next = new Data();
	}
	
	public Data getNullData() {
		return nullData;
	}

	public Data getData() {
		return data;
	}

	public Data getNullData2() {
		return getNullData().next;
	}
	
	public void setNullData2(Data nullData) {
		getNullData().next = nullData.next;
	}
	public Data getData2() {
		return data.next;
	}
	public void setData2(Data data) {
		this.data.next = data.next;
	}
	
	public Data getNullData3() {
		return nullData.next.next;
	}
	
	public void setNullData3(Data nullData) {
		getNullData().next.next = nullData.next.next;
	}
	public Data getData3() {
		Data d = data;
		d = d.next;
		Data e = d;
		
		Data x = new Data();
		Data y = x;
		x.next = e;
		e = y.next;
		e = e.next;
		return e;
//		return data.next.next;
	}
	public void setData3(Data data) {
		this.data.next.next = data.next.next;
	}
	
	public void setObject(Object d) {
		this.data.next.next.next.objectField = d;
	}

	public void setNullData(Data nullData) {
		this.nullData = nullData;
	}

	public void setData(Data data) {
		this.data = data;
	}
	
	public void getDataViaParameter(Data pdata){
		pdata.next.next.next = data.next.next.next;
	}
	
	public void getNullDataViaParameter(Data data){
		data.next.next.next = nullData.next.next.next;
	}
	
	public void fieldToField(){
		data2.next.next.next = data.next.next.next;
	}
	public void fieldToField2(){
		data2.next = data;
	}
	
	public void nullFieldToField(){
		nullData2.next.next.next = nullData.next.next.next;
	}
	
	public void parameterToParameter(Data p1, Data p2){
		p2.next.next  = p1.next.next.next;
	}

	public Data parameterToReturn(Data p1){
		Data resD = new Data();
		resD.next = new Data();
		resD.next.next = p1.next.next.next;
		return resD;
	}
}
