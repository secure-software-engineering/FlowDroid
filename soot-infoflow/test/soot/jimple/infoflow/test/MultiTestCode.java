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

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

public class MultiTestCode {
	
	public void multiSourceCode(){
		String tainted = TelephonyManager.getDeviceId();
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
				
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
		doSomething(pwd);
	}
	
	private void doSomething(String msg) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(msg);
	}
	
	private String pwd;

	public void multiSourceCode2(){
		AccountManager am = new AccountManager();
		this.pwd = am.getPassword();
				
		String tainted = TelephonyManager.getDeviceId();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
		doSomething();
	}

	private void doSomething() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(this.pwd);
	}

	public void ifPathTestCode1(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
		
		String foo = "";
		String bar = "";
		if (pwd.length() > 0)
			foo = pwd;
		else
			bar = pwd;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
		cm.publish(bar);
	}

	public void ifPathTestCode2(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
		
		String foo = "";
		if (pwd.length() > 0)
			foo = pwd;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void ifPathTestCode3(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
		
		String foo = pwd;
		if (pwd.length() > 0)
			foo = "";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void ifPathTestCode4(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
		String bar = am.getPassword();
		
		String foo = pwd;
		if (pwd.length() > 0)
			foo = bar;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void loopPathTestCode1(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();	
		sendPwd(pwd, 5);
	}
	
	private void sendPwd(String pwd, int cnt) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(pwd);
		if (cnt > 0)
			sendPwd(pwd, cnt - 1);
	}

	public void overwriteTestCode1(){
		AccountManager am = new AccountManager();
		String pwd = am.getPassword();
		System.out.println(pwd);
		
		pwd = new String("");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(pwd);
	}
	
	public void hashTestCode1(){
		AccountManager am = new AccountManager();
		int foo = am.getPassword().hashCode();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo);
	}

	public void shiftTestCode1(){
		AccountManager am = new AccountManager();
		int foo = am.getPassword().hashCode();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(foo << 32);
	}
	
	public void intMultiTest() {
		int imei = TelephonyManager.getIMEI();
		int imsi = TelephonyManager.getIMSI();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei + imsi);
	}
	
	int intField = 0;

	public void intMultiTest2() {
		int imei = TelephonyManager.getIMEI();
		int imsi = TelephonyManager.getIMSI();
		intField = imei + imsi;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(intField);
	}
	
	private String id(String s) {
		return s;
	}
	
	public void sameSourceMultiTest1() {
		String[] data = new String[2];
		data[0] = id(TelephonyManager.getDeviceId());
		data[1] = id(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data[0]);
	}

}
