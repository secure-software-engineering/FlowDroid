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
package soot.jimple.infoflow.android.resources;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for all resource parser classes
 * 
 * @author Steven Arzt
 */
public abstract class AbstractResourceParser {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Opens the given apk file and provides the given handler with a stream for
	 * accessing the contained resource manifest files
	 * 
	 * @param apk            The apk file to process
	 * @param fileNameFilter If this parameter is non-null, only files with a name
	 *                       (excluding extension) in this set will be analyzed.
	 * @param handler        The handler for processing the apk file
	 */
	protected void handleAndroidResourceFiles(String apk, Set<String> fileNameFilter, IResourceHandler handler) {
		File apkF = new File(apk);
		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");

		try {
			try (ZipFile archive = new ZipFile(apkF)) {
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();

					try (InputStream is = archive.getInputStream(entry)) {
						handler.handleResourceFile(entryName, fileNameFilter, is);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error when looking for XML resource files in apk " + apk, e);
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}

}
