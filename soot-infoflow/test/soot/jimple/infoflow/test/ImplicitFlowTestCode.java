package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.Base64;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Code for testing the implicit flow feature
 * 
 * @author Steven Arzt
 *
 */
public class ImplicitFlowTestCode {

	public void simpleTest() {
		String tainted = TelephonyManager.getDeviceId();
		String foo = "";
		if (tainted == "123")
			foo = "x";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void simpleNegativeTest() {
		String tainted = TelephonyManager.getDeviceId();
		String foo = "";
		if (foo.equals("")) {
			if (tainted == "123")
				tainted = "Hello";
			foo = "x";
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void simpleOverwriteTest() {
		String tainted = TelephonyManager.getDeviceId();
		String foo = "";
		if (tainted == "123") {
			tainted = "Hello";
			foo = "x";
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void switchTest() {
		int secret = TelephonyManager.getIMEI();
		String foo = "";
		switch (secret) {
		case 1:
			foo = "x";
			break;
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void convertTest() {
		int secret = TelephonyManager.getIMEI();
		String imei = Integer.toString(secret);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei);
	}

	public void sinkTest() {
		int secret = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		if (secret == 42)
			cm.publish("Secret is 42");
	}

	private boolean lookup(int i) {
		return i == 42;
	}

	public void returnTest() {
		int secret = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		if (lookup(secret))
			cm.publish("Secret is 42");
	}

	private void doPublish() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish("Secret is 42");
	}

	public void callTest() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			doPublish();
	}

	private void doSomething() {
		int i = 0;
		while (i % 2 == 0)
			i++;
	}

	public void callTest2() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42) {
			doSomething();
			secret = 0;
			doPublish();
		}
	}

	public void negativeCallTest() {
		int secret = TelephonyManager.getIMEI();
		int other = 42;
		if (other == 42)
			doPublish();
		if (secret == 42)
			other = 1;
	}

	private void runSimpleRecursion(int i) {
		if (i == 0)
			doPublish();
		else
			runSimpleRecursion(i - 1);
	}

	public void recursionTest() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			runSimpleRecursion(42);
	}

	public void recursionTest2() {
		int secret = TelephonyManager.getIMEI();
		runSimpleRecursion(secret);
	}

	private void recurseIndirect(int i) {
		if (i > 0)
			recurseIndirect2(i--);
		else
			doPublish();
	}

	private void recurseIndirect2(int i) {
		recurseIndirect(i);
	}

	public void recursionTest3() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			recurseIndirect(42);
	}

