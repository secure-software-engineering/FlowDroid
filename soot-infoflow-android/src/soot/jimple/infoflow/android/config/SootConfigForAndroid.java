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
package soot.jimple.infoflow.android.config;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

public class SootConfigForAndroid implements IInfoflowConfig {

	@Override
	public void setSootOptions(Options options, InfoflowConfiguration config) {
		// explicitly include packages for shorter runtime:
		List<String> excludeList = new LinkedList<String>();
		excludeList.add("java.*");
		excludeList.add("javax.*");

		excludeList.add("sun.*");

		// exclude classes of android.* will cause layout class cannot be
		// loaded for layout file based callback analysis.

		// 2020-07-26 (SA): added back the exclusion, because removing it breaks
		// calls to Android SDK stubs. We need a proper test case for the layout
		// file issue and then see how to deal with it.
		excludeList.add("android.*");
		excludeList.add("androidx.*");

		excludeList.add("org.apache.*");
		excludeList.add("org.eclipse.*");
		excludeList.add("soot.*");
		options.set_exclude(excludeList);
		Options.v().set_no_bodies_for_excluded(true);
	}

}
