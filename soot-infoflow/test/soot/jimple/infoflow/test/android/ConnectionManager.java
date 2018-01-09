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

public class ConnectionManager {

	public void publish(String str){
		System.out.println(str);
		//publish on internet...
	}

	public void publish(int i){
		System.out.println(i + "");
		//publish on internet...
	}

	public void publish(boolean b){
		System.out.println(b + "");
		//publish on internet...
	}

	public void publish(Double dbl){
		System.out.println(dbl + "");
		//publish on internet...
	}
	
}
