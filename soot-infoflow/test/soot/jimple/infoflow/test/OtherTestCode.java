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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.C1static;
import soot.jimple.infoflow.test.utilclasses.C2static;
import soot.jimple.infoflow.test.utilclasses.ClassWithField;
import soot.jimple.infoflow.test.utilclasses.ClassWithFinal;

public class OtherTestCode {

	
	public void testWithField(){
		ClassWithField fclass = new ClassWithField();
		fclass.field = TelephonyManager.getDeviceId();
			
		ConnectionManager cm = new ConnectionManager();
		cm.publish(fclass.field);
		cm.publish(fclass.field);
	}
	
	public void genericsfinalconstructorProblem(){
		String tainted = TelephonyManager.getDeviceId();
		ClassWithFinal<String> c0 = new ClassWithFinal<String>(tainted, false);
		String alsoTainted = c0.getString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(alsoTainted);	
	}

	private String deviceId = "";
	
	public interface MyInterface {
		void doSomething();
	}
	
	public void innerClassTest() {
		this.deviceId = TelephonyManager.getDeviceId();
		runIt(new MyInterface() {
			
			@Override
			public void doSomething() {
				ConnectionManager cm = new ConnectionManager();
				cm.publish(deviceId);
			}
			
		});
	}
	
	private void runIt(MyInterface intf) {
		intf.doSomething();
	}
	
	private String x(String y) {
		return y;
	}
	
	private String annotate(String data) {
		return x(data);
//		return "x" + data + "x";
	}

	public void multiCallTest() {
		ConnectionManager cm = new ConnectionManager();
		String deviceId = TelephonyManager.getDeviceId();
		
		String data = annotate(deviceId);
		cm.publish(data);
		
		String did = deviceId;
		String data2 = annotate(did);
		cm.publish(data2);
	}
	
	public void passOverTest() {
		ConnectionManager cm = new ConnectionManager();
		String deviceId = TelephonyManager.getDeviceId();

		C1 c = new C1(deviceId);
		
		annotate(c);
		cm.publish(c.field1);
	}
	
	private void annotate(C1 c) {
		System.out.println(c.field1);
		c = new C1("Hello World");
	}
	
	public void overwriteTest() {
		ConnectionManager cm = new ConnectionManager();
		String deviceId = TelephonyManager.getDeviceId();

		C1 c = new C1(deviceId);
		
		overwrite(c);
		cm.publish(c.field1);
	}

	private void overwrite(C1 c) {
		c.field1 = "Hello World";
	}

	public void loopTest() {
		String imei = TelephonyManager.getDeviceId();
		for (int i = 0; i < 10; i++) {
	        ConnectionManager cm = new ConnectionManager();
			cm.publish(imei);
		}
	}
	
	public void dataObjectTest(){
		String imei = TelephonyManager.getDeviceId();

		APIClass api = new APIClass();
		api.testMethod(imei);
        ConnectionManager cm = new ConnectionManager();
		cm.publish(api.getDi());
	}

	class APIClass{
		InternalData d = new InternalData();
		
		public void testMethod(String i){
			//d = i;
			d.setI(i);
		}
		
		String getDi(){
			return d.getI();
		}
	}
	
	class InternalData{
		
		public String getI() {
			return i;
		}

		public void setI(String i) {
			this.i = i;
		}

		String i = "";
	}

	
	//Tests from Mail:
	
