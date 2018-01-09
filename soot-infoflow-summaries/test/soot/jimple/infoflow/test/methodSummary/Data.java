package soot.jimple.infoflow.test.methodSummary;

public class Data {
	Object objectField = new Integer(3);
	Object objectField2 = new Integer(5);
	Data next = null;
	int value;
	int value2;
	public Data2 b;
	public String stringField;
	public String stringField2;
	
	public Data() {
		
	
	}
	public void switchData(){
		Object tmp  = objectField;
		objectField = objectField2;
		objectField2 =tmp;
	}
	public void switchSwitch(){
		next.switchData();
	}
	
	public Data(Object o, int v) {
		objectField = o;
		value = v;
	}
	public Object getData() {
		return objectField;
	}
	public void setData(Object data) {
		this.objectField = data;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public void setO(Object o){
		objectField = o;
	}
	public Object getO(){
		return objectField;
	}
	public String getI() {
		return stringField;
	}
	public void setI(String i) {
		this.stringField = i;
	}
	
	
}
