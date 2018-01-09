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
package soot.jimple.infoflow.taintWrappers;

import java.util.Set;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * This interface declares methods to define classes and methods which should not
 * be analyzed directly. Analysis results are instead taken from an external model
 * containing method summaries (which improves performance and helps if the sources
 * are not available).
 * 
 * Unless stated otherwise, all methods in this interface must be implemented
 * thread safe.
 * 
 * @author Christian Fritz
 * @author Steven Arzt
 */
public interface ITaintPropagationWrapper {
	
	/**
	 * This method is called before the taint propagation is started to give the taint wrapper
	 * the chance to initialize required components once Soot is running, but before it is
	 * queried for the first time.
	 * 
	 * Note that this method is guaranteed to be called only once and only by a single thread.
	 * @param manager The manager object providing access to the data flow solver processing
	 * the IFDS edges and the interprocedural control flow graph
	 */
	public void initialize(InfoflowManager manager);
	
	/**
	 * Checks an invocation statement for black-box taint propagation. This allows
	 * the wrapper to artificially propagate taints over method invocations without
	 * requiring the analysis to look inside the method.
	 * @param stmt The invocation statement which to check for black-box taint propagation
	 * @param d1 The abstraction at the beginning of the method that calls the
	 * wrapped method
	 * @param taintedPath The tainted field or value to propagate
	 * @return The list of tainted values after the invocation statement referenced in {@link Stmt}
	 * has been executed
	 */
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, 
			Abstraction taintedPath);
	
	/**
	 * Gets whether the taints produced by this taint wrapper are exclusive, i.e. there are
	 * no other taints than those produced by the wrapper. In effect, this tells the analysis
	 * not to propagate inside the callee.
	 * @param stmt The call statement to check
	 * @param taintedPath The tainted field or value to propagate 
	 * @return True if this taint wrapper is exclusive, otherwise false. 
	 */
	public boolean isExclusive(Stmt stmt, Abstraction taintedPath);
	
	/**
	 * Gets the aliases that a summarized method generates for the given
	 * abstraction. Note that this is a may-alias problem.
	 * @param stmt The statement that calls the summarized method
	 * @param d1 The abstraction at the entry point of the method that calls the
	 * wrapped method
	 * @param taintedPath The abstraction for which the aliases shall be
	 * computed
	 * @return The set of aliases for the given abstraction or null if no such
	 * aliases exist
	 */
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1,
			Abstraction taintedPath);
	
	/**
	 * Checks whether this taint wrapper can in general produce artificial taints
	 * for the given callee. If an implementation returns "false" for a callee,
	 * all call sites for this callee might be removed if not needed elsewhere.
	 * @param method The method to check
	 * @return True if this taint wrapper can in general produce taints for the
	 * given method.
	 */
	public boolean supportsCallee(SootMethod method);
	
	/**
	 * Checks whether this taint wrapper can in general produce artificial taints
	 * for the given call site. If an implementation returns "false" for a call
	 * site, this call sites might be removed if not needed elsewhere.
	 * @param callSite The call site to check
	 * @return True if this taint wrapper can in general produce taints for the
	 * given call site.
	 */
	public boolean supportsCallee(Stmt callSite);
	
	/**
	 * Gets the number of times in which the taint wrapper was able to
	 * exclusively model a method call. This is equal to the number of times
	 * isExclusive() returned true.
	 * @return The number of method model requests that succeeded
	 */
	public int getWrapperHits();
	
	/**
	 * Gets the number of times in which the taint wrapper was NOT able to
	 * exclusively model a method call. This is equal to the number of times
	 * isExclusive() returned false.
	 * @return The number of method model requests that failed
	 */
	public int getWrapperMisses();

}
