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

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
/**
 * checks if taint is removed after overwriting the values (for both string and list<string>)
 * @author Christian
 *
 */
public class OverwriteTestCode {
	static String staticString;
	static List<String> staticList;
	
	public void varOverwrite(){
		String var;
		List<String> varList = new LinkedList<String>();
		String tainted = TelephonyManager.getDeviceId();
		var = tainted;
		varList.add(tainted);
		var = "123";
		varList = new LinkedList<String>();
		varList.add("123");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(var);
		cm.publish(varList.get(0));
		
	}
	
	public void staticFieldOverwrite(){
		String tainted = TelephonyManager.getDeviceId();
		OverwriteTestCode.staticString = tainted;
		OverwriteTestCode.staticString = "123";
		OverwriteTestCode.staticList = new LinkedList<String>();
		OverwriteTestCode.staticList.add(tainted);
		OverwriteTestCode.staticList = new LinkedList<String>();
		OverwriteTestCode.staticList.add("123");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(OverwriteTestCode.staticString);
		cm.publish(OverwriteTestCode.staticList.get(0));
	}
	
	public void fieldOverwrite(){
		String tainted = TelephonyManager.getDeviceId();
		Test1 t = new Test1();
		t.field = tainted;
		t.list.add(tainted);
		t.field = "123";
		t.list = new LinkedList<String>();
		t.list.add("123");
		ConnectionManager cm = new ConnectionManager();
		cm.publish(t.field);
		cm.publish(t.list.get(0));
	}
	
	public void returnOverwrite(){
		String tainted = TelephonyManager.getDeviceId();
		Test1 t = new Test1();
		t.field = tainted;
		t.field = t.testMethod();
		t.list.add(tainted);
		t.list = t.testMethodList();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(t.field);
		cm.publish(t.list.get(0));
	}
	
	public void returnOverwrite2(){
		String var;
		List<String> varList = new LinkedList<String>();
		String tainted = TelephonyManager.getDeviceId();
		Test1 t = new Test1();
		var = tainted;
		var = t.testMethod();
		varList.add(tainted);
		varList = t.testMethodList();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(var);
		cm.publish(varList.get(0));
	}
	
	public void returnOverwrite3(){
		String tainted = TelephonyManager.getDeviceId();
		Test1 t = new Test1();
		OverwriteTestCode.staticString = tainted;
		OverwriteTestCode.staticString = t.testMethod();
		OverwriteTestCode.staticList = new LinkedList<String>();
		OverwriteTestCode.staticList.add(tainted);
		OverwriteTestCode.staticList = t.testMethodList();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(OverwriteTestCode.staticString);
		cm.publish(OverwriteTestCode.staticList.get(0));
	}
	
	public void returnOverwrite4(){
		String tainted = TelephonyManager.getDeviceId();
		tainted = null;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}

	public class Test1{
		String field;
		LinkedList<String> list = new LinkedList<String>();
		
		public String testMethod(){
			return "123";
		}
		
		public LinkedList<String> testMethodList(){
			LinkedList<String> result = new LinkedList<String>();
			result.add("123");
			return result;
		}
		
	}
	
	private String data = "";
	
	private void setData(String data) {
		this.data = data;
	}
	
	public void returnOverwrite5(){
		setData(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
		setData(null);
		cm.publish(data);
	}

	private static String dataStatic = "";
	
	private static void setDataStatic(String data) {
		dataStatic = data;
	}
	
	public void returnOverwrite6(){
		setDataStatic(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish(dataStatic);
		setDataStatic(null);
		cm.publish(dataStatic);
	}

	private class DataClass {
		public String data = "";
		public DataClass2 dataClass2;
		
		public void clear() {
			dataClass2 = null;
		}
		
	}
	private DataClass data2 = new DataClass();
	
	private class DataClass2 {
		public String data = "";		
	}
	
	private void setData2(String data) {
		this.data2.data = data;
	}

	public void returnOverwrite7(){
		setData2(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data2.data);
		setData2(null);
		cm.publish(data2.data);
	}
	
	private String nonsource() {
		return "foo";
	}
	
	public void loopOverwrite() {
		String tmp;
		ConnectionManager cm = new ConnectionManager();
		do {
			cm.publish(nonsource());
			tmp = TelephonyManager.getDeviceId();
			cm.publish(tmp);
		}
		while (true);
	}

	public void loopOverwrite2() {
		String tmp;
		ConnectionManager cm = new ConnectionManager();
		do {
			tmp = nonsource();
			cm.publish(tmp);
			tmp = TelephonyManager.getDeviceId();
			cm.publish(tmp);
		}
		while (true);
	}
	
	public void overwriteAlias() {
		DataClass dc = new DataClass();
		dc.dataClass2 = new DataClass2();
		dc.dataClass2.data = TelephonyManager.getDeviceId();
		DataClass dc2 = new DataClass();
		dc2.dataClass2 = dc.dataClass2;
		dc2.clear();
		ConnectionManager cm = new ConnectionManager();		
		cm.publish(dc.dataClass2.data);
		cm.publish(dc2.dataClass2.data);
	}
	
	public void simpleOverwriteAliasTest1() {
		DataClass dc1 = new DataClass();
		DataClass dc2 = dc1;
		dc1.data = TelephonyManager.getDeviceId();
		dc2.data = null;
		ConnectionManager cm = new ConnectionManager();		
		cm.publish(dc1.data);
	}

}
