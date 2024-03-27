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
package soot.jimple.infoflow.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.collect.AtomicBitSet;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * The abstraction class contains all information that is necessary to track the
 * taint.
 * 
 * @author Steven Arzt
 * @author Christian Fritz
 */
public class Abstraction implements Cloneable, FastSolverLinkedNode<Abstraction, Unit> {

	protected static boolean flowSensitiveAliasing = true;

	/**
	 * the access path contains the currently tainted variable or field
	 */
	protected AccessPath accessPath;

	protected Abstraction predecessor = null;
	protected volatile Set<Abstraction> neighbors = null;
	protected Stmt currentStmt = null;
	protected Stmt correspondingCallSite = null;

	protected SourceContext sourceContext = null;

	/**
	 * Unit/Stmt which activates the taint when the abstraction passes it
	 */
	protected Unit activationUnit = null;
	/**
	 * Unit/Stmt which indicates it origin; tells the aliasing to turn around in
	 * backwards analysis
	 */
	protected Unit turnUnit = null;
	/**
	 * taint is thrown by an exception (is set to false when it reaches the
	 * catch-Stmt)
	 */
	protected boolean exceptionThrown = false;
	protected int hashCode = 0;
	protected int neighborHashCode = 0;

	/**
	 * The postdominators we need to pass in order to leave the current conditional
	 * branch. Do not use the synchronized Stack class here to avoid deadlocks.
	 */
	protected List<UnitContainer> postdominators = null;
	protected Unit dominator = null;
	protected boolean isImplicit = false;

	/**
	 * Only valid for inactive abstractions. Specifies whether an access paths has
	 * been cut during alias analysis.
	 */
	protected boolean dependsOnCutAP = false;

	protected AtomicBitSet pathFlags = null;
	protected int propagationPathLength = 0;

	public Abstraction(Collection<ISourceSinkDefinition> definitions, AccessPath sourceVal, Stmt sourceStmt,
			Object userData, boolean exceptionThrown, boolean isImplicit) {
		this(sourceVal, new SourceContext(definitions, sourceVal, sourceStmt, userData), exceptionThrown, isImplicit);
	}

	Abstraction(AccessPath apToTaint, SourceContext sourceContext, boolean exceptionThrown, boolean isImplicit) {
		this.sourceContext = sourceContext;
		this.accessPath = apToTaint;
		this.activationUnit = null;
		this.turnUnit = null;
		this.exceptionThrown = exceptionThrown;

		this.neighbors = null;
		this.isImplicit = isImplicit;
		this.currentStmt = sourceContext == null ? null : sourceContext.getStmt();
	}

	/**
	 * Creates an abstraction as a copy of an existing abstraction, only exchanging
	 * the access path. -> only used by AbstractionWithPath
	 * 
	 * @param p        The access path for the new abstraction
	 * @param original The original abstraction to copy
	 */
	protected Abstraction(AccessPath p, Abstraction original) {
		if (original == null) {
			sourceContext = null;
			exceptionThrown = false;
			activationUnit = null;
			turnUnit = null;
			isImplicit = false;
		} else {
			sourceContext = original.sourceContext;
			exceptionThrown = original.exceptionThrown;
			activationUnit = original.activationUnit;
			turnUnit = original.turnUnit;
			assert activationUnit == null || flowSensitiveAliasing;

			postdominators = original.postdominators == null ? null
					: new ArrayList<UnitContainer>(original.postdominators);
			dominator = original.dominator;

			dependsOnCutAP = original.dependsOnCutAP;
			isImplicit = original.isImplicit;
		}
		accessPath = p;
		neighbors = null;
		currentStmt = null;
	}

	/**
	 * Initializes the configuration for building new abstractions
	 * 
	 * @param config The configuration of the data flow solver
	 */
	public static void initialize(InfoflowConfiguration config) {
		flowSensitiveAliasing = config.getFlowSensitiveAliasing();
	}

