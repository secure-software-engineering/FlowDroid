package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;

/**
 * Target class for the SourceSinkTests
 * 
 * @author Steven Arzt
 */
public class SourceSinkTestCode {
	
	private class Base {
		
		private String x = "foo";
		
		public String toString() {
			return x;
		}
		
	}
	
	private class A extends Base {
		private String data;
		private String data2;
		
		public A(String data) {
			this.data = data;
			this.data2 = "foo";
		}
		
		public String getData2() {
			return data2;
		}
		
		public String getSecret() {
			return "Secret";
		}
		
	}
	
	private class B extends Base {
		
	}
	
	private class C {
		private A a;
		
		public C() {
			a = new A("Hello World");
		}
		
	}
	
	private A getSecret() {
		return new A("Secret");
	}
	
	private B getSecret2() {
		return new B();
	}
	
	public void testDataObject() {
		A a = getSecret();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}
	
	private void doSomething(Object o) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish("" + o);
	}
	
	public void testAccessPathTypes() {
		A a = getSecret();
		doSomething(a);
		B b = getSecret2();
		doSomething(b);
	}
	
	public void testSourceAccessPaths() {
		A a = getSecret();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.getData2());
	}
	
	private void doLeakSecret(String s) {
		System.out.println(s);
	}
	
	public void testSinkAccessPaths() {
		A a = getSecret();
		doLeakSecret(a.data);
	}
	
	public void testSinkAccessPaths2() {
		A a = getSecret();
		a.data2 = a.data;
		doLeakSecret(a.data2);
	}

	public void ifAsSinkTest() {
		long x = System.currentTimeMillis();
		if (x > 100)
			System.out.println("Greater");
		else
			System.out.println("Smaller");
	}
	
	public void sourceToSourceTest() {
		A a = getSecret();
		String s = a.getSecret();
		doLeakSecret(s);
	}
	
	private void source(A a) {
		a.data = "Hello World";
	}

	private void annotatedSource(Object o) { }
	
	public void parameterSourceTest1() {
		A a = new A("x");
		source(a);
		doLeakSecret(a.data);
	}
	
	public void parameterSourceTest2() {
		C c = new C();
		A a = c.a;
		source(a);
		doLeakSecret(c.a.data);
	}

	public void parameterSourceTest3() {
		C c = new C();
		A a = c.a;
		annotatedSource(a.data);
		doLeakSecret(c.a.data);
	}

	public void parameterSourceTest4() {
		C c = new C();
		annotatedSource(c.a.data);
		A a = c.a;
		doLeakSecret(a.data);
	}

}
