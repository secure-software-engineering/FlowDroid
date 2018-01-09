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
package soot.jimple.infoflow.test.android;

public class AccountManager {

	public String getPassword(){
		
		return "123";
	}
	
	public String[] getUserData(String user){
		String[] userData = new String[2];
		userData[0] = user;
		userData[1] = getPassword();
		return userData;
	}
}
