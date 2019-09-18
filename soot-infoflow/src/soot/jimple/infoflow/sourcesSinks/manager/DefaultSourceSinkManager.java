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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * A {@link ISourceSinkManager} working on lists of source and sink methods
 * 
 * @author Steven Arzt
 */
public class DefaultSourceSinkManager implements ISourceSinkManager {

	protected Collection<String> sourceDefs;
	protected Collection<String> sinkDefs;

	private Collection<SootMethod> sources;
	private Collection<SootMethod> sinks;

	private Collection<String> returnTaintMethodDefs;
	private Collection<String> parameterTaintMethodDefs;

	private Collection<SootMethod> returnTaintMethods;
	private Collection<SootMethod> parameterTaintMethods;

	protected final LoadingCache<SootClass, Collection<SootClass>> interfacesOf = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {

				@Override
				public Collection<SootClass> load(SootClass sc) throws Exception {
					Set<SootClass> set = new HashSet<SootClass>(sc.getInterfaceCount());
					for (SootClass i : sc.getInterfaces()) {
						set.add(i);
						set.addAll(interfacesOf.getUnchecked(i));
					}
					SootClass superClass = sc.getSuperclassUnsafe();
					if (superClass != null)
						set.addAll(interfacesOf.getUnchecked(superClass));
					return set;
				}

			});

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources The list of methods to be treated as sources
	 * @param sinks   The list of methods to be treated as sins
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks) {
		this(sources, sinks, null, null);
	}

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources               The list of methods to be treated as sources
	 * @param sinks                 The list of methods to be treated as sinks
	 * @param parameterTaintMethods The list of methods whose parameters shall be
	 *                              regarded as sources
	 * @param returnTaintMethods    The list of methods whose return values shall be
	 *                              regarded as sinks
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks,
			Collection<String> parameterTaintMethods, Collection<String> returnTaintMethods) {
		this.sourceDefs = sources;
		this.sinkDefs = sinks;
		this.parameterTaintMethodDefs = (parameterTaintMethods != null) ? parameterTaintMethods : new HashSet<String>();
		this.returnTaintMethodDefs = (returnTaintMethods != null) ? returnTaintMethods : new HashSet<String>();
	}

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sourceSinkProvider The provider that defines source and sink methods
	 */
	public DefaultSourceSinkManager(ISourceSinkDefinitionProvider sourceSinkProvider) {
		this.sourceDefs = new HashSet<>();
		this.sinkDefs = new HashSet<>();

		// Load the sources
		for (ISourceSinkDefinition ssd : sourceSinkProvider.getSources()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				sourceDefs.add(mssd.getMethod().getSignature());
			}
		}

		// Load the sinks
		for (ISourceSinkDefinition ssd : sourceSinkProvider.getSinks()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				sinkDefs.add(mssd.getMethod().getSignature());
			}
		}
	}

	/**
	 * Sets the list of methods to be treated as sources
	 * 
	 * @param sources The list of methods to be treated as sources
	 */
	public void setSources(List<String> sources) {
		this.sourceDefs = sources;
	}

	/**
	 * Sets the list of methods to be treated as sinks
	 * 
	 * @param sinks The list of methods to be treated as sinks
	 */
	public void setSinks(List<String> sinks) {
		this.sinkDefs = sinks;
	}

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		SootMethod callee = sCallSite.containsInvokeExpr() ? sCallSite.getInvokeExpr().getMethod() : null;

		AccessPath targetAP = null;
		if (isSourceMethod(manager, sCallSite)) {
			if (callee.getReturnType() != null && sCallSite instanceof DefinitionStmt) {
				// Taint the return value
				Value leftOp = ((DefinitionStmt) sCallSite).getLeftOp();
				targetAP = manager.getAccessPathFactory().createAccessPath(leftOp, true);
			} else if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
				// Taint the base object
				Value base = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
				targetAP = manager.getAccessPathFactory().createAccessPath(base, true);
			}
		}
		// Check whether we need to taint parameters
		else if (sCallSite instanceof IdentityStmt) {
			IdentityStmt istmt = (IdentityStmt) sCallSite;
			if (istmt.getRightOp() instanceof ParameterRef) {
				ParameterRef pref = (ParameterRef) istmt.getRightOp();
				SootMethod currentMethod = manager.getICFG().getMethodOf(istmt);
				if (parameterTaintMethods != null && parameterTaintMethods.contains(currentMethod))
					targetAP = manager.getAccessPathFactory()
							.createAccessPath(currentMethod.getActiveBody().getParameterLocal(pref.getIndex()), true);
			}
		}

		if (targetAP == null)
			return null;

		// Create the source information data structure
		return new SourceInfo(callee == null ? null : new MethodSourceSinkDefinition(new SootMethodAndClass(callee)),
				targetAP);
	}

	/**
	 * Checks whether the given call sites invokes a source method
	 * 
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return True if the given call site invoked a source method, otherwise false
	 */
	protected boolean isSourceMethod(InfoflowManager manager, Stmt sCallSite) {
		// We only support method calls
		if (!sCallSite.containsInvokeExpr())
			return false;

		// Check for a direct match
		SootMethod callee = sCallSite.getInvokeExpr().getMethod();
		if (this.sources.contains(callee))
			return true;

		// Check whether we have any of the interfaces on the list
		String subSig = callee.getSubSignature();
		for (SootClass i : interfacesOf.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
			SootMethod sm = i.getMethodUnsafe(subSig);
			if (sm != null && this.sources.contains(sm))
				return true;
		}

		// Ask the CFG in case we don't know any better
		for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
			if (this.sources.contains(sm))
				return true;
		}

		// nothing found
		return false;
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		// Check whether values returned by the current method are to be
		// considered as sinks
		if (this.returnTaintMethods != null && sCallSite instanceof ReturnStmt) {
			SootMethod sm = manager.getICFG().getMethodOf(sCallSite);
			if (this.returnTaintMethods != null && this.returnTaintMethods.contains(sm))
				return new SinkInfo(new MethodSourceSinkDefinition(new SootMethodAndClass(sm)));
		}

		// Check whether the callee is a sink
		if (this.sinks != null && !sinks.isEmpty() && sCallSite.containsInvokeExpr()) {
			InvokeExpr iexpr = sCallSite.getInvokeExpr();

			// Is this method on the list?
			SootMethodAndClass smac = isSinkMethod(manager, sCallSite);
			if (smac != null) {
				// Check that the incoming taint is visible in the callee at all
				if (SystemClassHandler.v().isTaintVisible(ap, iexpr.getMethod())) {
					// If we don't have an access path, we can only
					// over-approximate
					if (ap == null)
						return new SinkInfo(new MethodSourceSinkDefinition(smac));

					// The given access path must at least be referenced
					// somewhere in the sink
					if (!ap.isStaticFieldRef()) {
						for (int i = 0; i < iexpr.getArgCount(); i++)
							if (iexpr.getArg(i) == ap.getPlainValue()) {
								if (ap.getTaintSubFields() || ap.isLocal())
									return new SinkInfo(new MethodSourceSinkDefinition(smac));
							}
						if (iexpr instanceof InstanceInvokeExpr)
							if (((InstanceInvokeExpr) iexpr).getBase() == ap.getPlainValue())
								return new SinkInfo(new MethodSourceSinkDefinition(smac));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks whether the given call sites invokes a sink method
	 * 
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return The method that was discovered as a sink, or null if no sink could be
	 *         found
	 */
	protected SootMethodAndClass isSinkMethod(InfoflowManager manager, Stmt sCallSite) {
		// Is the method directly in the sink set?
		SootMethod callee = sCallSite.getInvokeExpr().getMethod();
		if (this.sinks.contains(callee))
			return new SootMethodAndClass(callee);

		// Check whether we have any of the interfaces on the list
		String subSig = callee.getSubSignature();
		for (SootClass i : interfacesOf.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
			SootMethod sm = i.getMethodUnsafe(subSig);
			if (sm != null && this.sinks.contains(sm))
				return new SootMethodAndClass(sm);
		}

		// Ask the CFG in case we don't know any better
		for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
			if (this.sinks.contains(sm))
				return new SootMethodAndClass(sm);
		}

		// nothing found
		return null;
	}

	/**
	 * Sets the list of methods whose parameters shall be regarded as taint sources
	 * 
	 * @param parameterTaintMethods The list of methods whose parameters shall be
	 *                              regarded as taint sources
	 */
	public void setParameterTaintMethods(List<String> parameterTaintMethods) {
		this.parameterTaintMethodDefs = parameterTaintMethods;
	}

	/**
	 * Sets the list of methods whose return values shall be regarded as taint sinks
	 * 
	 * @param returnTaintMethods The list of methods whose return values shall be
	 *                           regarded as taint sinks
	 */
	public void setReturnTaintMethods(List<String> returnTaintMethods) {
		this.returnTaintMethodDefs = returnTaintMethods;
	}

	@Override
	public void initialize() {
		if (sourceDefs != null) {
			sources = new HashSet<>();
			for (String signature : sourceDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					sources.add(sm);
			}
			sourceDefs = null;
		}

		if (sinkDefs != null) {
			sinks = new HashSet<>();
			for (String signature : sinkDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					sinks.add(sm);
			}
			sinkDefs = null;
		}

		if (returnTaintMethodDefs != null) {
			returnTaintMethods = new HashSet<>();
			for (String signature : returnTaintMethodDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					returnTaintMethods.add(sm);
			}
			returnTaintMethodDefs = null;
		}

		if (parameterTaintMethodDefs != null) {
			parameterTaintMethods = new HashSet<>();
			for (String signature : parameterTaintMethodDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					parameterTaintMethods.add(sm);
			}
			parameterTaintMethodDefs = null;
		}
	}

}
