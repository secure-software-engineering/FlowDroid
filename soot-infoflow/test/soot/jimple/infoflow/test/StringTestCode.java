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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.ClassWithField;

public class StringTestCode {
	
	private ClassWithField fieldc = new ClassWithField();
	
	public void multipleSources(){
		String tainted1 = TelephonyManager.getDeviceId();
		AccountManager mgr = new AccountManager();
		String tainted2 = mgr.getPassword();
		
		String result = tainted1 + tainted2;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodSubstring(){
		String tainted = TelephonyManager.getDeviceId();
		String result = tainted.substring(1, 2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodStringLowerCase(){
		String tainted = TelephonyManager.getDeviceId();
		String result = tainted.toLowerCase();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodStringUpperCase(){
		String tainted = TelephonyManager.getDeviceId();
		String result = tainted.toUpperCase();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodStringConcat1(){
		String pre = "pre";
		String tainted = TelephonyManager.getDeviceId();
		String result = pre.concat(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodStringConcat1b(){
		String var = "2";
		String tainted = TelephonyManager.getDeviceId();
		var = var.concat(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(var);
	}
	
	public void methodStringConcat1c(String var){
		String tainted = TelephonyManager.getDeviceId();
		String result = var.concat(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodStringConcat2(){
		String pre = "pre";
		String tainted = TelephonyManager.getDeviceId();
		String post = tainted.concat(pre);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(post);
	}
	
	public void stringConcatTest(){
		String tainted = TelephonyManager.getDeviceId();
		String concat1 = tainted.concat("eins");
		String two = "zwei";
		String concat2 = two.concat(tainted);
		String concat3 = "test " + tainted;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(concat1.concat(concat2).concat(concat3));	
	}
	
	
	public void stringConcatTestSmall1(){
		String tainted = TelephonyManager.getDeviceId();
		String one = tainted.concat("zwei").concat("eins");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(one);
	}
	
	public void stringConcatTestSmall2(){
		String tainted = TelephonyManager.getDeviceId();
		String concat1 = tainted.concat("eins");
		String two = "zwei";
		String concat2 = two.concat(tainted);
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(concat1.concat(concat2).concat("foo"));
	}
	
	public void methodStringConcatNegative(){
		String pre = "pre";
		String tainted = TelephonyManager.getDeviceId();
		@SuppressWarnings("unused")
		String post = tainted.concat(pre);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(pre);
		
	}
	
	public void methodStringConcatPlus1(){
		String pre = "pre";
		String tainted = TelephonyManager.getDeviceId();;
		String post =  tainted + pre;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(post);
	}
	
	public void methodStringConcatPlus2(){
		String pre = "pre";
		String tainted = TelephonyManager.getDeviceId();
		String result = pre + tainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodValueOf(){
		String tainted = TelephonyManager.getDeviceId();
		String result = String.valueOf(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result);
	}
	
	public void methodtoString(){
		String tainted = TelephonyManager.getDeviceId();
		String result2 = tainted.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(result2);
	}
	
	public void methodStringBuffer1(){
		StringBuffer sb = new StringBuffer(TelephonyManager.getDeviceId());
//		sb.append("123");
		String test = sb.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}
	
	public void methodStringBuffer2(){
		StringBuffer sb = new StringBuffer("12");
		sb.append(TelephonyManager.getDeviceId());
		String test = sb.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}
	
	public void methodStringBuilder1(){
		StringBuilder sb = new StringBuilder(TelephonyManager.getDeviceId());
		//sb.append("123");
		String test = sb.toString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}
	
	public void methodStringBuilder2(){
		StringBuilder sb = new StringBuilder();
		sb.append(TelephonyManager.getDeviceId());
		String test = sb.toString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(test);	
	}
	
	public void methodStringBuilder3(){
		String tainted = TelephonyManager.getDeviceId();
		StringBuilder sb = new StringBuilder("Hello World");
        sb.append(tainted);
        String test = sb.toString();
        ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}

	public void methodStringBuilder4(String a){
		String b = a;
		fieldc.field = b;
		String tainted = TelephonyManager.getDeviceId();
		StringBuilder sb = new StringBuilder("Hello World");
        sb.append(tainted);
        String test = sb.toString();
        String c = fieldc.field;
        ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
		cm.publish(c);
		
		ClassWithField cf = new ClassWithField();
		cf.field = fieldc.field;
	}

	public void methodStringBuilder5(){
		String tainted = TelephonyManager.getDeviceId();
		StringBuilder sb = new StringBuilder("Hello World");
        sb.insert(0, tainted);
        String test = sb.toString();
        ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}

	public void methodStringBuilder6(){
		String tainted = TelephonyManager.getDeviceId();
		StringBuilder sb = new StringBuilder("Hello World");
        sb.insert(0, Integer.valueOf(tainted));		// does not run, just for simplicity
        String test = sb.toString();
        ConnectionManager cm = new ConnectionManager();
		cm.publish(test);
	}

	public void getChars(){
		//like: str.getChars(0, len, value, count);
		String t = TelephonyManager.getDeviceId();
		char[] x = new char[t.length()];
		t.getChars(0, t.length(), x, 0);
	
		ConnectionManager cm = new ConnectionManager();
		cm.publish(String.valueOf(x));
	}
	
	public void methodStringConcat() {
		String deviceID = TelephonyManager.getDeviceId();
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(deviceID + pwd);
	}
	
	public void methodStringConvert() {
		String deviceID = TelephonyManager.getDeviceId();
		int i1 = Integer.valueOf(deviceID);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(String.valueOf(i1));
	}

	public void methodStringConstructor() throws IOException {
		String s1 = new String("string1");
		Runtime.getRuntime().exec(s1);
	}
	
	public void methodToCharArray(){
        String tainted = TelephonyManager.getDeviceId();
        char[] chars = tainted.toCharArray();
        String newString = "";
        for (char c : chars) {
            newString += c;
        }
        
        ConnectionManager cm = new ConnectionManager();
        cm.publish(newString);
    }
	
	public void stringFileStreamTest1() throws IOException {
		FileInputStream fis1 = new FileInputStream(new File("D1.txt"));
		byte[] b1 = read(new byte[100], fis1);

		String s1 = new String(b1, StandardCharsets.UTF_8);
		FileOutputStream fos = new FileOutputStream(new File("D3.txt"));
		fos.write(s1.getBytes());
		fos.close();
	}

	private byte[] read(byte[] bs, FileInputStream fis1) throws IOException {
		fis1.read(bs, 0, 100);
		return bs;
	}
	
}
