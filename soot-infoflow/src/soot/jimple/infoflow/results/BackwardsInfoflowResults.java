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

import com.google.common.collect.Streams;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

import javax.sound.sampled.Line;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * For backwards analysis, we need to swap the sources and sinks to hide
 * the fact that we used sources as sinks and vice versa.
 * 
 * @author Tim Lange
 */
public class BackwardsInfoflowResults extends InfoflowResults {
	private static final Logger logger = LoggerFactory.getLogger(BackwardsInfoflowResults.class);

	@Override
	public Pair<ResultSourceInfo, ResultSinkInfo> addResult(ISourceSinkDefinition sinkDefinition, AccessPath sink,
															Stmt sinkStmt, ISourceSinkDefinition sourceDefinition, AccessPath source, Stmt sourceStmt, Object userData,
															List<Stmt> propagationPath, List<AccessPath> propagationAccessPath, List<Stmt> propagationCallSites,
															InfoflowManager manager) {
		// We create a sink info out of a source definition as in backwards analysis the start(=source def) is a sink
		ResultSinkInfo sourceObj = new ResultSinkInfo(sourceDefinition, source, sourceStmt);

		if (propagationCallSites != null)
			Collections.reverse(propagationCallSites);
		if (propagationPath != null) {
			Collections.reverse(propagationPath);
			if (!InfoflowConfiguration.getPathAgnosticResults() && propagationCallSites != null && manager != null) {
				for (int i = 0; i < propagationPath.size(); i++) {
					if (manager.getICFG().isExitStmt(propagationPath.get(i)))
						propagationPath.set(i, propagationCallSites.get(i));
				}
			}
		}
		if (propagationAccessPath != null)
			Collections.reverse(propagationAccessPath);
		ResultSourceInfo sinkObj = new ResultSourceInfo(sinkDefinition, sink, sinkStmt, userData, propagationPath, propagationAccessPath, propagationCallSites);

		this.addResult(sourceObj, sinkObj);
		return new Pair<>(sinkObj, sourceObj);
	}

	@Override
	public void addResult(ISourceSinkDefinition sinkDefinition, AccessPath sink, Stmt sinkStmt,
						  ISourceSinkDefinition sourceDefinition, AccessPath source, Stmt sourceStmt) {
		// We create a sink info out of a source definition as in backwards analysis the start(=source def) is a sink
		this.addResult(new ResultSinkInfo(sourceDefinition, sink, sinkStmt),
				new ResultSourceInfo(sinkDefinition, source, sourceStmt));
	}
}
