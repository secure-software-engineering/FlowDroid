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

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Tests constant tainting and dead-code elimination for checks on constants
 * 
 * @author Christian Fritz
 * @author Steven Arzt
 *
 */
public class ConstantTestCode {
	
	static final String tainted = TelephonyManager.getDeviceId();
	static final String[] staticArray = new String[1];
	final String[] fieldArray = new String[1];

	public void easyConstantFieldTest(){
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}
	
	public void easyConstantVarTest(){
		final String e = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(e);
	}
	
	public void constantArrayTest(){
		String tainted =  TelephonyManager.getDeviceId();
		fieldArray[0] = tainted;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(fieldArray[0]);
	}

	public void constantStaticArrayTest(){
		String tainted =  TelephonyManager.getDeviceId();
		staticArray[0] = tainted;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticArray[0]);
	}

	public void constantFieldArrayTest(){
		String tainted =  TelephonyManager.getDeviceId();
		staticArray[0] = tainted;
		fieldArray[0] = tainted;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticArray[0]);
		cm.publish(fieldArray[0]);
	}
	
	public void constantFieldTest(){
		ConstantClass c = new ConstantClass();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(c.e);
	}
	
	class ConstantClass{
		final String e;
		public ConstantClass(){
			e = TelephonyManager.getDeviceId();
		}
	}
	
	public void fpConstIntraproceduralTest1() {
		int i = 3;
		if (i > 5) {
			String tainted =  TelephonyManager.getDeviceId();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
		}
	}
	
	public void fpConstInterproceduralTest1() {
		int i = 3;
		leakIfGreaterThan(i, 5);
	}

	private void leakIfGreaterThan(int i, int j) {
		if (i > j) {
			String tainted = TelephonyManager.getDeviceId();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
		}
	}

	public void fpConstInterproceduralTest2() {
		int i = 3;
		indirectLeakIfGreaterThan(i);
	}

	private void indirectLeakIfGreaterThan(int i) {
		leakIfGreaterThan(i, 5);
	}

	public void fpConstInterproceduralTest3() {
		int i = 3;
		indirectLeakIfGreaterThan2(i);
	}
	
	private void indirectLeakIfGreaterThan2(int i) {
		int j = i;
		leakIfGreaterThan(j, 5);
	}
	
	public void fpConstInterproceduralTest4() {
		int i = 3;
		indirectLeakIfGreaterThan2(i);
		indirectLeakIfGreaterThan2(7);
	}
	
	public void fpConstInterproceduralTest5() {
		int i = 3;
		indirectLeakIfGreaterThan2(i);
		indirectLeakIfGreaterThan2(3);
	}
	
	public void constRecursiveTest1() {
		leakRecursive(0);
	}
	
	private void leakRecursive(int i) {
		if (i > 0) {			
			String tainted = TelephonyManager.getDeviceId();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
			return;
		}
		leakRecursive(0);
	}
	
	public void fpConstInterproceduralTest6() {
		int i = 3;
		leakIfGreaterThan(i, get5());
	}
	
	private int get5() {
		return 5;
	}
	
	private class MyException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3653915262998567435L;
		
	}
	
	public void constantExceptionTest1() {
		try {
			String secret = getSecretAndThrow();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(secret);			
		}
		catch (MyException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex + TelephonyManager.getDeviceId());			
		}
	}
	
	private String getSecretAndThrow() {
		if (Math.random() < 0.5)
			return "foo";
		else
			throw new MyException();
	}
	
	private abstract class Base {
		
		public abstract String transform(String data);
		
	}
	
	private class A extends Base {
		
		@Override
		public String transform(String data) {
			return data;
		}

	}
	
	private class B extends Base {
		
		@Override
		public String transform(String data) {
			return "foo";
		}

	}
	
	public void allocSiteTest1() {
		String tainted =  TelephonyManager.getDeviceId();
		int i = 3;
		Base obj = i > 10 ? new A() : new B();
		String copy = obj.transform(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(copy);
	}
	
	private void nextNextLevelConst(int i) {
		if (i > 10) {
			String tainted = TelephonyManager.getDeviceId();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
		}
	}
	
	public void multiLevelConstTest1() {
		nextNextLevelConst(1);
		nextLevelConst(1);
	}
	
	private void nextLevelConst(int i) {
		nextNextLevelConst(i);
	}
	
	public void multiLevelReturnTest1() {
		int i = returnFromNextLevel();
		int j = returnFromNextNextLevel();
		if (i != j) {
			String tainted = TelephonyManager.getDeviceId();
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
		}
	}
	
	private int returnFromNextLevel() {
		return returnFromNextNextLevel();
	}
	
	private int returnFromNextNextLevel() {
		return 5;
	}
	
}
