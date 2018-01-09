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

/**
 * Code for testing the EasyTaintWrapper
 * 
 * @author Steven Arzt
 */
public class EasyWrapperTestCode {
	
	private class A {
		String data;
		
		public A(String data) {
			this.data = data;
		}
		
		@Override
		public boolean equals(Object other) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data);
			return super.equals(other);
		}
		
		@Override
		public int hashCode() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data);
			return super.hashCode();			
		}
		
		public String getConstant() {
			return "Hello World";
		}
	}
	
	public void equalsTest() {
		String tainted = TelephonyManager.getDeviceId();
		A a = new A(tainted);
		if (a.equals(null))
			System.out.println("Hello World");
	}

	public void hashCodeTest() {
		String tainted = TelephonyManager.getDeviceId();
		A a = new A(tainted);
		System.out.println("Hello World " + a.hashCode());
	}
	
	private class B {	
		String data;
		
		public B(String data) {
			this.data = data;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof B))
				return false;
			if (this == other)
				return true;
			B otherB = (B) other;
			return this.data.equals(otherB.data);
		}
		
		@Override
		public int hashCode() {
			return 31 * this.data.hashCode();			
		}
	}
	
	public void equalsTest2() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B(tainted);
		String s = "" + b.equals(new B("x"));
		ConnectionManager cm = new ConnectionManager();
		cm.publish(s);
	}

	public void hashCodeTest2() {
		String tainted = TelephonyManager.getDeviceId();
		B b = new B(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish("" + b.hashCode());
	}
	
	public void constantTest1() {
		String tainted = TelephonyManager.getDeviceId();
		A a = new A(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(a.getConstant());
	}
	
	private interface I1 {
		public String getSecret();
		public void taintMe(String s);
	}
	
	private interface I2 extends I1 {
		
	}
	
	private class C implements I2 {
		private String data;
		
		public C(String data) {
			this.data = data;
		}
		
		public String getSecret() {
			return "Fake secret";
		}
		
		public void taintMe(String s) {
			// do nothing
		}
		
		public String getData() {
			return this.data;
		}
		
	}
	
	public void interfaceInheritanceTest() {
		String tainted = TelephonyManager.getDeviceId();
		C c = new C(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.getSecret());
	}

	public void interfaceInheritanceTest2() {
		String tainted = TelephonyManager.getDeviceId();
		C c = new C("");
		c.taintMe(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.getData());
	}

	public void interfaceInheritanceTest3() {
		String tainted = TelephonyManager.getDeviceId();
		C c = new C("");
		I2 i2 = (I2) c;
		i2.taintMe(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.getData());
	}

	public void interfaceInheritanceTest4() {
		String tainted = TelephonyManager.getDeviceId();
		C c = new C("");
		I2 i2 = (I2) c;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.getData());
		i2.taintMe(tainted);
	}
	
	public void stringConcatTest() {
		String tainted = TelephonyManager.getDeviceId();
		String tainted2 = (new AccountManager()).getPassword();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted + tainted2);
	}

}
