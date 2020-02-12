/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/

package soot.jimple.infoflow.test;

import java.io.Serializable;

import soot.jimple.infoflow.test.android.Bundle;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.LocationManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Test code for the type checker
 * 
 * @author Steven Arzt
 */
public class TypeTestCode {

	public void typeTest1() {
		String tainted = TelephonyManager.getDeviceId();

		Object obj = (Object) tainted;
		String newStr = obj.toString();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(newStr);
	}

	private class A {
		String data;
		String data2;

		public A() {

		}

		@SuppressWarnings("unused")
		public A(String data) {
			this.data = data;
		}

		String bar() {
			return this.data;
		}

		void leak() {
			ConnectionManager cm = new ConnectionManager();
			// cm.publish("A: " + data);
			cm.publish(data);
		}

		@Override
		public String toString() {
			return "data: " + data + ", data2: " + data2;
		}
	}

	private class B extends A {
		String foo() {
			return this.data;
		}

		@Override
		void leak() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish("B: " + data);
		}
	}

	private class B2 extends A {
		@Override
		void leak() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish("B2: " + data);
		}
	}

	private class C {
		String data;
	}

	public void classCastTest1() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		b.data = tainted;

		A a = (A) b;
		String newStr = a.bar();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(newStr);
	}

	public void classCastTest2() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		b.data = tainted;

		A a = (A) b;
		B b2 = (B) a;
		String newStr = b2.foo();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(newStr);
	}

	public void classCastTest3() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		b.data = tainted;

		A a = (A) b;
		B b2 = (B) a;
		String newStr = b2.bar();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(newStr);
	}

	private static A a;

	public void classCastTest4() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		b.data = tainted;
		a = b;

		B b2 = (B) a;
		String newStr = b2.bar();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(newStr);
	}

	public void instanceofTest1() {
		String tainted = TelephonyManager.getDeviceId();

		A a;
		if (tainted.startsWith("x"))
			a = new A();
		else
			a = new B();
		a.data = tainted;

		ConnectionManager cm = new ConnectionManager();
		if (a instanceof A)
			cm.publish(a.bar());
		else if (a instanceof B)
			cm.publish(((B) a).foo());
		else {
			Object o = (Object) a;
			C c = (C) o;
			cm.publish(c.data);
		}
	}

	public void instanceofTest2() {
		String tainted = TelephonyManager.getDeviceId();
		boolean isString = tainted instanceof String;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(isString);
	}

	private void callIt(A a) {
		a.leak();
	}

	public void callTargetTest1() {
		A b2 = new B2();
		callIt(b2);
		b2.data = TelephonyManager.getDeviceId();

		A b = new B();
		b.data = TelephonyManager.getDeviceId();
		callIt(b);
	}

	public void arrayObjectCastTest() {
		Object obj = Bundle.get("foo");
		A foo2[] = (A[]) obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo2[0].data);
	}

	public void arrayObjectCastTest2() {
		Object obj = Bundle.get("foo");
		A foo2[] = (A[]) obj;
		obj = foo2[0];
		A a = (A) obj;
		a.data2 = a.data;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.data);
	}

	public void arrayObjectCastTest3() {
		Object obj = Bundle.get("foo");
		objArr = (Object[]) obj;
		doMagic(objArr);
	}

	private Object[] objArr;

	private void doMagic(Object obj) {
		if (obj instanceof Object[]) {
			Object[] arr = (Object[]) obj;
			int i = arr.length;
			System.out.println("Length: " + i);
			obj = arr[0];
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(obj == null ? "null" : (String) obj);
	}

	public void callTypeTest() {
		String[] x = new String[1];
		x[0] = TelephonyManager.getDeviceId();
		objArgFunction(x);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x[0]);
	}

	private void objArgFunction(Object[] x) {
		System.out.println(x);
	}

	public void callTypeTest2() {
		String[] x = new String[1];
		objArgFunction2(x);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x[0]);
	}

	private void objArgFunction2(Object[] x) {
		x[0] = TelephonyManager.getDeviceId();
	}

	public void arrayCastAndAliasTest() {
		String[] x = new String[1];
		Object y = x;
		x[0] = TelephonyManager.getDeviceId();
		Object obj = y;
		String[] out = (String[]) obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(out[0]);
	}

	public void arrayCastAndAliasTest2() {
		String[] x = new String[1];
		Object e = x;
		Object a = (Object) e;
		Object z = a;
		Object y = z;
		x[0] = TelephonyManager.getDeviceId();
		Object obj = y;
		String[] out = (String[]) obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(out[0]);
	}

	public void arrayIncompatibleCastAndAliasTest() {
		String[] x = new String[1];
		Object e = x;
		String a = (String) e;
		Object a2 = a;
		String[] z = (String[]) a2;
		Object y = z;
		z[0] = TelephonyManager.getDeviceId();
		Object obj = y;
		String[] out = (String[]) obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(out[0]);
	}

	private static String[] aci_x;
	private static Object aci_e;
	private static String aci_a;
	private static Object aci_a2;
	private static String[] aci_z;
	private static Object aci_y;
	private static Object aci_obj;
	private static String[] aci_out;

	public void arrayIncompatibleCastAndAliasTest2() {
		aci_x = new String[1];
		aci_e = aci_x;
		aci_a = (String) aci_e;
		aci_a2 = aci_a;
		aci_z = (String[]) aci_a2;
		aci_y = aci_z;
		aci_z[0] = TelephonyManager.getDeviceId();
		aci_obj = aci_y;
		aci_out = (String[]) aci_obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(aci_out[0]);
	}

	private class X {
		private B b;
		private Object o;
		private Object[] arr;

		public X() {
			this.b = new B();
		}
	}

	public void fieldIncompatibleCastAndAliasTest() {
		X x = new X();
		x.b.data = TelephonyManager.getDeviceId();
		Object e = x;
		String a = (String) e;
		Object z = a;
		Object y = z;
		X x2 = (X) y;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x2.b.data);
	}

	public void twoDimensionArrayTest() {
		String[] x = new String[1];
		Object y = x;
		x[0] = TelephonyManager.getDeviceId();
		Object[] foo = new Object[1];
		foo[0] = y;
		Object bar = foo;
		String[][] out = (String[][]) bar;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(out[0][0]);
	}

	public void arrayBackPropTypeTest() {
		Object[] oarr = new Object[2];
		Object odata = new String[] { TelephonyManager.getDeviceId() };
		oarr[0] = odata;
		oarr[1] = TelephonyManager.getDeviceId();
		Object o = oarr[1];
		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) o);
	}

	public void arrayBackPropTypeTest2() {
		Object[] oarr = new Object[2];
		Object odata = new String[] { TelephonyManager.getDeviceId() };
		Object foo = odata;
		oarr[0] = odata;
		oarr[1] = TelephonyManager.getDeviceId();
		String[] o = (String[]) foo;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(o[1]);
	}

	public void arrayBackPropTypeTest3() {
		Object[] oarr = new Object[2];
		Object foo = oarr;
		Object odata = new String[] { TelephonyManager.getDeviceId() };
		oarr[0] = odata;
		oarr[1] = TelephonyManager.getDeviceId();
		String[] o = (String[]) foo;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(o[1]);
	}

	public void arrayBackPropTypeTest4() {
		Object[] oarr = new Object[2];
		Object foo = oarr;
		Object odata = new String[] { TelephonyManager.getDeviceId() };
		String[] sa = (String[]) oarr[1];
		String s = (String) oarr[1];
		System.out.println(s + sa);
		oarr[0] = odata;
		oarr[1] = TelephonyManager.getDeviceId();
		String[] o = (String[]) foo;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(o[1]);
	}

	public void arrayBackPropTypeTest5() {
		Object[] oarr = new Object[2];
		Object foo = oarr;
		Object odata = new String[] { TelephonyManager.getDeviceId() };
		String[] sa = (String[]) oarr[1];
		String s = (String) oarr[1];
		System.out.println(s + sa);
		oarr[1] = TelephonyManager.getDeviceId(); // different ordering of array
													// writes
		oarr[0] = odata;
		String[] o = (String[]) foo;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(o[1]);
	}

	public void objectTypeBackPropTest() {
		X b = new X();
		X c = b;
		b.o = TelephonyManager.getDeviceId();
		b.o = new String[] { TelephonyManager.getDeviceId() };
		ConnectionManager cm = new ConnectionManager();
		cm.publish(((String[]) c.o)[1]);
	}

	public void objectArrayBackPropTest() {
		X b = new X();
		X c = b;
		b.arr = new Object[] { TelephonyManager.getDeviceId(), "foo" };
		b.arr[0] = TelephonyManager.getDeviceId();
		b.arr[1] = new String[] { TelephonyManager.getDeviceId() };
		ConnectionManager cm = new ConnectionManager();
		cm.publish(((String[]) c.arr)[0]);
	}

	public void aliasTypeTest() {
		X b = new X();
		b.arr = new Object[2];
		X c = new X();

		doAlias(b, c);
		b.arr[0] = TelephonyManager.getDeviceId();

		X d = new X();
		doAlias(c, d);
		b.arr[1] = new String[] { TelephonyManager.getDeviceId() };

		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) d.arr[0]);
	}

	public void aliasTypeTest2() {
		X b = new X();
		b.arr = new Object[2];
		X c = new X();

		doAlias(b, c);
		b.arr[0] = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) c.arr[0]);
	}

	public void aliasTypeTest3() {
		X b = new X();
		b.arr = new Object[2];
		X c = new X();

		doAlias(b, c);
		b.arr[0] = TelephonyManager.getDeviceId();

		X d = new X();
		d.arr = c.arr;

		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) d.arr[0]);
	}

	private void doAlias(X b, X c) {
		c.arr = b.arr;
	}

	public void aliasReturnTest() {
		X b = new X();
		b.arr = new Object[2];
		Object[] x = b.arr;

		Object[] c = id(x);
		c[0] = TelephonyManager.getDeviceId();

		Object[] d = id(x);
		b.arr[1] = new String[] { TelephonyManager.getDeviceId() };

		ConnectionManager cm = new ConnectionManager();
		cm.publish(((String[]) d)[0]);
	}

	private <T> T id(T x) {
		return x;
	}

	public void arrayLengthObjectTest() {
		Serializable s = new Integer(42);
		Object[] oArr = new Object[3];
		int[] iArr = new int[3];
		iArr[0] = 42;
		int len = iArr.length;
		oArr[1] = iArr;
		oArr[2] = s;
		iArr[1] = TelephonyManager.getIMEI();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(len);
	}

	public void doubleBoxingTest1() {
		double longitude = LocationManager.getLongitude();
		Double dblLong = longitude;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(dblLong);
	}

	public void doubleBoxingTest2() {
		double longitude = LocationManager.getLongitude();
		Double dblLong = (Double) longitude;

		ConnectionManager cm = new ConnectionManager();
		cm.publish((Double) dblLong);
	}

	public void doubleToIntTest1() {
		double longitude = LocationManager.getLongitude();
		ConnectionManager cm = new ConnectionManager();
		cm.publish((int) longitude);
	}

	private abstract class D extends A {

		public void doTaint() {
			transform(TelephonyManager.getDeviceId());
		}

		protected abstract void transform(String str);

	}

	private class E extends D {

		@Override
		protected void transform(String str) {
			this.data = str;
		}

	}

	private class F extends D {

		String str;

		@Override
		protected void transform(String str) {
			this.str = str;
		}

	}

	public void followReturnsPastSeedsTest1() {
		E e = new E();
		doTaintX(e);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(e.data);
		B b = new B();
		doTaintX(b);
		cm.publish(b.data);
	}

	private void doTaintX(A a) {
		((D) a).doTaint();
	}

	public void followReturnsPastSeedsTest2() {
		E e = new E();
		doTaintX(e);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(e.data);
		F f = new F();
		doTaintX(f);
		cm.publish(f.data);
		cm.publish(f.str);
	}

	public void multiDimensionalArrayTest1() {
		int[][] arr = new int[1][];
		int[] arr2 = new int[1];
		arr[0] = arr2;
		String id = TelephonyManager.getDeviceId();
		arr2[0] = id.charAt(0);
		int[] arr3 = arr[0];
		ConnectionManager cm = new ConnectionManager();
		cm.publish(arr3[0]);
	}

	public void multiDimensionalArrayTest2() {
		int[][] arr = new int[1][1];
		String id = TelephonyManager.getDeviceId();
		int[] arr2 = arr[0];
		arr2[0] = id.charAt(0);
		int[] arr3 = arr[0];
		ConnectionManager cm = new ConnectionManager();
		cm.publish(arr3[0]);
	}

	public void aliasPerformanceTest1() {
		String id = TelephonyManager.getDeviceId();

		B a = new B();

		X x2 = new X();
		X x = new X();
		foo(x);
		x = x2;
		a.data = id;
		x.b = a;
		x2.o = id;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(x2.b.data);
	}

	private void foo(X x) {
		System.out.println(x.b.data);
	}

}
