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

public class ForwardBackwardTest {

	public void testMethod(){
		C c = new C();
		A a = new A();
		c.h = TelephonyManager.getDeviceId();
		G b = a.g;
		foo(a, c);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(b.f);
	}
	
	public void foo(A z, C t){
		G x = z.g;
		String w = t.h;
		x.f = w;
	}
	
	private class A{
		public G g;
		
		public A() {
			this.g = new G();
		}
	}
	
	private class C{
		public String h;
	}
	private class G{
		public String f;
	}
}
