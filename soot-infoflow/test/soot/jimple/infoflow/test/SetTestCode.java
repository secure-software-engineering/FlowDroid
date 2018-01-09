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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * tests set tainting
 * @author Christian
 *
 */
public class SetTestCode {
	
	public void concreteWriteReadHashTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashSet<String> set = new HashSet<String>();
		set.add(tainted);
		String taintedElement2 = set.iterator().next();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
		
	}
	
	public void containsTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashSet<String> set = new HashSet<String>();
		set.add(tainted);
		boolean x = set.contains(tainted);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(String.valueOf(x));
	}
	
	public void concreteWriteReadTreePos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		TreeSet<String> set = new TreeSet<String>();
		set.add("neutral");
		set.add(tainted);
		
		String taintedElement = set.last();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadTreePos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		TreeSet<String> set = new TreeSet<String>();
		set.add("neutral");
		set.add(tainted);
		//because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		String taintedElement2 = set.iterator().next();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void concreteWriteReadLinkedPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		set.add("neutral");
		set.add(tainted);
		Iterator<String> it = set.iterator();
		String taintedElement = it.next();
		//because whole list is tainted, even untainted elements are tainted if they are fetched from the list

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadLinkedPos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		set.add("neutral");
		set.add(tainted);
		Iterator<String> it = set.iterator();
		it.next();
		String taintedElement2 = it.next();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void writeReadTest(){
		String tainted = TelephonyManager.getDeviceId();
		Set<String> set = new HashSet<String>();
		set.add(tainted);
		String taintedElement = set.iterator().next();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void iteratorTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashSet<String> set = new HashSet<String>();
		set.add("neutral");
		set.add(tainted);
		
		Iterator<String> it = set.iterator();
		it.next();
		String taintedElement2 = it.next();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void concreteWriteReadNegativeTest(){
		String tainted = TelephonyManager.getDeviceId();
		TreeSet<String> notRelevantList = new TreeSet<String>();
		TreeSet<String> list = new TreeSet<String>();
		list.add("neutral");
		notRelevantList.add(tainted);
		String taintedElement = notRelevantList.first();
		String untaintedElement = list.first();
		taintedElement.toString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}
	
}
