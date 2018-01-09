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

import java.util.Arrays;
import java.util.List;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.ClassWithFinal;

/**
 * tests array tainting
 * @author Christian
 *
 */
public class ArrayTestCode {
	
	static String[] staticTainted;
	transient String[] transTainted;
	String[] globalTainted;
	int[] globalIntTainted = new int[30];
	
	
	public void concreteWriteReadSamePosIntArrayTest(){
		int tainted = TelephonyManager.getIMEI();
		globalIntTainted[3] = tainted; 
		int taintedElement = globalIntTainted[3];
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadSamePosTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		String taintedElement = array[1];
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadDiffPosTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		
		//because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		String taintedElement2 = array[0];
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);	
	}
	
	
	public void concreteStaticTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		staticTainted = array;
		String[] tainted123 = staticTainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted123[0]);
	}
	
	public void concreteStaticTest2(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		staticTainted = array;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(staticTainted[0]);
	}
	
	public void concreteTransientTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		transTainted = array;
		String[] tainted456 = transTainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted456[0]);
	}
	
	public void concreteGlobalTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = tainted;
		globalTainted = array;
		String[] tainted789 = globalTainted;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted789[0]);		
	}
	
	
	public void copyTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[2];
		array[0] = tainted;
		String[] copyTainted = Arrays.copyOf(array, 100);
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(copyTainted[0]);
	}
	
	public void arrayAsFieldOfClass(){
		String tainted = TelephonyManager.getDeviceId();
		
		String[] array = new String[2];
		array[1] = "neutral";
		array[0] = tainted;
		
		ClassWithFinal<String> c = new ClassWithFinal<String>(array);
		String[] taintTaint = c.a;
		String y = taintTaint[0];
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y);
	}
	
	public void arrayAsListTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] array = new String[1];
		array[0] = tainted;
		List<String> list = Arrays.asList(array);
		String taintedElement = list.get(0);
		String dummyString = taintedElement;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(dummyString);
		
	}
	
	public void concreteWriteReadNegativeTest(){
		String tainted = TelephonyManager.getDeviceId();
		String[] notRelevant = new String[1];
		String[] array = new String[2];
		array[0] = "neutral";
		array[1] = "neutral2";
		
		notRelevant[0] = tainted;
		
		String taintedElement = notRelevant[0];
		String untaintedElement = array[0];
		taintedElement.toString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}

	public void arrayOverwriteTest(){
		String tainted = TelephonyManager.getDeviceId();
		
		String[] array = new String[2];
		array[0] = tainted;
		array[1] = "neutral";
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array[0]);
	}
	
	public void arrayLengthTest() {
		String tainted = TelephonyManager.getDeviceId();		
		char[] array = tainted.toCharArray();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array.length);
	}
	
	public void arrayLengthTest2() {
		char[] array = new char[TelephonyManager.getIMEI()];
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array.length);
	}
	
	public void arrayLengthTest3() {
		String[] array = new String[TelephonyManager.getIMEI()];
		array[0] = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array.length);
		cm.publish(array[0]);
	}
	
	public void arrayLengthTest4() {
		String[] array = new String[TelephonyManager.getIMEI()];
		array[0] = "foo";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array[0]);
	}
	
	public void arrayLengthTest5() {
		String[] array = new String[TelephonyManager.getIMEI()];
		String[] array2 = array;
		array2[0] = "foo";
		ConnectionManager cm = new ConnectionManager();
		cm.publish(array[0]);
	}
	
}
