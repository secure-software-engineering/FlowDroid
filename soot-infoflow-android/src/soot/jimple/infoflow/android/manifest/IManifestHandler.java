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
package soot.jimple.infoflow.android.manifest;

import java.io.InputStream;

/**
 * Common interface for handlers working on Android manifest files
 * 
 * @author Steven Arzt
 *
 */
public interface IManifestHandler {
	
	/**
	 * Called when the contents of the Android manifest file shall be processed
	 * @param stream The stream through which the manifest file can be accesses
	 */
	public void handleManifest(InputStream stream);

}