	public Abstraction deriveInactiveAbstraction(Stmt activationUnit) {
		if (!flowSensitiveAliasing) {
			assert this.isAbstractionActive();
			return this;
		}

		// If this abstraction is already inactive, we keep it
		if (!this.isAbstractionActive())
			return this;

		Abstraction a = deriveNewAbstractionMutable(accessPath, null);
		if (a == null)
			return null;

		a.postdominators = null;
		a.dominator = null;
		a.activationUnit = activationUnit;
		a.dependsOnCutAP |= a.getAccessPath().isCutOffApproximation();
		return a;
	}

	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt) {
		return deriveNewAbstraction(p, currentStmt, isImplicit);
	}

	public Abstraction deriveDefinitelyNewAbstraction(AccessPath p, Stmt currentStmt) {
		return deriveDefinitelyNewAbstraction(p, currentStmt, isImplicit);
	}

	public Abstraction deriveDefinitelyNewAbstraction(AccessPath p, Stmt currentStmt, boolean isImplicit) {
		Abstraction abs = deriveNewAbstractionMutable(p, currentStmt);
		if (abs == null)
			return null;

		abs.isImplicit = isImplicit;
		return abs;
	}

	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt, boolean isImplicit) {
		// If the new abstraction looks exactly like the current one, there is
		// no need to create a new object
		if (this.accessPath.equals(p) && this.currentStmt == currentStmt && this.isImplicit == isImplicit)
			return this;

		return deriveDefinitelyNewAbstraction(p, currentStmt, isImplicit);
	}

	protected Abstraction deriveNewAbstractionMutable(AccessPath p, Stmt currentStmt) {
		// An abstraction needs an access path
		if (p == null)
			return null;

		if (this.accessPath.equals(p) && this.currentStmt == currentStmt) {
			Abstraction abs = clone();
			abs.currentStmt = currentStmt;
			return abs;
		}

		Abstraction abs = new Abstraction(p, this);
		abs.predecessor = this;
		abs.currentStmt = currentStmt;
		abs.propagationPathLength = propagationPathLength + 1;

		if (!abs.getAccessPath().isEmpty())
			abs.postdominators = null;
		if (!abs.isAbstractionActive())
			abs.dependsOnCutAP = abs.dependsOnCutAP || p.isCutOffApproximation();

		abs.sourceContext = null;
		return abs;
	}

	/**
	 * Derives a new abstraction that models the current local being thrown as an
	 * exception
	 * 
	 * @param throwStmt The statement at which the exception was thrown
	 * @return The newly derived abstraction
	 */
	public Abstraction deriveNewAbstractionOnThrow(Stmt throwStmt) {
		Abstraction abs = clone();

		abs.currentStmt = throwStmt;
		abs.sourceContext = null;
		abs.exceptionThrown = true;
		return abs;
	}

	/**
	 * Derives a new abstraction that models the current local being caught as an
	 * exception
	 * 
	 * @param ap The access path in which the tainted exception is stored
	 * @return The newly derived abstraction
	 */
	public Abstraction deriveNewAbstractionOnCatch(AccessPath ap) {
		assert this.exceptionThrown;
		Abstraction abs = deriveNewAbstractionMutable(ap, null);
		if (abs == null)
			return null;

		abs.exceptionThrown = false;
		return abs;
	}

	public boolean isAbstractionActive() {
		return activationUnit == null;
	}

	public boolean isImplicit() {
		return isImplicit;
	}

	@Override
	public String toString() {
		return (isAbstractionActive() ? "" : "_") + accessPath.toString() + " | "
				+ (turnUnit != null || activationUnit == null ? "" : activationUnit.toString())
				+ (turnUnit == null ? "" : turnUnit.toString()) + ">>";
	}

	public AccessPath getAccessPath() {
		return accessPath;
	}

	public Unit getActivationUnit() {
		return this.activationUnit;
	}

	public Unit getTurnUnit() {
		return this.turnUnit;
	}

	public Abstraction deriveNewAbstractionWithTurnUnit(Unit turnUnit) {
		if (this.turnUnit == turnUnit)
			return this;

		Abstraction a = clone();
		a.sourceContext = null;
		a.activationUnit = null;
		a.turnUnit = turnUnit;
		return a;
	}

	public Abstraction getActiveCopy() {
		if (this.isAbstractionActive())
			return this;

		Abstraction a = clone();
		a.sourceContext = null;
		a.activationUnit = null;
		return a;
	}

	/**
	 * Gets whether this value has been thrown as an exception
	 * 
	 * @return True if this value has been thrown as an exception, otherwise false
	 */
	public boolean getExceptionThrown() {
		return this.exceptionThrown;
	}

	public Abstraction deriveConditionalAbstractionEnter(UnitContainer postdom, Stmt conditionalUnit) {
		assert this.isAbstractionActive();

		if (postdominators != null && postdominators.contains(postdom))
			return this;

		Abstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), conditionalUnit);
		if (abs == null)
			return null;

		if (abs.postdominators == null)
			abs.postdominators = Collections.singletonList(postdom);
		else
			abs.postdominators.add(0, postdom);
		return abs;
	}

	public Abstraction deriveConditionalAbstractionCall(Unit conditionalCallSite) {
		assert this.isAbstractionActive();
		assert conditionalCallSite != null;

		Abstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), (Stmt) conditionalCallSite);
		if (abs == null)
			return null;

		// Postdominators are only kept intraprocedurally in order to not
		// mess up the summary functions with caller-side information
		abs.postdominators = null;

		return abs;
	}

	public Abstraction dropTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return this;

		Abstraction abs = clone();
		abs.sourceContext = null;
		abs.postdominators.remove(0);
		return abs;
	}

	public UnitContainer getTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return null;
		return this.postdominators.get(0);
	}

	public boolean isTopPostdominator(Unit u) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getUnit() == u;
	}

	public boolean isTopPostdominator(SootMethod sm) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getMethod() == sm;
	}

	public Abstraction deriveNewAbstractionWithDominator(Unit dominator, Stmt stmt) {
		if (this.dominator != null)
			return this;

		Abstraction abs = deriveNewAbstractionMutable(accessPath, stmt);
		if (abs == null)
			return null;

		abs.setDominator(dominator);
		return abs;
	}

	public Abstraction deriveNewAbstractionWithDominator(Unit dominator) {
		return deriveNewAbstractionWithDominator(dominator, null);
	}

	public Abstraction deriveConditionalUpdate(Stmt stmt) {
		return deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), stmt);
	}

	public Abstraction deriveCondition(AccessPath ap, Stmt stmt) {
		Abstraction abs = deriveNewAbstractionMutable(ap, stmt);
		if (abs == null)
			return null;
		abs.turnUnit = stmt;
		abs.dominator = null;
		return abs;
	}

	public Abstraction removeDominator(Stmt stmt) {
		Abstraction abs = deriveNewAbstraction(accessPath, stmt);
		if (abs == null)
			return null;
		abs.setDominator(null);
		return abs;
	}

	public void setDominator(Unit dominator) {
		this.dominator = dominator;
	}

	public Unit getDominator() {
		return this.dominator;
	}

	public boolean isDominator(Unit u) {
		if (dominator == null)
			return false;
		return dominator == u;
	}

	@Override
	public Abstraction clone() {
		Abstraction abs = new Abstraction(accessPath, this);
		abs.predecessor = this;
		abs.neighbors = null;
		abs.currentStmt = null;
		abs.correspondingCallSite = null;
		abs.propagationPathLength = propagationPathLength + 1;
		assert abs.equals(this);
		return abs;
	}

	@Override
	public Abstraction clone(Unit currentUnit, Unit callSite) {
		Abstraction abs = new Abstraction(accessPath, this);
		abs.predecessor = this;
		abs.neighbors = null;
		abs.currentStmt = (Stmt) currentUnit;
		abs.correspondingCallSite = (Stmt) callSite;
		abs.propagationPathLength = propagationPathLength + 1;
		assert abs.equals(this);
		return abs;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Abstraction other = (Abstraction) obj;

		// If we have already computed hash codes, we can use them for
		// comparison
		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;

		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;

		return localEquals(other);
	}

	public boolean equalsWithoutContext(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Abstraction other = (Abstraction) obj;

		// If we have already computed hash codes, we can use them for
		// comparison
		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;

		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;

		return localEquals(other);
	}

	/**
	 * Checks whether this object locally equals the given object, i.e. the both are
	 * equal modulo the access path
	 * 
	 * @param other The object to compare this object with
	 * @return True if this object is locally equal to the given one, otherwise
	 *         false
	 */
	public boolean localEquals(Abstraction other) {
		// deliberately ignore prevAbs
		if (sourceContext == null) {
			if (other.sourceContext != null)
				return false;
		} else if (!sourceContext.equals(other.sourceContext))
			return false;
		if (activationUnit == null) {
			if (other.activationUnit != null)
				return false;
		} else if (!activationUnit.equals(other.activationUnit))
			return false;
		if (turnUnit == null) {
			if (other.turnUnit != null)
				return false;
		} else if (!turnUnit.equals(other.turnUnit))
			return false;
		if (this.exceptionThrown != other.exceptionThrown)
			return false;
		if (postdominators == null) {
			if (other.postdominators != null)
				return false;
		} else if (!postdominators.equals(other.postdominators))
			return false;
		if (dominator == null) {
			if (other.dominator != null)
				return false;
		} else if (!dominator.equals(other.dominator))
			return false;
		if (this.dependsOnCutAP != other.dependsOnCutAP)
			return false;
		if (this.isImplicit != other.isImplicit)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		if (this.hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;

		// deliberately ignore prevAbs
		result = prime * result + ((sourceContext == null) ? 0 : sourceContext.hashCode());
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((activationUnit == null) ? 0 : activationUnit.hashCode());
		result = prime * result + ((turnUnit == null) ? 0 : turnUnit.hashCode());
		result = prime * result + (exceptionThrown ? 1231 : 1237);
		result = prime * result + ((postdominators == null) ? 0 : postdominators.hashCode());
		result = prime * result + ((dominator == null) ? 0 : dominator.hashCode());
		result = prime * result + (dependsOnCutAP ? 1231 : 1237);
		result = prime * result + (isImplicit ? 1231 : 1237);
		this.hashCode = result;

		return this.hashCode;
	}

	/**
	 * Checks whether this abstraction entails the given abstraction, i.e. this
	 * taint also taints everything that is tainted by the given taint.
	 * 
	 * @param other The other taint abstraction
	 * @return True if this object at least taints everything that is also tainted
	 *         by the given object
	 */
	public boolean entails(Abstraction other) {
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.entails(other.accessPath))
			return false;
		return localEquals(other);
	}

	/**
	 * Gets the context of the taint, i.e. the statement and value of the source
	 * 
	 * @return The statement and value of the source
	 */
	public SourceContext getSourceContext() {
		return sourceContext;
	}

	public boolean dependsOnCutAP() {
		return dependsOnCutAP;
	}

	@Override
	public Abstraction getPredecessor() {
		return this.predecessor;
	}

	public Set<Abstraction> getNeighbors() {
		return this.neighbors;
	}

	public Stmt getCurrentStmt() {
		return this.currentStmt;
	}

	@Override
	public boolean addNeighbor(Abstraction originalAbstraction) {
		// We should not register ourselves as a neighbor
		if (originalAbstraction == this)
			return false;

		synchronized (this) {
			if (this.neighbors == null)
				this.neighbors = Sets.newIdentityHashSet();
			return this.neighbors.add(originalAbstraction);
		}
	}

	public void setCorrespondingCallSite(Stmt callSite) {
		this.correspondingCallSite = callSite;
	}

	public Stmt getCorrespondingCallSite() {
		return this.correspondingCallSite;
	}

	public static Abstraction getZeroAbstraction(boolean flowSensitiveAliasing) {
		Abstraction zeroValue = new Abstraction(AccessPath.getZeroAccessPath(), null, false, false);
		Abstraction.flowSensitiveAliasing = flowSensitiveAliasing;
		return zeroValue;
	}

	@Override
	public void setPredecessor(Abstraction predecessor) {
		this.predecessor = predecessor;
		assert this.predecessor != this;

		this.neighborHashCode = 0;
	}

	/**
	 * Only use this method if you really need to fake a source context and know
	 * what you are doing.
	 * 
	 * @param sourceContext The new source context
	 */
	public void setSourceContext(SourceContext sourceContext) {
		this.sourceContext = sourceContext;
		this.hashCode = 0;
		this.neighborHashCode = 0;
	}

	/**
	 * Registers that a worker thread with the given ID has already processed this
	 * abstraction
	 * 
	 * @param id The ID of the worker thread
	 * @return True if the worker thread with the given ID has not been registered
	 *         before, otherwise false
	 */
	public boolean registerPathFlag(int id, int maxSize) {
		if (pathFlags == null || pathFlags.getLargestInt() < maxSize) {
			synchronized (this) {
				if (pathFlags == null) {
					// Make sure that the field is set only after the
					// constructor
					// is done and the object is fully usable
					AtomicBitSet pf = new AtomicBitSet(maxSize);
					pathFlags = pf;
				} else if (pathFlags.getLargestInt() < maxSize) {
					AtomicBitSet pf = new AtomicBitSet(maxSize);
					for (int i = 0; i < pathFlags.size(); i++) {
						if (pathFlags.get(i))
							pf.set(i);
					}
					pathFlags = pf;
				}
			}
		}
		return pathFlags.set(id);
	}

	public Abstraction injectSourceContext(SourceContext sourceContext) {
		if (this.sourceContext != null && this.sourceContext.equals(sourceContext))
			return this;

		Abstraction abs = clone();
		abs.predecessor = null;
		abs.neighbors = null;
		abs.sourceContext = sourceContext;
		abs.currentStmt = this.currentStmt;
		return abs;
	}

	/**
	 * For internal use by memory manager only
	 */
	void setAccessPath(AccessPath accessPath) {
		this.accessPath = accessPath;
		this.hashCode = 0;
		this.neighborHashCode = 0;
	}

	void setCurrentStmt(Stmt currentStmt) {
		this.currentStmt = currentStmt;
	}

	@Override
	public int getNeighborCount() {
		return neighbors == null ? 0 : neighbors.size();
	}

	@Override
	public int getPathLength() {
		return propagationPathLength;
	}

}
