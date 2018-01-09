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
package soot.jimple.infoflow.handlers;

import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Handler that is called when information flow results become available
 * @author Steven Arzt
 */
public interface ResultsAvailableHandler {

	/**
	 * Callback that is invoked when information flow results are available
	 * @param cfg The program graph
	 * @param results The results that were computed
	 */
	public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results);

}