	public void methodTainted() {
		O x = new O();
		x.field = TelephonyManager.getDeviceId();
		O y = new O();
		y.field = "not_tainted";

		String x2 = foo(x);
		String y2 = foo(y);
		y2.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x2);
	}
	public void methodNotTainted() {
		O x = new O();
		x.field = TelephonyManager.getDeviceId();
		O y = new O();
		y.field = "not_tainted";

		String x2 = foo(x);
		String y2 = foo(y);
		x2.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y2);
	}
	

	String foo(O obj) {
		return obj.field;
	}

	private class O {
		String field;

		public String get() {
			return field;
		}
		
		public void set(String data) {
			this.field = data;
		}
	}
	
	public void method2() {
		String tainted = TelephonyManager.getDeviceId();
		O a = new O();
		//Fehler wenn:
		O b = new O();
		
		foo(a, tainted);
		foo(b, "untainted");
		
		String taint = a.field;
		String untaint = b.field;
		untaint.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taint);
	}
	
	public void method2NotTainted() {
		String tainted = TelephonyManager.getDeviceId();
		O a = new O();
		//Fehler wenn:
		O b = new O();
		
		foo(a, tainted);
		foo(b, "untainted");
		
		String taint = a.field;
		String untaint = b.field;
		taint.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaint);
	}
	
	
	void foo(O obj, String s){
		obj.field = s;
	}
	
	
	
	public void method3(){
		String tainted = TelephonyManager.getDeviceId();
		String untainted = "hallo welt";
		List1 a = new List1();
		List1 b = new List1();
		a.add(tainted);
		b.add(untainted);
		
		String c = a.get();
		String d = b.get();
		d.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c);
	}
	
	public void method3NotTainted(){
		String tainted = TelephonyManager.getDeviceId();
		String untainted = "hallo welt";
		List1 a = new List1();
		List1 b = new List1();
		a.add(tainted);
		b.add(untainted);
		
		String c = a.get();
		String d = b.get();
		c.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(d);
	}
	
	
	
	
	
	private class List1 {
		private String field;
		
		public void add(String e){
			field = e;
		}
		
		public String get(){
			return field;
		}

	}
	
	//advanced:
	
	public void method4(){
		String tainted = TelephonyManager.getDeviceId();
		C2 c = new C2(tainted); 
		C2 d = c;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(d.cfield.field1);
	}
	
	public void method5(){
		String tainted = TelephonyManager.getDeviceId();
		
		C2 c = new C2("test");
		C1 changes = c.cfield;
		c.cfield.field1 = tainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(changes.field1);
		
		
	}
	
	private class C1{
		String field1;
		
		public C1(String c){
			field1 = c;
		}
	}
	
	private class C2{
		C1 cfield;
		
		public C2(String c){
			cfield = new C1(c);
		}
	}
	
	public void method6(){
		String tainted = TelephonyManager.getDeviceId();
		
		@SuppressWarnings("unused")
		C2static c = new C2static("test");
		C1static.field1 = tainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(C2static.cfield.getField());
		
	}
	
	public void testPointsToSet(){
		Testclass1 tc1 = new Testclass1();
		String tainted = TelephonyManager.getDeviceId();
		tc1.dummyMethod(tainted);
		String s1 = (String) tc1.getIt();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(s1);
	}
	
	private class Testclass1{
		private Object[] elementData;
		
		public Testclass1(){
			elementData = new Object[3];
		}
		
		public boolean dummyMethod(Object obj){
			elementData[0] = obj;
			return true;
		}
		
		public Object getIt(){
			return elementData[0];
		}
		
	}
	
	public void easyNegativeTest(){
		String tainted = TelephonyManager.getDeviceId();
		LinkedList<String> notRelevantList = new LinkedList<String>();
		LinkedList<String> list = new LinkedList<String>();
		list.add("neutral");
		notRelevantList.add(tainted);
		String taintedElement = notRelevantList.get(0);
		taintedElement.toString();
		String outcome = list.get(0);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(outcome);
	}
	
	public L1 l;
	public void paramTransferTest(){
		ConnectionManager cm = new ConnectionManager();
		String tainted = TelephonyManager.getDeviceId();
		l = new L1();
		taint(tainted, l);
		cm.publish(l.f);
	}
	
	public void taint(String e, L1 m){
		m.f = e;
	}
	
	class L1{
		String f;
	}
	
	@SuppressWarnings("unused")
	public void objectSensitiveTest1(){
		ConnectionManager cm = new ConnectionManager();
		IntermediateObject i1 = new IntermediateObject("123");
		IntermediateObject i2 = new IntermediateObject(TelephonyManager.getDeviceId());
		cm.publish(i1.getValue());
	}
	
	class IntermediateObject{
		Object1 o;
		
		public IntermediateObject(String s){
			o = new Object1();
			o.setField1(s);
		}
		
		public String getValue(){
			return o.getField1();
		}
	}

	class Object1{
		String field1;
		
		public void setField1(String s){
			field1 = s;
		}
		public String getField1(){
			return field1;
		}
	}
	
	class MyLinkedList {
		Object element;
		MyLinkedList nextElement;
	}
	
	public void accessPathTest() {
		MyLinkedList ll1 = new MyLinkedList();
		ll1.nextElement = new MyLinkedList();
		ll1.nextElement.nextElement = new MyLinkedList();
		ll1.nextElement.nextElement.nextElement = new MyLinkedList();
		ll1.nextElement.nextElement.nextElement.nextElement = new MyLinkedList();
		
		String tainted = TelephonyManager.getDeviceId();

		MyLinkedList taintedList = new MyLinkedList();
		taintedList.element = tainted;
		taintedList.nextElement = new MyLinkedList();
		ll1.nextElement.nextElement.nextElement.nextElement.nextElement = taintedList;
		ll1.nextElement.nextElement.nextElement.nextElement.nextElement.nextElement.nextElement = null;

		ConnectionManager cm = new ConnectionManager();
		
		cm.publish((String) ll1.nextElement.nextElement.nextElement.nextElement.nextElement.element);
	}

	public void pathSkipTest() {
		O o = new O();
		o.field = TelephonyManager.getDeviceId();
		o.get();
		String x = o.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x);
	}
		
	public void pathSkipTest2() {
		O o = new O();
		AccountManager am = new AccountManager();
		String deviceId = TelephonyManager.getDeviceId();

		// make sure o.field is tainted before the first call to set()
		o.field = TelephonyManager.getDeviceId();
		
		// call set() twice with different sources
		o.set(deviceId);
		o.set(am.getPassword());
		
		String x = o.get();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(x);
	}
	
	private String id(String data) {
		String foo = data;
		return foo;
	}
	
	public void pathSkipTest3() {
		AccountManager am = new AccountManager();
		String deviceId = TelephonyManager.getDeviceId();
		String data = "";

		// call set() twice with different sources
		data = id(deviceId);
		System.out.println(data);
		data = id(am.getPassword());
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
	}

	public void pathSkipTest4() {
		AccountManager am = new AccountManager();
		String deviceId = TelephonyManager.getDeviceId();
		String data = "";
		String data2 = "";
		
		data = id(deviceId);
		System.out.println(data);
		data2 = id(am.getPassword());
		System.out.println(data2);
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
	}
	
	public void pathSkipTest5() {
		AccountManager am = new AccountManager();
		String deviceId = TelephonyManager.getDeviceId();
		String data = "";

		// call set() twice with different sources
		data = id(deviceId);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
		data = id(am.getPassword());
		System.out.println(data);
	}
	
	private String id2(String data) {
		return id(data);
	}
	
	public void pathSkipTest6() {
		AccountManager am = new AccountManager();
		String deviceId = TelephonyManager.getDeviceId();
		String data = "";

		// call set() twice with different sources
		data = id(deviceId);
		data = id(data);
		System.out.println(data);

		data = id2(am.getPassword());
		data = id2(data);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
	}

	public void noPathsTest1() {
		String deviceId = TelephonyManager.getDeviceId();
		String did = id(deviceId);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(did);
	}

	
	public void recursionTest1() {
		String deviceId = TelephonyManager.getDeviceId();
		recurse("", deviceId);
//		recurse(deviceId, "");
	}

	private void recurse(String deviceId, String deviceId2) {
		recurse(deviceId2, deviceId);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(deviceId2);
	}
	
	private class MyAction implements PrivilegedAction<O> {

		private String data = "";
		
		@Override
		public O run() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data);
			return new O();
		}
		
	}
	
	public void doPrivilegedTest1() {
		MyAction action = new MyAction();
		action.data = TelephonyManager.getDeviceId();
		AccessController.doPrivileged(action);
	}
	
	public void doPrivilegedTest2() {
		MyAction action = new MyAction();
		action.data = TelephonyManager.getDeviceId();
		AccessController.doPrivileged(action, null);
	}

	private class MyHeapAction implements PrivilegedAction<O> {

		private String data = "";
		
		@Override
		public O run() {
			this.data = TelephonyManager.getDeviceId();
			return new O();
		}
		
	}

	public void doPrivilegedTest3() {
		MyHeapAction action = new MyHeapAction();
		AccessController.doPrivileged(action, null);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(action.data);
	}
	
	public void multiSinkTest1() {
		String imei = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei);
		cm.publish(imei);
	}

	public void multiSinkTest2() {
		String imei = TelephonyManager.getDeviceId();
		doLeak(imei);		
		doLeak(imei);
	}

	private void doLeak(String imei) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei);
	}
	
	public void skipOverObjectTest1() {
		String imei = TelephonyManager.getDeviceId();
		Object o = new Object();
		System.out.println(o);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei);
	}
		
	public void skipOverObjectTest2() {
		C1 a = new C1(TelephonyManager.getDeviceId());
		a.notify();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.field1);
	}

}
