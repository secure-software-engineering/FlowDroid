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
package soot.jimple.infoflow.nativeCallHandler;

import java.util.Set;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * The NativeCallHandler defines the taint propagation behavior for native code,
 * as we cannot analyze these methods
 */
public interface INativeCallHandler {

	/**
	 * This method is called before the taint propagation is started to give the native
	 * call handler the chance to initialize required components once Soot is running,
	 * but before it is queried for the first time.
	 * 
	 * Note that this method is guaranteed to be called only once and only by a single thread.
	 * @param manager The manager object providing access to the data flow solver processing
	 * the IFDS edges and the interprocedural control flow graph
	 */
	public void initialize(InfoflowManager manager);
	
	/**
	 * Returns the set of tainted values for a given call to native code, a
	 * given tainted value and the list of passed arguments
	 * @param call the statement which contains the call to the native code
	 * @param source the incoming taint value
	 * @param params list of arguments
	 * @return the resulting set of taints
	 */
	public abstract Set<Abstraction> getTaintedValues(Stmt call, Abstraction source, Value[] params);

	/**
	 * Checks whether this handler is able to handle the given call, i.e., has
	 * an explicit model for it
	 * @param call The call site to check
	 * @return True if this native call handler has an explicit model for the
	 * given call site, otherwise false
	 */
	public abstract boolean supportsCall(Stmt call);

	/**
	 * Tells the native call handler that it can free all resources it has allocated at initialization time
	 */
	public void shutdown();
	
}
