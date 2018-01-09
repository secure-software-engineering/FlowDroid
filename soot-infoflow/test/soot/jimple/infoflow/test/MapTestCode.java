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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * 
 * @author Christian
 *
 */
public class MapTestCode {
	
	public void concreteWriteReadPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		String taintedElement2 = map.get("tainted");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void concreteLinkedWriteReadPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		String taintedElement2 = map.get("tainted");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}

	public void concreteWriteReadPos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		
		String taintedElement = map.get("neutral");
		//because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteRead2Test(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(tainted, "tainted");
		
		String taintedElement2 = map.get(tainted);
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void writeReadPos0Test(){
		String tainted = TelephonyManager.getDeviceId();
		Map<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		
		//because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		String taintedElement2 = map.get("neutral");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void writeReadPos1Test(){
		String tainted = TelephonyManager.getDeviceId();
		Map<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		String taintedElement = map.get("tainted");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void entryTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		Set<Entry<String, String>> entries = map.entrySet();
		String taintedElement = entries.iterator().next().getValue();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void iteratorTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("neutral", "neutral");
		map.put("tainted", tainted);
		
		Iterator<Entry<String, String>> it = map.entrySet().iterator();
		String taintedElement = it.next().getValue(); //entry is not enough!
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}
	
	public void concreteWriteReadTableTest(){
		String tainted = TelephonyManager.getDeviceId();
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("tainted", tainted);
		String taintedElement2 = map.get("tainted");
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}
	
	public void concreteWriteReadNegativeTest(){
		String tainted = TelephonyManager.getDeviceId();
		HashMap<String, String> notRelevantList = new HashMap<String, String>();
		HashMap<String, String> list = new HashMap<String, String>();
		list.put("neutral", "neutral");
		notRelevantList.put("tainted", tainted);
		String taintedElement = notRelevantList.get("tainted");
		String untaintedElement = list.get("neutral");
		taintedElement.toString();
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}
	
	private String append(String s) {
		return s + "x";
	}
	
	public void loopCallTest() {
		String tainted = TelephonyManager.getDeviceId();
		while (tainted.length() < 100) {
			tainted = append(tainted);
			tainted = append(tainted);
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}
	
}
