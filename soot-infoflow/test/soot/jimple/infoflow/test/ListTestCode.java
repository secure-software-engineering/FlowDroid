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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * tests list tainting
 * 
 * @author Christian Fritz
 * @author Steven Arzt
 * 
 */
public class ListTestCode {

	public void concreteWriteReadPos0Test() {
		String tainted = TelephonyManager.getDeviceId();
		ArrayList<String> list = new ArrayList<String>();
		list.add("neutral");
		list.add(tainted);

		String taintedElement2 = list.get(0);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}

	public void concreteWriteReadPos1Test() {
		String tainted = TelephonyManager.getDeviceId();
		ArrayList<String> list = new ArrayList<String>();
		list.add("neutral");
		list.add(tainted);

		String taintedElement = list.get(1);
		// because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void concreteWriteReadNegativeTest() {
		String tainted = TelephonyManager.getDeviceId();
		ArrayList<String> notRelevantList = new ArrayList<String>();
		ArrayList<String> list = new ArrayList<String>();
		list.add("neutral");
		notRelevantList.add(tainted);
		String taintedElement = notRelevantList.get(0);
		String untaintedElement = list.get(0);
		taintedElement.toString();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}

	public void writeReadTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new ArrayList<String>();
		list.add("neutral");
		list.add(tainted);

		String taintedElement = list.get(1);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void iteratorTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new ArrayList<String>();
		list.add(tainted);
		list.add("neutral");

		Iterator<String> it = list.iterator();
		String taintedElement = it.next();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void subListTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new ArrayList<String>();
		list.add("neutral");
		list.add(tainted);
		List<String> subList = list.subList(1, 1);
		String taintedElement = subList.get(0);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void linkedListConcreteWriteReadTest() {
		String tainted = TelephonyManager.getDeviceId();
		LinkedList<String> list = new LinkedList<String>();
		list.add(tainted);
		// because whole list is tainted, even untainted elements are tainted if they are fetched from the list
		String taintedElement2 = list.get(0);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}

	public void linkedListConcreteWriteReadNegativeTest() {
		String tainted = TelephonyManager.getDeviceId();
		LinkedList<String> notRelevantList = new LinkedList<String>();
		LinkedList<String> list = new LinkedList<String>();
		list.add("neutral");
		notRelevantList.add(tainted);
		String taintedElement = notRelevantList.get(0);
		String untaintedElement = list.get(0);
		taintedElement.toString();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}

	public void linkedListWriteReadTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new LinkedList<String>();
		list.add("neutral");
		list.add(tainted);

		String taintedElement = list.get(1);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);

	}

	public void linkedListIteratorTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new LinkedList<String>();
		list.add("neutral");
		list.add(tainted);

		Iterator<String> it = list.iterator();
		it.next();
		String taintedElement2 = it.next();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}

	public void linkedListSubListTest() {
		String tainted = TelephonyManager.getDeviceId();
		List<String> list = new LinkedList<String>();
		list.add("neutral");
		list.add(tainted);

		List<String> subList = list.subList(1, 1);
		String taintedElement = subList.get(0);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void concreteWriteReadStackGetTest() {
		String tainted = TelephonyManager.getDeviceId();
		Stack<String> stack = new Stack<String>();
		stack.addElement("neutral");
		stack.push(tainted);

		String taintedElement2 = stack.get(0);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement2);
	}

	public void concreteWriteReadStackPeekTest() {
		String tainted = TelephonyManager.getDeviceId();
		Stack<String> stack = new Stack<String>();
		stack.addElement("neutral");
		stack.push(tainted);

		String taintedElement = stack.peek();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement);
	}

	public void concreteWriteReadStackPopTest() {
		String tainted = TelephonyManager.getDeviceId();
		Stack<String> stack = new Stack<String>();
		stack.addElement("neutral");
		stack.push(tainted);

		String taintedElement3 = stack.pop();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(taintedElement3);
	}

	public void concreteWriteReadStackNegativeTest() {
		String tainted = TelephonyManager.getDeviceId();
		Stack<String> stack = new Stack<String>();
		Stack<String> stack2 = new Stack<String>();
		stack.addElement("neutral");
		stack2.push(tainted);
		stack2.add(tainted);
		String untaintedElement = stack.get(0);
		String taintedElement = stack2.peek();
		taintedElement = stack2.pop();
		taintedElement = stack2.get(0);
		taintedElement.toString();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(untaintedElement);
	}

	private final Collection<String> c = new LinkedList<String>();

	public void linkedList() {
		bar2(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) c.iterator().next());
	}

	public void staticLinkedList() {
		bar(TelephonyManager.getDeviceId());
		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) COLLECTION1.iterator().next());
	}

	@SuppressWarnings("rawtypes")
	static final Collection COLLECTION1 = new LinkedList();

	@SuppressWarnings("unchecked")
	private void bar(Object s) {
		COLLECTION1.add(s);
	}

	private void bar2(String s) {
		c.add(s);
	}
	
	public void listToStringTest() {
        String secret = TelephonyManager.getDeviceId();

        ArrayList<String> secretList = new ArrayList<String>();
        secretList.add("asd");
        secretList.add(secret);
        secretList.add("aaa");
        
        ConnectionManager cm = new ConnectionManager();
        cm.publish(secretList.toString());
	}
	
	public void iteratorHasNextTest() {
		List<String> list = new ArrayList<String>();
		list.add(TelephonyManager.getDeviceId());
		boolean b = list.iterator().hasNext();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b);
	}

}
