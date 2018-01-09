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

public class LengthTestCode{
	
	public void easy1(){
		String taint = TelephonyManager.getDeviceId();
		Firstclass f = new Firstclass();
		f.data.secretValue = taint;
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(f.data.publicValue);	
	}
	
	public void method1(){
		String taint = TelephonyManager.getDeviceId();
		Firstclass f = new Firstclass();
		f.data.secretValue = taint;
		f.data.publicValue = "PUBLIC";
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(f.data.publicValue);	
		
	}
	
	public void method2(){
		String taint = TelephonyManager.getDeviceId();
		Firstclass f = new Firstclass();
		f.data.secretValue = taint;
		f.data.publicValue = "PUBLIC";
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(f.data.secretValue);	
		
	}
	
	class Firstclass{
		SecondClass data;
		
		public Firstclass(){
			data = new SecondClass();
		}
		
	}
	
	class SecondClass{
		public String secretValue;
		public String publicValue;
	}

}
