package soot.jimple.infoflow.test.methodSummary;

import java.util.LinkedList;
import java.util.List;

public class ParaToReturn {

	public int returnRec(int taint, int count) {
		if (count == 4)
			return taint;
		if (count < 4)
			return returnRec(taint, count + 1);
		return returnRec(taint, count - 1);
	}

	public int return1(int i) {
		return i;
	}

	public Object return2(Object i) {
		return i;
	}

	public List<Object> return3(List<Object> i) {
		return i;
	}

	public Object return31(List<Object> i) {
		return i.get(3);
	}

	public Object return31(LinkedList<Object> i) {
		return i.get(3);
	}

	public Object return4(Object[] i) {
		return i[3];
	}

	public Object[] return5(Object[] i) {
		return i;
	}

	public Object return6(Data i) {
		return i.objectField;
	}

	public Object return7(Data i) {
		return i.getO();
	}
}