	public void exceptionTest() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			if (tainted == "123")
				throw new RuntimeException("Secret is 42");
		} catch (RuntimeException ex) {
			doPublish();
		}
	}

	public void exceptionTest2() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			if (tainted == "123")
				throw new RuntimeException("Secret is 42");
		} catch (RuntimeException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex.getMessage());
		}
	}

	public void exceptionTest3() {
		String tainted = TelephonyManager.getDeviceId();
		Throwable t = null;
		if (tainted == "123")
			t = new Throwable();
		if (t != null)
			doPublish();
	}

	private int val = 0;

	private void fieldAccess() {
		this.val = 3;
	}

	public void fieldTest() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			fieldAccess();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(val);
	}

	private static int staticVal = 0;

	private void bar() {
		String x = "Hello World";
		System.out.println(x);
	}

	private void staticFieldAccess() {
		staticVal = 42;
		bar();
	}

	public void staticFieldTest() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			staticFieldAccess();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticVal);
	}

	private static void staticDoPublish() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticVal);
	}

	private static void conditionalStaticFieldAccess(int i) {
		if (i == 42)
			staticVal = 42;
	}

	public void staticFieldTest2() {
		int secret = TelephonyManager.getIMEI();
		conditionalStaticFieldAccess(secret);
		staticDoPublish();
	}

	private static class StaticDataClass {
		StaticDataClass2 data;
	}

	private static StaticDataClass staticDataClass = new StaticDataClass();

	private static class StaticDataClass2 {
		int data;
	}

	private static void conditionalStaticClassAccess() {
		ImplicitFlowTestCode.staticDataClass.data.data = 42;
	}

	public void staticFieldTest3() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			conditionalStaticClassAccess();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(ImplicitFlowTestCode.staticDataClass.data.data);
	}

	private static void conditionalStaticAliasAccess() {
		StaticDataClass data = new StaticDataClass();
		ImplicitFlowTestCode.staticDataClass = data;
		data.data.data = 42;
	}

	public void staticFieldTest4() {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			conditionalStaticAliasAccess();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(ImplicitFlowTestCode.staticDataClass.data.data);
	}

	private static void conditionalStaticAliasAccess(StaticDataClass data) {
		data.data.data = 42;
	}

	public void staticFieldTest5() {
		StaticDataClass data = new StaticDataClass();
		ImplicitFlowTestCode.staticDataClass = data;
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			conditionalStaticAliasAccess(data);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(ImplicitFlowTestCode.staticDataClass.data.data);
	}

	public void integerClassTest() {
		// Not an implicit flow, but used to produce a hickup with implicit
		// flows enabled
		int secret = TelephonyManager.getIMEI();
		Integer i = new Integer(secret);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i);
	}

	public void stringClassTest() {
		// Not an implicit flow, but used to produce a hickup with implicit
		// flows enabled
		String secret = TelephonyManager.getDeviceId();
		int len = secret.length();
		char[] secret2 = new char[len];
		secret.getChars(0, len, secret2, 0);
		ConnectionManager cm = new ConnectionManager();
		// cm.publish(new String(secret2));
		cm.publish(new String(secret));
	}

	private void leavesByException() throws Exception {
		throw new Exception("foobar");
	}

	private void conditional() throws Exception {
		int secret = TelephonyManager.getIMEI();
		if (secret == 42)
			leavesByException();
	}

	public void conditionalExceptionTest() {
		try {
			conditional();
		} catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ImplicitFlowTestCode.staticDataClass.data.data);
		}
	}

	private class A {
		String data;
		int intData;
	}

	private class B {
		A a;
	}

	private void taint(B b) {
		b.a.data = TelephonyManager.getDeviceId();
	}

	public void aliasingTest() {
		B b = new B();
		b.a = new A();
		A a = b.a;
		taint(b);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	private void throwAround(String secret) {
		System.out.println(secret);
	}

	public void passOverTest() {
		String secret = TelephonyManager.getDeviceId();
		throwAround(secret);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(secret);
	}

	public void passOverTest2() {
		String secret = TelephonyManager.getDeviceId();
		int imei = TelephonyManager.getIMEI();
		if (imei == 42)
			throwAround(secret);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(secret);
	}

	public void callToReturnTest() {
		String secret = TelephonyManager.getDeviceId();
		String s1 = "foo";
		String s2 = "bar";
		String res = "";
		if (secret.equals("test"))
			res = String.valueOf(s1);
		else
			res = String.valueOf(s2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(res);
	}

	private void alias(B b1, B b2) {
		b2.a = b1.a;
	}

	public void createAliasInFunctionTest() {
		B b1 = new B();
		b1.a = new A();
		B b2 = new B();
		alias(b1, b2);
		int tainted = TelephonyManager.getIMEI();
		if (tainted == 42)
			b1.a.data = "Hello World";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b2.a.data);
	}

	public void createAliasInFunctionTest2() {
		B b1 = new B();
		b1.a = new A();
		B b2 = new B();
		int tainted = TelephonyManager.getIMEI();
		if (tainted == 42)
			aliasAndTaint(b1, b2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b2.a.data);
	}

	private void aliasAndTaint(B b1, B b2) {
		alias(b1, b2);
		b1.a.data = "foo";
	}

	public void implicitFlowTaintWrapperTest() {
		StringBuilder builder = new StringBuilder();
		builder.append("foo");
		int tainted = TelephonyManager.getIMEI();
		if (tainted == 42)
			builder.append("bar");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(builder.toString());
	}

	public void hierarchicalCallSetTest() {
		A a = new A();
		int tainted = TelephonyManager.getIMEI();
		if (tainted == 42)
			setLevel1(a);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	private void setLevel1(A a) {
		setLevel2(a);
	}

	private void setLevel2(A a) {
		a.data = "foo";
	}

	public void conditionalAliasingTest() {
		B b = new B();
		b.a = new A();
		A a = b.a;
		if (TelephonyManager.getIMEI() == 42)
			setVal(b);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	private void setVal(B b) {
		b.a.data = "foo";
	}

	public void conditionalAliasingTest2() {
		B b = new B();
		b.a = new A();
		A a = new A();
		if (TelephonyManager.getIMEI() == 42)
			setVal(b);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
		a = b.a;
		System.out.println(a);
	}

	private B b1;

	public void conditionalAliasingTest3() {
		b1 = new B();
		b1.a = new A();
		A a = b1.a;
		if (TelephonyManager.getIMEI() == 42)
			setVal();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	private void setVal() {
		b1.a.data = "foo";
	}

	private void setVal(A a) {
		String s = "foo";
		a.data = s;
	}

	public void afterCallNegativeTest() {
		A a = new A();
		if (TelephonyManager.getIMEI() == 42)
			setVal(a);
		String s = "foo";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(s);
	}

	public void ifInCalleeTest() {
		A a = new A();
		a.intData = 42;
		if (TelephonyManager.getIMEI() == 42)
			ifInCallee(a);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	private void ifInCallee(A a) {
		if (a.intData > 0)
			a.data = "foo";
	}

	public void activationConditionalTest() {
		B b = new B();
		b.a = new A();

		A a = b.a;
		if (b.a.intData == 42)
			doPublish();

		a.intData = TelephonyManager.getIMEI();
	}

	private interface I {
		public void leak();
	}

	private class I1 implements I {
		public void leak() {
			doPublish();
		}
	}

	private class I2 implements I {
		public void leak() {
			// Could be a different message, we use the same method as in I1
			// just to simply things
			doPublish();
		}
	}

	public void classTypeTest() {
		I i;
		if (TelephonyManager.getIMEI() == 42)
			i = new I1();
		else
			i = new I2();
		i.leak();
	}

	public void conditionalReturnTest() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(getConditionalValue());
	}

	private String getConditionalValue() {
		return TelephonyManager.getIMEI() == 42 ? "a" : "b";
	}

	private String getBar() {
		return Math.random() < 0.5 ? "bar" : "foobar";
	}

	public void callToReturnTest2() {
		String tainted = "foo";
		if (TelephonyManager.getIMEI() == 42)
			tainted = getBar();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}

	public void stringObfuscationTest1() {
		String pwdString = TelephonyManager.getDeviceId();
		String obfPwd = "";
		for (char c : pwdString.toCharArray())
			obfPwd += c + "_";
		String message = " | PWD: " + obfPwd;
		String message_base64 = Base64.encodeToString(message.getBytes());

		ConnectionManager cm = new ConnectionManager();
		cm.publish(message_base64);
	}

	public void arrayIndexTest1() {
		String[] arr = new String[] { "hello", "world" };
		ConnectionManager cm = new ConnectionManager();
		cm.publish(arr[TelephonyManager.getIMEI()]);
	}

	public void exceptionTest1() {
		int val = TelephonyManager.getIMEI();
		String[] arr = new String[val];
		try {
			arr[42] = "Hello";
		} catch (ArrayIndexOutOfBoundsException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish("Hello World");
		}
	}

	public void exceptionTest4() {
		int val = TelephonyManager.getIMEI();
		String[] arr = new String[val];
		try {
			System.out.println("Hello World");
		} catch (ArrayIndexOutOfBoundsException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish("Hello World");
		}
		arr[42] = "Hello";
	}

	public void userCodeTest1() {
		boolean A = TelephonyManager.getIMEI() > 42;

		boolean a = false;
		boolean b = false;

		if (A) {
			a = true;
		}

		try {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(b);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (a) {
			System.out.println("");
		}
	}

	public void userCodeTest2() {
		boolean A = TelephonyManager.getIMEI() > 42;
		ConnectionManager cm = new ConnectionManager();

		boolean a = false;
		boolean b = false;

		if (A) {
			a = true;
		}

		try {
			cm.publish(b);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (a) {
			System.out.println("");
		}
	}

	public void nestedIfTest() {
		int i = constantReturnInIf();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i);
	}

	private int return42() {
		return 42;
	}

	private int constantReturnInIf() {
		int tainted = TelephonyManager.getIMEI();
		int notTainted = return42();
		int x = 0;
		if (tainted > 42) {
			if (notTainted == 42) {
				return42();
				return 42;
			}
		}
		return 0;
	}
}
