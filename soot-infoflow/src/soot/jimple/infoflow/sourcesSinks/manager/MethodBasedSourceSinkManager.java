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
package soot.jimple.infoflow.sourcesSinks.manager;

import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Abstracts from the very generic statement-based SourceSinkManager so that
 * users can conveniently work on the called methods instead of having to
 * analyze the call statement every time
 * 
 * @author Steven Arzt
 *
 */
public abstract class MethodBasedSourceSinkManager implements ISourceSinkManager {

	public abstract SourceInfo getSourceMethodInfo(SootMethod method);

	public abstract SinkInfo getSinkMethodInfo(SootMethod method);

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		assert sCallSite != null;
		if (!sCallSite.containsInvokeExpr())
			return null;
		return getSourceMethodInfo(sCallSite.getInvokeExpr().getMethod());
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		assert sCallSite != null;
		if (!sCallSite.containsInvokeExpr())
			return null;
		return getSinkMethodInfo(sCallSite.getInvokeExpr().getMethod());
	}

	protected boolean typeIsString(Type type) {
		return type instanceof RefType && ((RefType) type).getSootClass().getName().equals("java.lang.String");
	}

}
