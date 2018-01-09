package soot.jimple.infoflow.test.methodSummary;

import java.util.LinkedList;
import java.util.List;

public class FieldToReturn {
	int intField = 1;
	Object obField = new Object();
	@SuppressWarnings("rawtypes")
	LinkedList listField = new LinkedList();
	Object[] arrayField = new Object[100];
	int[] intArray = new int[100];
	Data dataField =  new Data();
	
	@SuppressWarnings("unchecked")
	public
	FieldToReturn(){
		listField.add(new Object());
		listField.add(new Object());
		listField.add(new Integer(3));
		listField.add(new Object());
		listField.add(new Object());
		listField.add(new Object());
	}
	
	int fieldToReturn(){
		return intField;
	}
	
	Object fieldToReturn2(){
		return obField;
	}
	
	@SuppressWarnings("rawtypes")
	List fieldToReturn3(){
		return listField;
	}
	
	public Object fieldToReturn4(){
		return listField.get(2);
	}
	
	Object fieldToReturn5(){
		return arrayField[3];
	}
	Object fieldToReturn5Rec(int i){
		if( i == 0)
			return arrayField[3];
		return fieldToReturn5Rec(i-1);
	}
	Object[] fieldToReturn6(){
		return arrayField;
	}
	
	int fieldToReturn7(){
		return intArray[3];
	}
	int[] fieldToReturn8(){
		return intArray;
	}
	
	int fieldToReturn9(){
		return dataField.value;
	}
	
	Object fieldToReturn10(){
		return dataField.getO();
	}
	
	String fieldToReturn11(){
		return dataField.stringField;
	}
	Data fieldToReturn12(){
		Data d = new Data();
		d.setValue(dataField.value);
		return d;
	}
	
	Data fieldToReturn13(){
		Data d = new Data();
		d.setO(dataField.getO());
		return d;
	}
	
	Data fieldToReturn14(){
		Data d = new Data();
		d.setValue(intField);
		return d;
	}
}
