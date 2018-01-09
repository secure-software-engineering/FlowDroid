package soot.jimple.infoflow.test.methodSummary;

import java.util.List;

public class ParaToParaFlows {
	class InnerClass{
		Object o;

		public Object getO() {
			return o;
		}

		public void setO(Object o) {
			this.o = o;
		}
		
	}
	
	void innerClass(Object o, InnerClass ic){
		ic.o = o;
	}
	void array(Object o, Object[] array){
		array[0] = o;
	}
	void arrayRec(Object o, Object[] array, int count){
		if(count == 4){
			array[count] = o;
		}else{
			arrayRec(o,array,count +1);
		}
	}
	int list(List<Object> data, Object o){
		data.add(o);
		return data.indexOf(o);
	}
	
	int setter(String s, Data d){
		d.setI(s);
		return 3;
	}
	int setter2(int i, Data d){
		d.setValue(i);
		return 3;
	}
	
	

}
