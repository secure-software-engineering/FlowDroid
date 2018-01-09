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
 * Target code for tests that require the taint tracking engine to correctly
 * evaluate the semantics of primitive operations.
 * 
 * @author Steven Arzt
 */
public class OperationSemanticTestCode {
	
	public void baseTestCode() {
		String deviceID = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		int len = deviceID.length();
		cm.publish(len);
	}

	public void mathTestCode() {
		String deviceID = TelephonyManager.getDeviceId();
		ConnectionManager cm = new ConnectionManager();
		int len = deviceID.length();
		len = len - len;
		cm.publish(len);
	}

}
