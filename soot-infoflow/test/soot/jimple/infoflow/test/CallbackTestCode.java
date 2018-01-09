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

public class CallbackTestCode {
	public void checkLocation(){
		Container con = new Container();
		MyLocationListener locationListener = new MyLocationListener(con); 
		
		locationListener.onLocationChanged();
		con.publish();
	}
	
	private class MyLocationListener {  
		
		private Container con;
		
		public MyLocationListener(Container con) {
			this.con = con;
		}
		
		  public void onLocationChanged() {
			  con.field = TelephonyManager.getDeviceId();
		  }
	}
	
	private class Container{
		private String field = "";

		public void publish() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(field);
		}
		
	}
	
	//---------- 2nd try:
	
	public void tryNext(){
		Activity a = new Activity();
		a.onCreate();
		LocListener l2 = new LocListener(a);
		l2.set();
		a.send();
	}
	
	private class Activity{
		String field;
		public void onCreate(){
			@SuppressWarnings("unused")
			LocListener l = new LocListener(this);
			
		}
		
		public void send(){
			ConnectionManager cm = new ConnectionManager();
			cm.publish(field);
		}
		
		
	}
	
	private class LocListener{
		private Activity parent;
		public LocListener(Activity a){
			parent = a;
		}
		
		public void set(){
			parent.field = TelephonyManager.getDeviceId();
		}
		
	}
	
	//---------- 3rd try:
	
	public void tryNext2(){
		Activity2 a = new Activity2();
		a.onCreate();
		a.send();
	}
	
	private class Activity2{
		String field;
		public void onCreate(){
			LocListener2 l = new LocListener2(this);
			l.set();
		}
		
		public void send(){
			ConnectionManager cm = new ConnectionManager();
			cm.publish(field);
		}
		
		
	}
	
	private class LocListener2{
		private Activity2 parent;
		public LocListener2(Activity2 a){
			parent = a;
		}
		
		public void set(){
			parent.field = TelephonyManager.getDeviceId();
		}
		
	}

}
