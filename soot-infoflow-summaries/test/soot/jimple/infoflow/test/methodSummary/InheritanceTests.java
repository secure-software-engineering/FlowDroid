package soot.jimple.infoflow.test.methodSummary;

public class InheritanceTests {
	
	private abstract class Base {
		
		public abstract String doIt(int i, String s);
		
	}
	
	private class Concrete extends Base {

		@Override
		public String doIt(int i, String s) {
			if (i == 0)
				return s;
			return doIt(i - 1, s);
		}
		
	}
	
	public String abstractClassTest1(String in) {
		Base b = new Concrete();
		return b.doIt(10, in);
	}

}
