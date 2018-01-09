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

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Interface to configure Soot options like the output format or a list of
 * packages that should be included or excluded for analysis
 * 
 * @author Christian
 *
 */
public interface IInfoflowConfig {

	/**
	 * Configure Soot options (Be careful, wrong options can corrupt the
	 * analysis results!)
	 * 
	 * @param options
	 *            the singleton to configure soot options
	 * @param config
	 *            The configuration of the data flow solver
	 * 
	 */
	public void setSootOptions(soot.options.Options options, InfoflowConfiguration config);
}
