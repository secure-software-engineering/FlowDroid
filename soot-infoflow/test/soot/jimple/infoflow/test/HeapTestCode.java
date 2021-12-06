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

import java.util.ArrayList;
import java.util.Random;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.ClassWithField;
import soot.jimple.infoflow.test.utilclasses.ClassWithStatic;

public class HeapTestCode {

	public class Y {
		String f;

		public void set(String s) {
			f = s;
		}
	}

	public void simpleTest() {
		String taint = TelephonyManager.getDeviceId();
		Y a = new Y();
		Y b = new Y();

		a.set(taint);
		b.set("notaint");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.f);
	}

	public void argumentTest() {
		ClassWithField x = new ClassWithField();
		run(x);
		x.listField = new ArrayList<String>();
		x.listField.add(TelephonyManager.getDeviceId());
	}

	public static void run(ClassWithField o) {
		o = new ClassWithField();
		o.listField = new ArrayList<String>();
		o.listField.add("empty");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(o.field);
	}

	public void negativeTest() {
		String taint = TelephonyManager.getDeviceId();

		MyArrayList notRelevant = new MyArrayList();
		MyArrayList list = new MyArrayList();
		notRelevant.add(taint);
		list.add("test");

		ConnectionManager cm = new ConnectionManager();
		cm.publish(list.get());
	}

	class MyArrayList {

		String[] elements;

		public void add(String obj) {
			if (elements == null) {
				elements = new String[3];
			}
			elements[0] = obj;
		}

		public String get() {
			return elements[0];
		}

	}

	public void doubleCallTest() {
		X a = new X();
		X b = new X();
		a.save("neutral");
		b.save(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish(String.valueOf(a.e));
	}

	public void methodTest0() {
		String taint = TelephonyManager.getDeviceId();
		X x = new X();
		A a = new A();
		String str = x.xx(a);
		a.b = taint;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(str);
	}

	public void methodTest0b() {
		String taint = TelephonyManager.getDeviceId();
		A a = new A();
		String str = a.b;
		a.b = taint;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(str);
	}

	class A {
		public String b = "Y";
		public String c = "X";
		public int i = 0;
	}

	class X {
		public char[] e;

		public String xx(A o) {
			return o.b;
		}

		public void save(String f) {
			e = f.toCharArray();
		}
	}

	// ########################################################################

	public void methodTest1() {
		String tainted = TelephonyManager.getDeviceId();
		new AsyncTask().execute(tainted);
	}

	protected class AsyncTask {
		public Worker mWorker;
		public FutureTask mFuture;

		public AsyncTask() {
			mWorker = new Worker() {
				public void call() {
					ConnectionManager cm = new ConnectionManager();
					cm.publish(mParams);
				}
			};
			mFuture = new FutureTask(mWorker);
		}

		public void execute(String t) {
			mWorker.mParams = t;
			// shortcut (no executor used):
			// exec.execute(mFuture);
			mFuture.run();
		}

	}

	protected class Worker {
		public String mParams;

		public void call() {
		}
	}

	protected class FutureTask {
		private final Worker wo;

		public FutureTask(Worker w) {
			wo = w;
		}

		public void run() {
			wo.call();
		}
	}

	public void testForWrapper() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish("");
		ClassWithStatic cws = new ClassWithStatic();
		int i = 4 + 3;
		while (true) {
			cws.getTitle();
			if (i == 8) {
				break;
			}
		}
		ClassWithStatic.staticString = TelephonyManager.getDeviceId();
	}

	public void testForLoop() {
		while (true) {
			WrapperClass f = new WrapperClass();
			f.sink();

			WrapperClass w = new WrapperClass();
			w.callIt();
		}
	}

	public void testForEarlyTermination() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(ClassWithStatic.staticString);

		@SuppressWarnings("unused")
		ClassWithStatic c1 = new ClassWithStatic();

		WrapperClass w1 = new WrapperClass();
		w1.callIt();
	}

	class WrapperClass {

		public void callIt() {
			ClassWithStatic.staticString = TelephonyManager.getDeviceId();
		}

		public void sink() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ClassWithStatic.staticString);
		}

	}

	// ----------------- backward flow on return:

	public void methodReturn() {
		B b = new B();
		B b2 = b;
		b.attr = m();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b2.attr.b);
	}

	public class B {
		public A attr;

		public B() {
			attr = new A();
		}

		public void setAttr(A attr) {
			this.attr = attr;
		}
	}

	public A m() {
		A a = new A();
		a.b = TelephonyManager.getDeviceId();
		return a;
	}

	public void twoLevelTest() {
		SecondLevel l2 = new SecondLevel();
		FirstLevel l1 = new FirstLevel();

		String x = l1.getValue(l2, TelephonyManager.getDeviceId());
		String y = l1.getValue(l2, "test");
		x.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y);
	}

	public class FirstLevel {

		public String getValue(SecondLevel l, String c) {
			return l.id(c);
		}
	}

	public class SecondLevel {

		public String id(String i) {
			return i;
		}

	}

	private class DataClass {
		public String data;
		public DataClass next;
	}

	public void multiAliasTest() {
		DataClass dc = new DataClass();
		DataClass dc2 = null;
		DataClass dc3 = new DataClass();

		dc2 = dc3;

		dc2.next = dc;

		String a = TelephonyManager.getDeviceId();
		dc.data = a;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(dc3.next.data);
	}

	public void overwriteAliasTest() {
		DataClass dc = new DataClass();
		DataClass dc2 = null;
		DataClass dc3 = new DataClass();

		dc2 = dc3;

		dc2.next = dc;
		dc3.next = null;

		String a = TelephonyManager.getDeviceId();
		dc.data = a;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(dc3.next.data);
	}

	public void arrayAliasTest() {
		String[] a = new String[1];
		String[] b = a;
		a[0] = TelephonyManager.getDeviceId();
		String[] c = b;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c[0]);
	}

	public void arrayAliasTest2() {
		String tainted = TelephonyManager.getDeviceId();
		String[] arr = new String[] { "foo", "bar" };
		String[] arr2 = arr;
		int size = arr.length;
		arr[1] = tainted;
		String x = arr2[1];

		ConnectionManager cm = new ConnectionManager();
		cm.publish(x);
		System.out.println(size);
	}

	public void functionAliasTest() {
		String tainted = TelephonyManager.getDeviceId();
		DataClass dc1 = new DataClass();
		DataClass dc2 = new DataClass();
		dc1.data = tainted;
		copy(dc1, dc2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(dc2.data);
	}

	private void copy(DataClass dc1, DataClass dc2) {
		dc2.data = dc1.data;
	}

	public void functionAliasTest2() {
		DataClass dc1 = new DataClass();
		DataClass dc2 = new DataClass();
		taintMe(dc1);
		copy(dc1, dc2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(dc2.data);
	}

	public void taintMe(DataClass dc) {
		String tainted = TelephonyManager.getDeviceId();
		dc.data = tainted;
	}

	public void multiLevelTaint() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		A a = b.attr;
		taintLevel1(tainted, b);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.b);
	}

	private void taintLevel1(String data, B b) {
		taintLevel2(data, b.attr);
	}

	private void taintLevel2(String data, A a) {
		a.b = data;
	}

	public void negativeMultiLevelTaint() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		A a = b.attr;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.b);
		taintLevel1(tainted, b);
	}

	public void negativeMultiLevelTaint2() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		taintLevel1b(tainted, b);
	}

	private void taintLevel1b(String data, B b) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
		taintLevel2(data, b.attr);
	}

	public void multiLevelTaint2() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		taintLevel1c(tainted, b);
	}

	private void taintLevel1c(String data, B b) {
		taintLevel2(data, b.attr);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	public void threeLevelTest() {
		B b = new B();
		A a = b.attr;
		taintOnNextLevel(b, a);
	}

	private void taintMe(B b) {
		b.attr.b = TelephonyManager.getDeviceId();
	}

	private void taintOnNextLevel(B b, A a) {
		taintMe(b);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.b);
	}

	private class RecursiveDataClass {
		RecursiveDataClass child;
		String data;

		public void leakIt() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data);
			if (child != null)
				child.leakIt();
		}
	}

	public void recursionTest() {
		RecursiveDataClass rdc = new RecursiveDataClass();
		rdc.data = TelephonyManager.getDeviceId();
		rdc.leakIt();
	}

	public void activationUnitTest1() {
		B b = new B();

		A a = b.attr;
		String tainted = TelephonyManager.getDeviceId();
		a.b = tainted;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	public void activationUnitTest2() {
		B b = new B();
		b.attr = new A();

		A a = b.attr;
		String tainted = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);

		a.b = tainted;
	}

	public void activationUnitTest3() {
		B b = new B();
		b.attr = new A();

		B b2 = new B();
		b2.attr = new A();

		String tainted = TelephonyManager.getDeviceId();

		b.attr.b = tainted;
		b2.attr.b = b.attr.b;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b2.attr.b);
	}

	public void activationUnitTest4() {
		B b = new B();
		b.attr = new A();

		B b2 = new B();
		b2.attr = new A();

		String tainted = TelephonyManager.getDeviceId();

		b2.attr.b = tainted;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);

		b.attr.b = tainted;
	}

	public void activationUnitTest4b() {
		B b = new B();
		b.attr = new A();

		B b2 = new B();
		b2.attr = new A();

		String tainted = TelephonyManager.getDeviceId();

		b2.attr.b = tainted;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);

		b.attr = b2.attr;
	}

	public void activationUnitTest5() {
		B b = new B();
		b.attr = new A();

		B b2 = new B();
		b2.attr = new A();

		ConnectionManager cm = new ConnectionManager();
		String tainted = TelephonyManager.getDeviceId();

		cm.publish(b.attr.b);
		cm.publish(b2.attr.b);

		b.attr = b2.attr;

		cm.publish(b.attr.b);
		cm.publish(b2.attr.b);

		b.attr.b = tainted;
	}

	public void returnAliasTest() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B();
		B c = b;
		A a = alias(c);
		c.attr.b = tainted;
		b.attr.b = tainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.b);
	}

	private A alias(B b) {
		return b.attr;
	}

	public void callPerformanceTest() {
		A a = new A();
		a.b = getDeviceId();
		B b = new B();
		b.attr = a;

		doIt(b);
	}

	private void doIt(B b) {
		throwAround(b);
		System.out.println(b.attr.b);
	}

	private String getDeviceId() {
		String tainted = TelephonyManager.getDeviceId();
		return tainted;
	}

	private void throwAround(B b) {
		throwAround2(b.attr);
	}

	private void throwAround2(A a) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.b);
	}

	private B b1;
	private B b2;

	private void foo(B b1, B b2) {
		this.b1 = b1;
		this.b2 = b2;
	}

	@SuppressWarnings("unused")
	private void foo2(B b1, B b2) {
		//
	}

	private A bar(A a) {
		this.b1.attr = a;
		return this.b2.attr;
	}

	@SuppressWarnings("unused")
	private A bar2(A a) {
		return null;
	}

	public void testAliases() {
		B b = new B();
		A a = new A();
		a.b = TelephonyManager.getDeviceId();

		// Create the alias
		foo(b, b);
		String tainted = bar(a).b;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}

	public void testWrapperAliases() {
		B b = new B();
		A a = new A();
		a.b = TelephonyManager.getDeviceId();

		// Create the alias
		foo2(b, b);
		String tainted = bar2(a).b;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}

	public void negativeTestAliases() {
		B b = new B();
		A a = new A();
		a.b = TelephonyManager.getDeviceId();

		// Create the alias
		foo(b, b);
		String untainted = bar(a).c;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	private void alias(B b1, B b2) {
		b2.attr = b1.attr;
	}

	private void set(B a, String secret, B b) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
		a.attr.b = secret;
		cm.publish(b.attr.b);
	}

	private void foo(B a) {
		System.out.println(a);
	}

	public void aliasPerformanceTest() {
		B a = new B();
		B b = new B();
		alias(a, b);
		set(a, TelephonyManager.getDeviceId(), b);
		foo(a);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	public void backwardsParameterTest() {
		B b1 = new B();
		b1.attr = new A();
		B b2 = new B();

		alias(b1, b2);

		b2.attr.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b1.attr.b);
	}

	public void aliasTaintLeakTaintTest() {
		B b = new B();
		b.attr = new A();
		A a = b.attr;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
		b.attr.b = TelephonyManager.getDeviceId();
		cm.publish(a.b);
	}

	public void fieldBaseOverwriteTest() {
		A a = new A();
		a.b = TelephonyManager.getDeviceId();
		A a2 = a;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a2.b);
	}

	private A alias(A a) {
		return a;
	}

	public void doubleAliasTest() {
		A a = new A();
		A b = alias(a);
		A c = alias(a);
		a.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
		cm.publish(c.b);
	}

	private A alias2(A a) {
		A a2 = a;
		return a2;
	}

	public void doubleAliasTest2() {
		A a = new A();
		A b = alias2(a);
		A c = alias2(a);
		a.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
		cm.publish(c.b);
	}

	public void tripleAliasTest() {
		A a = new A();
		A b = alias(a);
		A c = alias(a);
		A d = alias(a);
		a.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
		cm.publish(c.b);
		cm.publish(d.b);
	}

	private int intData;

	private void setIntData() {
		this.intData = TelephonyManager.getIMEI();
	}

	public void intAliasTest() {
		setIntData();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(intData);
	}

	private static B staticB1;
	private static B staticB2;

	private void aliasStatic() {
		staticB2.attr = staticB1.attr;
	}

	public void staticAliasTest() {
		staticB1 = new B();
		staticB2 = new B();

		aliasStatic();

		staticB1.attr.b = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticB2.attr.b);
	}

	public void staticAliasTest2() {
		staticB1 = new B();
		staticB2 = staticB1;
		staticB1.attr.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticB2.attr.b);
	}

	public void unAliasParameterTest() {
		B b1 = new B();
		B b2 = new B();

		b2.attr = b1.attr;
		doUnalias(b2);
		b1.attr.b = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b2.attr.b);
	}

	private void doUnalias(B b2) {
		b2.attr = new A();
	}

	public void overwriteParameterTest() {
		B b = new B();
		b.attr.b = TelephonyManager.getDeviceId();

		overwriteParameter(b);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	private void overwriteParameter(B b) {
		System.out.println(b);
		b = new B();
	}

	public void multiAliasBaseTest() {
		A a = new A();
		B b1 = new B();
		B b2 = new B();

		b1.setAttr(a);
		b2.setAttr(a);

		a.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b1.attr.b);
		cm.publish(b2.attr.b);
	}

	private class Inner1 {

		private class Inner2 {
			private String data;

			public void set() {
				data = TelephonyManager.getDeviceId();
			}
		}

		private Inner2 obj;

		public String get() {
			return obj.data;
		}
	}

	public void innerClassTest() {
		Inner1 a = new Inner1();
		Inner1 b = new Inner1();

		a.obj = a.new Inner2();
		b.obj = a.new Inner2();

		a.obj.set();
		String untainted = b.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	private class Inner1b {

		private class Inner2b {
			public String data;

			public void set() {
				obj.data = TelephonyManager.getDeviceId();
			}

			public String get() {
				return obj.data;
			}

			public String getParent() {
				return parentData;
			}
		}

		public Inner2b obj;
		public String parentData;

		public String get() {
			return obj.data;
		}
	}

	public void innerClassTest2() {
		Inner1b a = new Inner1b();
		Inner1b b = new Inner1b();

		a.obj = a.new Inner2b();
		b.obj = a.new Inner2b();

		a.obj.set();
		String untainted = b.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	public void innerClassTest3() {
		Inner1b a = new Inner1b();
		Inner1b b = new Inner1b();

		a.obj = a.new Inner2b();
		b.obj = a.new Inner2b();

		b.obj.set();
		String untainted = a.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	public void innerClassTest4() {
		Inner1b a = new Inner1b();
		Inner1b b = new Inner1b();

		a.obj = a.new Inner2b();
		b.obj = b.new Inner2b();

		a.obj.set();
		String untainted = b.obj.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	private class Inner3 {

		private class Inner2b {

			private Inner2 foo;

		}

		private String data;

		private class Inner2 {

			public void set() {
				data = TelephonyManager.getDeviceId();
			}
		}

		private Inner2b obj2;

		public String get() {
			return data;
		}
	}

	public void innerClassTest5() {
		Inner3 a = new Inner3();
		Inner3 b = new Inner3();

		a.obj2 = b.new Inner2b();
		a.obj2.foo = b.new Inner2();

		a.obj2.foo.set();
		String untainted = a.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untainted);
	}

	public void innerClassTest6() {
		Inner1b a = new Inner1b();
		a.obj = a.new Inner2b();
		a.parentData = TelephonyManager.getDeviceId();

		Inner1b.Inner2b inner = a.obj;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(inner.getParent());
	}

	private class SimpleTree {
		private String data = "";
		private SimpleTree left;
		private SimpleTree right;
	}

	public void datastructureTest() {
		SimpleTree root = new SimpleTree();
		root.left = new SimpleTree();
		root.right = new SimpleTree();
		root.left.data = TelephonyManager.getDeviceId();
		root.right.data = "foo";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(root.left.data);
	}

	public void datastructureTest2() {
		SimpleTree root = new SimpleTree();
		root.left = new SimpleTree();
		root.right = new SimpleTree();
		root.left.data = TelephonyManager.getDeviceId();
		root.right.data = "foo";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(root.right.data);
	}

	private class Tree {
		private Tree left;
		private Tree right;
		private String data;
	}

	private static Tree myTree;

	public void staticAccessPathTest() {
		myTree = new Tree();
		myTree.left = new Tree();
		myTree.left.right = new Tree();
		myTree.left.right.left = myTree;
		myTree.data = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(myTree.left.right.left.data);
	}

	private class SeparatedTree {
		private TreeElement left;
		private TreeElement right;
	}

	private class TreeElement {
		private SeparatedTree child;
		private String data;
	}

	public void separatedTreeTest() {
		SeparatedTree myTree = new SeparatedTree();
		myTree.left = new TreeElement();
		myTree.left.child = new SeparatedTree();
		myTree.left.child.right = new TreeElement();
		myTree.left.child.right.data = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(myTree.left.child.right.data);
	}

	public void overwriteAliasedVariableTest() {
		Y y1 = new Y();
		Y y2 = y1;
		y1.f = TelephonyManager.getDeviceId();
		y2 = new Y();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y1.f);
		cm.publish(y2.f);
	}

	public void overwriteAliasedVariableTest2() {
		Y y1 = new Y();
		Y y2 = y1;
		y1.f = TelephonyManager.getDeviceId();
		y2.f = "";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y1.f);
		cm.publish(y2.f);
	}

	public void overwriteAliasedVariableTest3() {
		Y y1 = new Y();
		Y y2 = y1;
		y1.f = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y1.f);
		y2.f = "";
		cm.publish(y2.f);
	}

	public void overwriteAliasedVariableTest4() {
		Y y1 = new Y();
		Y y2 = y1;
		y1.f = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y1.f);
		cm.publish(y2.f);
		y2.f = "";
	}

	public void overwriteAliasedVariableTest5() {
		ConnectionManager cm = new ConnectionManager();
		Object x = TelephonyManager.getDeviceId();
		Object y = new AccountManager().getPassword();

		String z = "";

		z = (String) x;
		String z2 = z;

		z = (String) y;
		String z3 = z;

		cm.publish(z2);
		cm.publish(z3);
	}

	@SuppressWarnings("null") // would cause an NPE
	public void overwriteAliasedVariableTest6() {
		Y y1 = new Y();
		y1.f = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y1.f);
		y1 = null;
		cm.publish(y1.f);
	}

	public void aliasFlowTest() {
		A b, q, y;
		B a, p, x;

		a = new B();
		p = new B();

		b = new A();
		q = new A();

		if (Math.random() < 0.5) {
			x = a;
			y = b;
		} else {
			x = p;
			y = q;
		}
		x.attr = y;
		q.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.attr.b);
	}

	private class Data {
		public Data next;
	}

	private Data getSecretData() {
		return new Data();
	}

	private void leakData(Data e) {
		System.out.println(e);
	}

	public void aliasStrongUpdateTest() {
		Data d = getSecretData();
		d = d.next;
		Data e = d;

		Data x = new Data();
		x.next = e;
		Data y = x;
		e = y.next;
		e = e.next;
		leakData(e);
	}

	public void aliasStrongUpdateTest2() {
		Data d = getSecretData();
		d = d.next;
		Data e = d;

		Data x = new Data();
		Data y = x;
		x.next = e;
		e = y.next;
		e = e.next;
		leakData(e);
	}

	private Data taintedBySourceSinkManager = null;

	public void aliasStrongUpdateTest3() {
		Data d = taintedBySourceSinkManager;
		d = d.next;
		Data e = d;

		Data x = new Data();
		Data y = x;
		x.next = e;
		e = y.next;
		e = e.next;
		leakData(y.next);
	}

	public void arrayLengthAliasTest1() {
		String tainted = TelephonyManager.getDeviceId();
		String[] arr = new String[] { "foo", "xx", "bar" };
		int size = arr.length;
		arr[1] = tainted;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(size);
	}

	public void arrayLengthAliasTest2() {
		String tainted = TelephonyManager.getDeviceId();
		String[] arr = new String[] { "foo", "xx", "bar" };
		String[] arr2 = arr;
		int size = arr.length;
		arr[1] = tainted;
		int size2 = arr2.length;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(size2);
		System.out.println(size);
	}

	public void arrayLengthAliasTest3() {
		String tainted = TelephonyManager.getDeviceId();
		String[] arr = new String[tainted.length()];
		int size = arr.length;
		arr[1] = tainted;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(size);
	}

	public void arrayLengthAliasTest4() {
		String tainted = TelephonyManager.getDeviceId();
		String[] arr = new String[tainted.length()];
		String[] arr2 = arr;
		int size = arr.length;
		arr[1] = tainted;
		int size2 = arr2.length;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(size2);
		System.out.println(size);
	}

	public void taintPrimitiveFieldTest1() {
		A a = new A();
		A b = a;
		a.i = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.i);
	}

	public void taintPrimitiveFieldTest2() {
		B b = new B();
		A a = new A();
		b.attr = a;
		a.i = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.i);
	}

	public void multiContextTest1() {
		A a = new A();
		a.b = TelephonyManager.getDeviceId();
		a.c = TelephonyManager.getDeviceId();
		String data = id(a.b);
		String data2 = id(a.c);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
		cm.publish(data2);
	}

	private String id(String val) {
		return val;
	}

	public void recursiveFollowReturnsPastSeedsTest1() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(doTaintRecursively(new A()));
	}

	private String doTaintRecursively(A a) {
		if (new Random().nextBoolean()) {
			a.b = TelephonyManager.getDeviceId();
			return "";
		} else {
			A a2 = new A();
			doTaintRecursively(a2);
			return a2.b;
		}
	}

	public void doubleAliasTest1() {
		A a1 = new A();
		a1.b = TelephonyManager.getDeviceId();
		A a2 = new A();
		a2.b = new AccountManager().getPassword();

		B b = new B();
		if (new Random().nextBoolean())
			b.attr = a1;
		else
			b.attr = a2;

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	private class C {
		private B b;
	}

	public void longAPAliasTest1() {
		C c = new C();
		c.b = new B();
		c.b.attr = new A();

		A a = c.b.attr;
		a.b = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.b.attr.b);
	}

	public void simpleFieldTest1() {
		A a = new A();
		B b = new B();
		b.attr = a;
		a.b = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	public void contextTest1() {
		A a = new A();
		A b = new A();
		String data = TelephonyManager.getDeviceId();
		copy(a, data);
		copy(b, "Hello World");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
	}

	private void copy(A b, String string) {
		A c = b;
		c.b = string;
	}

	public void contextTest2() {
		String data = TelephonyManager.getDeviceId();
		A a = copy(data);
		A b = copy("Hello World");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
		System.out.println(a);
	}

	private A copy(String data) {
		A a = new A();
		A b = a;
		b.b = data;
		return a;
	}

	public void contextTest3() {
		String data = TelephonyManager.getDeviceId();
		A a = copy(data, new AccountManager().getPassword());
		A b = copy("Hello World", "Foobar");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.b);
		System.out.println(a);
	}

	private A copy(String context, String data) {
		System.out.println(context);
		A a = new A();
		A b = a;
		b.b = data;
		return a;
	}

	public static class Container1 {
		String g;
	}

	public static class Container2 {
		Container1 f;
	}

	private void doWrite(final Container2 base, final String string) {
		base.f.g = string;
	}

	public void summaryTest1() {
		final Container2 base1 = new Container2();
		final Container2 base2 = new Container2();
		final String tainted = TelephonyManager.getDeviceId();
		doWrite(base1, tainted);

		final Container1 z = base2.f;
		doWrite(base2, tainted);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(z.g);
	}

	public void delayedReturnTest1() {
		A a = new A();
		B b = new B();
		doAlias(b, a);

		a.b = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.attr.b);
	}

	private void doAlias(B b, A a) {
		b.attr = a;
	}

	class D {
		E e;

		public void read() {
			e = new E();
			e.read();
		}
	}

	class E {
		String str;

		public void read() {
			str = "";
			str = TelephonyManager.getDeviceId();
		}
	}

	public void aliasWithOverwriteTest1() {
		D d = new D();
		d.read();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(d.e.str);
	}

	class F {
		G g;

		public void read() {
			g = new G();
			g.read();
		}
	}

	class G {
		String str;

		public void read() {
			str = TelephonyManager.getDeviceId();
		}
	}

	public void aliasWithOverwriteTest2() {
		F f = new F();
		f.read();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(f.g.str);
	}

	class H {
		I i;

		public void read() {
			i = new I();
			i.read();
		}
	}

	class I {
		String str;
		String str2;

		public void read() {
			str = "";
			str2 = str;
			str = TelephonyManager.getDeviceId();
		}
	}

	public void aliasWithOverwriteTest3() {
		H h = new H();
		h.read();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(h.i.str2);
	}

	class J {
		E e;
		E f;

		public void read() {
			e = new E();
			f = e;
			e.read();
		}
	}

	public void aliasWithOverwriteTest4() {
		J j = new J();
		j.read();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(j.f.str);
	}

}
