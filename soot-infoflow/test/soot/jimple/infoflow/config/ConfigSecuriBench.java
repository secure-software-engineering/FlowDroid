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
package soot.jimple.infoflow.config;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.options.Options;

public class ConfigSecuriBench implements IInfoflowConfig {

	@Override
	public void setSootOptions(Options options, InfoflowConfiguration config) {
		// explicitly include packages for shorter runtime:
		List<String> includeList = new LinkedList<String>();
		includeList.add("java.lang.*");
		includeList.add("java.util.*");
		includeList.add("java.io.*");
		includeList.add("sun.misc.*");
		includeList.add("java.net.*");
		includeList.add("org.apache.http.*");
		includeList.add("de.test.*");
		includeList.add("soot.*");
		includeList.add("securibench.*");
		includeList.add("javax.servlet.*");
		includeList.add("com.oreilly.servlet.*");
		// includeList.add("java.security.");
		// includeList.add("javax.crypto.");
		options.set_include(includeList);
		options.set_output_format(Options.output_format_none);
	}

}
