package soot.jimple.infoflow.test.methodSummary;

import java.util.LinkedList;
import java.util.List;

public class ParaToField {
	int intField = 1;
	Object obField;
	List<Object> listField = new LinkedList<Object>();
	Object[] arrayField = new Object[100];
	int[] intArray = new int[100];
	public Data dataField = new Data();

	public ParaToField() {
		dataField = new Data();
		listField = new LinkedList<Object>();
	}
	void intPara(int i) {
		intField = i;
		dataField.setValue(i);
		intArray[3] = i;
	}

	int intPara2(int i) {
		dataField.setValue(i);
		return dataField.value;
	}
	void intParaRec(int i, int count) {
		if (count == 3) {
			intField = i;
			dataField.value = i;
			intArray[3] = i;
		} else {
			intParaRec(i, count - 1);
		}
	}

	void objPara(Object o) {
		obField = o;
		dataField.objectField = o;
		arrayField[3] = o;
		listField.add(o);
	}

	void intAndObj(int i, Object o) {
		intField = i;
		dataField.value = i;
		intArray[3] = i;
		obField = o;
		dataField.objectField = o;
		arrayField[3] = o;
		listField.add(o);
	}

	void arrayParas(int[] i, Object[] o) {
		intField = i[3];
		dataField.value = i[2];
		intArray = i;
		obField = o;
		dataField.setO(o);
		arrayField[3] = o[5];
		listField.add(o[1]);
	}

	void dataAndList(Data d, List<Object> list) {
		intField = d.getValue();
		dataField.setValue(d.value);
		intArray[5] = d.value;
		obField = list.get(0);
		dataField.objectField = d.getO();
		arrayField[3] = d.objectField;
		listField.add(list.get(0));
	}
	
	void setIntArray(Data d, List<Object> list) {
		intArray[5] = d.value;
	}
	int intArrayRet(){
		return intArray[5];
	}
	void data(Data d) {
		intField = d.getValue();
	}
	public void intParaToData(int i) {
		dataField.value = i;
	}
	
	void contextSensitivity(Data d) {
		this.intField = id(d.value);
		this.intArray[0] = id(d.value2);
	}
	
	private int id(int value) {
		int x = value;
		int y = x + 1;
		return y - 1;
	}
	
}
