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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.cfg.FlowDroidEssentialMethodTag;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler.FlowFunctionType;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * abstract super class which - concentrates functionality used by
 * InfoflowProblem and AliasProblem - contains helper functions which should not
 * pollute the naturally large InfofflowProblems
 *
 */
public abstract class AbstractInfoflowProblem
		extends DefaultJimpleIFDSTabulationProblem<Abstraction, IInfoflowCFG> {

	protected final InfoflowManager manager;

	protected final Map<Unit, Set<Abstraction>> initialSeeds = new HashMap<Unit, Set<Abstraction>>();
	protected ITaintPropagationWrapper taintWrapper;
	protected INativeCallHandler ncHandler;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Abstraction zeroValue = null;

	protected IInfoflowSolver solver = null;

	protected TaintPropagationHandler taintPropagationHandler = null;

	private static class CallSite {
		public Set<Unit> callsites = new ConcurrentHashSet<>();
		public SoftReference<Set<SootMethod>> callsiteMethods = new SoftReference<>(new ConcurrentHashSet<>());

		public boolean addCallsite(Unit callSite, IInfoflowCFG icfg) {
			if (callsites.add(callSite)) {
				Set<SootMethod> c = callsiteMethods.get();
				if (c == null) {
					c = new ConcurrentHashSet<>();
					callsiteMethods = new SoftReference<>(c);
				}
				c.add(icfg.getMethodOf(callSite));
				return true;
			}
			return false;
		}
	}

	private ConcurrentHashMap<Unit, CallSite> activationUnitsToCallSites = new ConcurrentHashMap<Unit, CallSite>();

	protected final PropagationRuleManager propagationRules;
	protected final TaintPropagationResults results;

	private static Function<? super Unit, ? extends CallSite> createNewCallSite = new Function<Unit, CallSite>() {

		@Override
		public CallSite apply(Unit t) {
			return new CallSite();
		}
	};

	public AbstractInfoflowProblem(InfoflowManager manager, Abstraction zeroValue,
			IPropagationRuleManagerFactory ruleManagerFactory) {
		super(manager.getICFG());
		this.manager = manager;
		this.zeroValue = zeroValue == null ? createZeroValue() : zeroValue;
		this.results = new TaintPropagationResults(manager);
		this.propagationRules = ruleManagerFactory.createRuleManager(manager, this.zeroValue, results);
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
		return manager.getConfig().getSolverConfiguration().isFollowReturnsPastSources();
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

	@Override
	public boolean autoAddZero() {
		return false;
	}

	public boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
		if (!manager.getConfig().getFlowSensitiveAliasing())
			return false;

		if (activationUnit == null)
			return false;
		CallSite callSites = activationUnitsToCallSites.get(activationUnit);
		if (callSites != null)
			return callSites.callsites.contains(callSite);
		return false;
	}

	protected boolean registerActivationCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs) {
		if (!manager.getConfig().getFlowSensitiveAliasing())
			return false;
		Unit activationUnit = activationAbs.getActivationUnit();
		if (activationUnit == null)
			return false;

		CallSite callSites = activationUnitsToCallSites.computeIfAbsent(activationUnit, createNewCallSite);
		if (callSites.callsites.contains(callSite))
			return false;

		IInfoflowCFG icfg = (IInfoflowCFG) super.interproceduralCFG();
		if (!activationAbs.isAbstractionActive()) {
			if (!callee.getActiveBody().getUnits().contains(activationUnit)) {
				Set<SootMethod> cm = callSites.callsiteMethods.get();
				if (cm != null) {
					if (!cm.contains(callee))
						return false;
				} else {
					cm = new HashSet<>();
					boolean found = false;
					for (Unit au : callSites.callsites) {
						cm.add(icfg.getMethodOf(au));
						if (callee.getActiveBody().getUnits().contains(au)) {
							found = true;
							break;
						}
					}
					callSites.callsiteMethods = new SoftReference<Set<SootMethod>>(cm);
					if (!found)
						return false;
				}
			}
		}

		return callSites.addCallsite(callSite, icfg);
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

	/**
	 * Gets the taint propagation handler
	 *
	 * @return taint propagation handler
	 */
	public TaintPropagationHandler getTaintPropagationHandler() {
		return this.taintPropagationHandler;
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
		if (manager.getConfig().getExcludeSootLibraryClasses()) {
			SootClass declClass = sm.getDeclaringClass();
			if (declClass != null && declClass.isLibraryClass())
				return true;
		}

		// We can ignore system classes according to FlowDroid's definition
		if (manager.getConfig().getIgnoreFlowsInSystemPackages()) {
			SootClass declClass = sm.getDeclaringClass();
			if (declClass != null && SystemClassHandler.v().isClassInSystemPackage(declClass))
				return true;
		}

		if (sm.isConcrete() && !sm.hasActiveBody())
			return true;

		return false;
	}

	/**
	 * Gets the results of the data flow analysis
	 */
	public TaintPropagationResults getResults() {
		return this.results;
	}

	/**
	 * Gets the rules that FlowDroid uses internally to conduct specific analysis
	 * tasks such as handling sources or sinks
	 * 
	 * @return The propagation rule manager
	 */
	public PropagationRuleManager getPropagationRules() {
		return propagationRules;
	}

	/**
	 * Checks whether the arguments of a given invoke expression
	 * has a reference to a given base object while ignoring the given index
	 * @param e the invoke expr
	 * @param actualBase the base to look for
	 * @param ignoreIndex the index to ignore
	 * @return true if there is another reference
	 */
	protected boolean hasAnotherReferenceOnBase(InvokeExpr e, Value actualBase, int ignoreIndex) {
		for (int i = 0; i < e.getArgCount(); i++) {
			if (i != ignoreIndex) {
				if (e.getArg(i) == actualBase)
					return true;
			}
		}
		return false;
	}
}
