package soot.jimple.infoflow.test.methodSummary;

public class Pair {
	
	private Object o1;
	private Object o2;

	public Pair(Object o1, Object o2) {
		this.o1 = o1;
		this.o2 = o2;
	}

	public Object getO1() {
		return this.o1;
	}

	public void setComplex(Data a) {
		this.o1 = a.b.c;
	}
	
	public Object getO2() {
		return this.o2;
	}
	
}
