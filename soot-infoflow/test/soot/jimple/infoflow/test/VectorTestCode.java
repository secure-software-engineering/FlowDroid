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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * 
 * @author Christian
 *
 */
public class VectorTestCode {
	
	public void concreteWriteReadPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		Vector<String> v = new Vector<String>();
		v.add(tainted);
		v.add("neutral");
		
		String taintedElement = v.get(0);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadPos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		Vector<String> v = new Vector<String>();
		v.add(tainted);
		v.add("neutral");
		//because whole collection is tainted, even untainted elements are tainted if they are fetched 
		String taintedElement2 = v.lastElement();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	
	public void iteratorPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		Collection<String> v = new Vector<String>();
		v.add("neutral");
		v.add(tainted);
		@SuppressWarnings("rawtypes")
		Iterator it = v.iterator();
		String taintedElement = (String) it.next();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void iteratorPos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		Collection<String> v = new Vector<String>();
		v.add("neutral");
		v.add(tainted);
		@SuppressWarnings("rawtypes")
		Iterator it = v.iterator();
		it.next();
		Object obj = it.next();
		String taintedElement2 = (String) obj;	
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	
	public void concreteIteratorTest(){
		String tainted = TelephonyManager.getDeviceId();
		Vector<String> v = new Vector<String>();
		v.add(tainted);
		
		Iterator<String> it = v.iterator();
		String taintedElement = it.next();	
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadNegativeTest(){
		String tainted = TelephonyManager.getDeviceId();
		Vector<String> notRelevantList = new Vector<String>();
		Vector<String> list = new Vector<String>();
		list.add("neutral");
		notRelevantList.add(tainted);
		String taintedElement = notRelevantList.get(0);
		String untaintedElement = list.get(0);
		taintedElement.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}
	
}
