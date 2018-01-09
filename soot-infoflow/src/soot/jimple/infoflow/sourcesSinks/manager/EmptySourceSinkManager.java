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

import soot.SootMethod;

/**
 * A {@link ISourceSinkManager} that always returns false, i.e. one for which
 * there are no sources or sinks at all.
 * 
 * @author Steven Arzt
 */
public class EmptySourceSinkManager extends MethodBasedSourceSinkManager {

	public EmptySourceSinkManager() {
		//
	}

	@Override
	public SourceInfo getSourceMethodInfo(SootMethod sMethod) {
		return null;
	}

	@Override
	public SinkInfo getSinkMethodInfo(SootMethod sMethod) {
		return null;
	}

	@Override
	public void initialize() {
		//
	}

}
