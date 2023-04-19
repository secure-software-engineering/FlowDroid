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
package soot.jimple.infoflow.results;

import java.util.*;

import heros.solver.Pair;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * For backwards analysis, we need to swap the sources and sinks to hide the
 * fact that we used sources as sinks and vice versa.
 * 
 * @author Tim Lange
 */
public class BackwardsInfoflowResults extends InfoflowResults {

	public BackwardsInfoflowResults() {
		super();
	}

	public BackwardsInfoflowResults(boolean pathAgnosticResults) {
		super(pathAgnosticResults);
	}

	@Override
	public Collection<Pair<ResultSourceInfo, ResultSinkInfo>> addResult(Collection<ISourceSinkDefinition> sinkDefinitions, AccessPath sink,
																		Stmt sinkStmt, Collection<ISourceSinkDefinition> sourceDefinitions, AccessPath source, Stmt sourceStmt, Object userData,
																		List<Stmt> propagationPath, List<AccessPath> propagationAccessPath, List<Stmt> propagationCallSites,
																		InfoflowManager manager) {
		// We create a sink info out of a source definition as in backwards analysis the
		// start(=source def) is a sink

		if (propagationCallSites != null)
			Collections.reverse(propagationCallSites);
		if (propagationPath != null) {
			Collections.reverse(propagationPath);
			if (!manager.getConfig().getPathAgnosticResults() && propagationCallSites != null && manager != null) {
				for (int i = 0; i < propagationPath.size(); i++) {
					if (manager.getICFG().isExitStmt(propagationPath.get(i)))
						propagationPath.set(i, propagationCallSites.get(i));
				}
			}
		}
		if (propagationAccessPath != null)
			Collections.reverse(propagationAccessPath);

		Collection<Pair<ResultSourceInfo, ResultSinkInfo>> resultPairs = new HashSet<>(sinkDefinitions.size() * sourceDefinitions.size());
		for (ISourceSinkDefinition sourceDefinition : sourceDefinitions) {
			for (ISourceSinkDefinition sinkDefinition : sinkDefinitions) {
				ResultSinkInfo sourceObj = new ResultSinkInfo(sourceDefinition, source, sourceStmt);
				ResultSourceInfo sinkObj = new ResultSourceInfo(sinkDefinition, sink, sinkStmt, userData, propagationPath,
						propagationAccessPath, propagationCallSites, pathAgnosticResults);

				this.addResult(sourceObj, sinkObj);
				resultPairs.add(new Pair<>(sinkObj, sourceObj));
			}
		}
		return resultPairs;
	}

	@Override
	public void addResult(Collection<ISourceSinkDefinition> sinkDefinitions, AccessPath sink, Stmt sinkStmt,
						  Collection<ISourceSinkDefinition> sourceDefinitions, AccessPath source, Stmt sourceStmt) {
		for (ISourceSinkDefinition sourceDefinition : sourceDefinitions) {
			for (ISourceSinkDefinition sinkDefinition : sinkDefinitions) {
				// We create a sink info out of a source definition as in backwards analysis the
				// start(=source def) is a sink
				this.addResult(new ResultSinkInfo(sourceDefinition, sink, sinkStmt),
						new ResultSourceInfo(sinkDefinition, source, sourceStmt, pathAgnosticResults));
			}
		}
	}
}
