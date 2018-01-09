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
package soot.jimple.infoflow.ipc;

import java.util.List;

import soot.SootMethod;

/**
 * A {@link IIPCManager} working on lists of IPC methods
 * 
 * @author Steven Arzt
 */
public class DefaultIPCManager extends MethodBasedIPCManager {

	private List<String> ipcMethods;
	
	/**
	 * Creates a new instance of the {@link DefaultIPCManager} class
	 * @param ipcMethods The list of methods to be treated as IPCs
	 */
	public DefaultIPCManager(List<String> ipcMethods) {
		this.ipcMethods = ipcMethods;
	}

	
	/**
	 * Sets the list of methods to be treated as IPCs
	 * @param ipcMethods The list of methods to be treated as IPCs
	 */
	public void setSinks(List<String> ipcMethods){
		this.ipcMethods = ipcMethods;
	}
	
	@Override
	public boolean isIPCMethod(SootMethod sMethod) {
		return ipcMethods.contains(sMethod.toString());
	}

	@Override
    public void updateJimpleForICC() {
        return;

    }
}
