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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;


public class HTTPTestCode {
	
	public void testURL() throws MalformedURLException{
		String urlString = "http://www.google.de/?q="+ TelephonyManager.getDeviceId();
		URL url = new URL(urlString);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(url.toString());
		}

	public void method1() throws IOException{
		String imei = TelephonyManager.getDeviceId();
    	URL url = new URL(imei);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setReadTimeout(10000 /* milliseconds */);
//        conn.setConnectTimeout(15000 /* milliseconds */);
//        conn.setRequestMethod("GET");
//        conn.setDoInput(true);
        // Starts the query
//        conn.connect();
        ConnectionManager cm = new ConnectionManager();
		cm.publish(conn.toString());
	}
}
