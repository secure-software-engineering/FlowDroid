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
package soot.jimple.infoflow.problems;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.cfg.FlowDroidEssentialMethodTag;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler.FlowFunctionType;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * abstract super class which - concentrates functionality used by
 * InfoflowProblem and BackwardsInfoflowProblem - contains helper functions
 * which should not pollute the naturally large InfofflowProblems
 *
 */
public abstract class AbstractInfoflowProblem
		extends DefaultJimpleIFDSTabulationProblem<Abstraction, BiDiInterproceduralCFG<Unit, SootMethod>> {

	protected final InfoflowManager manager;

	protected final Map<Unit, Set<Abstraction>> initialSeeds = new HashMap<Unit, Set<Abstraction>>();
	protected ITaintPropagationWrapper taintWrapper;
	protected INativeCallHandler ncHandler;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Abstraction zeroValue = null;

	protected IInfoflowSolver solver = null;

	protected TaintPropagationHandler taintPropagationHandler = null;

	private MyConcurrentHashMap<Unit, Set<Unit>> activationUnitsToCallSites = new MyConcurrentHashMap<Unit, Set<Unit>>();

	public AbstractInfoflowProblem(InfoflowManager manager) {
		super(manager.getICFG());
		this.manager = manager;
	}

	public void setSolver(IInfoflowSolver solver) {
		this.solver = solver;
	}

	public void setZeroValue(Abstraction zeroValue) {
		this.zeroValue = zeroValue;
	}

	/**
	 * we need this option as we start directly at the sources, but need to go
	 * backward in the call stack
	 */
	@Override
	public boolean followReturnsPastSeeds() {
		return true;
	}

	/**
	 * Sets the taint wrapper that shall be used for applying external library
	 * models
	 * 
	 * @param wrapper The taint wrapper that shall be used for applying external
	 *                library models
	 */
	public void setTaintWrapper(ITaintPropagationWrapper wrapper) {
		this.taintWrapper = wrapper;
	}

	/**
	 * Sets the handler class to be used for modeling the effects of native methods
	 * on the taint state
	 * 
	 * @param handler The native call handler to use
	 */
	public void setNativeCallHandler(INativeCallHandler handler) {
		this.ncHandler = handler;
	}

	/**
	 * Gets whether the given method is an entry point, i.e. one of the initial
	 * seeds belongs to the given method
	 * 
	 * @param sm The method to check
	 * @return True if the given method is an entry point, otherwise false
	 */
	protected boolean isInitialMethod(SootMethod sm) {
		for (Unit u : this.initialSeeds.keySet())
			if (interproceduralCFG().getMethodOf(u) == sm)
				return true;
		return false;
	}

	@Override
	public Map<Unit, Set<Abstraction>> initialSeeds() {
		return initialSeeds;
	}

	/**
	 * performance improvement: since we start directly at the sources, we do not
	 * need to generate additional taints unconditionally
	 */
	@Override
	public boolean autoAddZero() {
		return false;
	}

	protected boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
		if (!manager.getConfig().getFlowSensitiveAliasing())
			return false;

		if (activationUnit == null)
			return false;
		Set<Unit> callSites = activationUnitsToCallSites.get(activationUnit);
		return (callSites != null && callSites.contains(callSite));
	}

	protected boolean registerActivationCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs) {
		if (!manager.getConfig().getFlowSensitiveAliasing())
			return false;
		Unit activationUnit = activationAbs.getActivationUnit();
		if (activationUnit == null)
			return false;

		Set<Unit> callSites = activationUnitsToCallSites.putIfAbsentElseGet(activationUnit,
				new ConcurrentHashSet<Unit>());
		if (callSites.contains(callSite))
			return false;

		if (!activationAbs.isAbstractionActive())
			if (!callee.getActiveBody().getUnits().contains(activationUnit)) {
				boolean found = false;
				for (Unit au : callSites)
					if (callee.getActiveBody().getUnits().contains(au)) {
						found = true;
						break;
					}
				if (!found)
					return false;
			}

		return callSites.add(callSite);
	}

	public void setActivationUnitsToCallSites(AbstractInfoflowProblem other) {
		this.activationUnitsToCallSites = other.activationUnitsToCallSites;
	}

	@Override
	public IInfoflowCFG interproceduralCFG() {
		return (IInfoflowCFG) super.interproceduralCFG();
	}

	/**
	 * Adds the given initial seeds to the information flow problem
	 * 
	 * @param unit  The unit to be considered as a seed
	 * @param seeds The abstractions with which to start at the given seed
	 */
	public void addInitialSeeds(Unit unit, Set<Abstraction> seeds) {
		if (this.initialSeeds.containsKey(unit))
			this.initialSeeds.get(unit).addAll(seeds);
		else
			this.initialSeeds.put(unit, new HashSet<Abstraction>(seeds));
	}

	/**
	 * Gets whether this information flow problem has initial seeds
	 * 
	 * @return True if this information flow problem has initial seeds, otherwise
	 *         false
	 */
	public boolean hasInitialSeeds() {
		return !this.initialSeeds.isEmpty();
	}

	/**
	 * Gets the initial seeds with which this information flow problem has been
	 * configured
	 * 
	 * @return The initial seeds with which this information flow problem has been
	 *         configured.
	 */
	public Map<Unit, Set<Abstraction>> getInitialSeeds() {
		return this.initialSeeds;
	}

	/**
	 * Sets a handler which is invoked whenever a taint is propagated
	 * 
	 * @param handler The handler to be invoked when propagating taints
	 */
	public void setTaintPropagationHandler(TaintPropagationHandler handler) {
		this.taintPropagationHandler = handler;
	}

	@Override
	public Abstraction createZeroValue() {
		if (zeroValue == null)
			zeroValue = Abstraction.getZeroAbstraction(manager.getConfig().getFlowSensitiveAliasing());
		return zeroValue;
	}

	protected Abstraction getZeroValue() {
		return zeroValue;
	}

	/**
	 * Checks whether the given unit is the start of an exception handler
	 * 
	 * @param u The unit to check
	 * @return True if the given unit is the start of an exception handler,
	 *         otherwise false
	 */
	protected boolean isExceptionHandler(Unit u) {
		if (u instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) u;
			return defStmt.getRightOp() instanceof CaughtExceptionRef;
		}
		return false;
	}

	/**
	 * Notifies the outbound flow handlers, if any, about the computed result
	 * abstractions for the current flow function
	 * 
	 * @param d1           The abstraction at the beginning of the method
	 * @param stmt         The statement that has just been processed
	 * @param incoming     The incoming abstraction from which the outbound ones
	 *                     were computed
	 * @param outgoing     The outbound abstractions to be propagated on
	 * @param functionType The type of flow function that was computed
	 * @return The outbound flow abstracions, potentially changed by the flow
	 *         handlers
	 */
	protected Set<Abstraction> notifyOutFlowHandlers(Unit stmt, Abstraction d1, Abstraction incoming,
			Set<Abstraction> outgoing, FlowFunctionType functionType) {
		if (taintPropagationHandler != null && outgoing != null && !outgoing.isEmpty())
			outgoing = taintPropagationHandler.notifyFlowOut(stmt, d1, incoming, outgoing, manager, functionType);
		return outgoing;
	}

	@Override
	public boolean computeValues() {
		return false;
	}

	public InfoflowManager getManager() {
		return this.manager;
	}

	/**
	 * Checks whether the given method is excluded from the data flow analysis,
	 * i.e., should not be analyzed
	 * 
	 * @param sm The method to check
	 * @return True if the method is excluded and shall not be analyzed, otherwise
	 *         false
	 */
	protected boolean isExcluded(SootMethod sm) {
		// Is this an essential method?
		if (sm.hasTag(FlowDroidEssentialMethodTag.TAG_NAME))
			return false;

		// We can exclude Soot library classes
		if (manager.getConfig().getExcludeSootLibraryClasses() && sm.getDeclaringClass().isLibraryClass())
			return true;

		// We can ignore system classes according to FlowDroid's definition
		if (manager.getConfig().getIgnoreFlowsInSystemPackages()
				&& SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
			return true;

		return false;
	}

}
