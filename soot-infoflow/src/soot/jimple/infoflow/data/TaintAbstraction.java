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
import java.util.Collections;
import java.util.List;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

/**
 * The abstraction class contains all information that is necessary to track the
 * taint.
 * 
 * @author Steven Arzt
 * @author Christian Fritz
 */
public class TaintAbstraction extends AbstractDataFlowAbstraction {

	protected static boolean flowSensitiveAliasing = true;
	protected static boolean keepStatements = true;
	protected static boolean propagateSourceContext = false;

	/**
	 * the access path contains the currently tainted variable or field
	 */
	protected AccessPath accessPath;

	protected Stmt currentStmt = null;

	/**
	 * Unit/Stmt which activates the taint when the abstraction passes it
	 */
	protected Unit activationUnit = null;
	/**
	 * taint is thrown by an exception (is set to false when it reaches the
	 * catch-Stmt)
	 */
	protected boolean exceptionThrown = false;
	protected int hashCode = 0;

	/**
	 * The postdominators we need to pass in order to leave the current conditional
	 * branch. Do not use the synchronized Stack class here to avoid deadlocks.
	 */
	protected List<UnitContainer> postdominators = null;
	protected boolean isImplicit = false;

	/**
	 * Only valid for inactive abstractions. Specifies whether an access paths has
	 * been cut during alias analysis.
	 */
	protected boolean dependsOnCutAP = false;

	protected int propagationPathLength = 0;

	public TaintAbstraction(SourceSinkDefinition definition, AccessPath sourceVal, Stmt sourceStmt, Object userData,
			boolean exceptionThrown, boolean isImplicit) {
		this(sourceVal, new SourceContext(definition, sourceVal, sourceStmt, userData), exceptionThrown, isImplicit);
	}

	protected TaintAbstraction(AccessPath apToTaint, SourceContext sourceContext, boolean exceptionThrown,
			boolean isImplicit) {
		this.sourceContext = sourceContext;
		this.accessPath = apToTaint;
		this.activationUnit = null;
		this.exceptionThrown = exceptionThrown;

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
	protected TaintAbstraction(AccessPath p, TaintAbstraction original) {
		if (original == null) {
			sourceContext = null;
			exceptionThrown = false;
			activationUnit = null;
			isImplicit = false;
		} else {
			sourceContext = original.sourceContext;
			exceptionThrown = original.exceptionThrown;
			activationUnit = original.activationUnit;
			assert activationUnit == null || flowSensitiveAliasing;

			postdominators = original.postdominators == null ? null
					: new ArrayList<UnitContainer>(original.postdominators);

			dependsOnCutAP = original.dependsOnCutAP;
			isImplicit = original.isImplicit;
		}
		accessPath = p;
		currentStmt = null;
	}

	/**
	 * Initializes the configuration for building new abstractions
	 * 
	 * @param config The configuration of the data flow solver
	 */
	public static void initialize(InfoflowConfiguration config) {
		flowSensitiveAliasing = config.getFlowSensitiveAliasing();
		keepStatements = config.getPathConfiguration().mustKeepStatements();
	}

	public final TaintAbstraction deriveInactiveAbstraction(Stmt activationUnit) {
		if (!flowSensitiveAliasing) {
			assert this.isAbstractionActive();
			return this;
		}

		// If this abstraction is already inactive, we keep it
		if (!this.isAbstractionActive())
			return this;

		TaintAbstraction a = deriveNewAbstractionMutable(accessPath, null);
		if (a == null)
			return null;

		a.postdominators = null;
		a.activationUnit = activationUnit;
		a.dependsOnCutAP |= a.getAccessPath().isCutOffApproximation();
		return a;
	}

	public TaintAbstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt) {
		return deriveNewAbstraction(p, currentStmt, isImplicit);
	}

	public TaintAbstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt, boolean isImplicit) {
		// If the new abstraction looks exactly like the current one, there is
		// no need to create a new object
		if (this.accessPath.equals(p) && this.currentStmt == currentStmt && this.isImplicit == isImplicit)
			return this;

		TaintAbstraction abs = deriveNewAbstractionMutable(p, currentStmt);
		if (abs == null)
			return null;

		abs.isImplicit = isImplicit;
		return abs;
	}

	private TaintAbstraction _predecessor;

	private TaintAbstraction deriveNewAbstractionMutable(AccessPath p, Stmt currentStmt) {
		// An abstraction needs an access path
		if (p == null)
			return null;

		if (this.accessPath.equals(p) && this.currentStmt == currentStmt) {
			TaintAbstraction abs = clone();
			abs.currentStmt = currentStmt;
			return abs;
		}

		TaintAbstraction abs = new TaintAbstraction(p, this);
		abs.currentStmt = currentStmt;
		abs.propagationPathLength = propagationPathLength + 1;

		if (!abs.getAccessPath().isEmpty())
			abs.postdominators = null;
		if (!abs.isAbstractionActive())
			abs.dependsOnCutAP = abs.dependsOnCutAP || p.isCutOffApproximation();

		if (!propagateSourceContext)
			abs.sourceContext = null;

		abs._predecessor = this;

		return abs;
	}

	public TaintAbstraction get_predecessor() {
		return _predecessor;
	}

	/**
	 * Derives a new abstraction that models the current local being thrown as an
	 * exception
	 * 
	 * @param throwStmt The statement at which the exception was thrown
	 * @return The newly derived abstraction
	 */
	public final TaintAbstraction deriveNewAbstractionOnThrow(Stmt throwStmt) {
		TaintAbstraction abs = clone();

		abs.currentStmt = throwStmt;
		if (!propagateSourceContext)
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
	public final TaintAbstraction deriveNewAbstractionOnCatch(AccessPath ap) {
		assert this.exceptionThrown;
		TaintAbstraction abs = deriveNewAbstractionMutable(ap, null);
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
				+ (activationUnit == null ? "" : activationUnit.toString()) + ">>";
	}

	public AccessPath getAccessPath() {
		return accessPath;
	}

	public Unit getActivationUnit() {
		return this.activationUnit;
	}

	/**
	 * If this abstraction supports alias analysis, this returns the active copy of
	 * the current abstraction. Otherwise, "this" is returned.
	 * 
	 * @return The active copy if supported, otherwise the "this" reference
	 */
	public TaintAbstraction getActiveCopy() {
		if (this.isAbstractionActive())
			return this;

		TaintAbstraction a = clone();
		if (!propagateSourceContext)
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

	public final TaintAbstraction deriveConditionalAbstractionEnter(UnitContainer postdom, Stmt conditionalUnit) {
		assert this.isAbstractionActive();

		if (postdominators != null && postdominators.contains(postdom))
			return this;

		TaintAbstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), conditionalUnit);
		if (abs == null)
			return null;

		if (abs.postdominators == null)
			abs.postdominators = Collections.singletonList(postdom);
		else
			abs.postdominators.add(0, postdom);
		return abs;
	}

	public final TaintAbstraction deriveConditionalAbstractionCall(Unit conditionalCallSite) {
		assert this.isAbstractionActive();
		assert conditionalCallSite != null;

		TaintAbstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), (Stmt) conditionalCallSite);
		if (abs == null)
			return null;

		// Postdominators are only kept intraprocedurally in order to not
		// mess up the summary functions with caller-side information
		abs.postdominators = null;

		return abs;
	}

	public final TaintAbstraction dropTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return this;

		TaintAbstraction abs = clone();
		if (!propagateSourceContext)
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

	@Override
	public TaintAbstraction clone() {
		TaintAbstraction abs = new TaintAbstraction(accessPath, this);
		abs.currentStmt = null;
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
		TaintAbstraction other = (TaintAbstraction) obj;

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
	private boolean localEquals(TaintAbstraction other) {
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
		if (this.exceptionThrown != other.exceptionThrown)
			return false;
		if (postdominators == null) {
			if (other.postdominators != null)
				return false;
		} else if (!postdominators.equals(other.postdominators))
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
		result = prime * result + (exceptionThrown ? 1231 : 1237);
		result = prime * result + ((postdominators == null) ? 0 : postdominators.hashCode());
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
	public boolean entails(TaintAbstraction other) {
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.entails(other.accessPath))
			return false;
		return localEquals(other);
	}

	public boolean dependsOnCutAP() {
		return dependsOnCutAP;
	}

	public Stmt getCurrentStmt() {
		return this.currentStmt;
	}

	public static TaintAbstraction getZeroAbstraction(boolean flowSensitiveAliasing) {
		TaintAbstraction zeroValue = new TaintAbstraction(AccessPath.getZeroAccessPath(), null, false, false);
		TaintAbstraction.flowSensitiveAliasing = flowSensitiveAliasing;
		return zeroValue;
	}

	public TaintAbstraction injectSourceContext(SourceContext sourceContext) {
		if (this.sourceContext != null && this.sourceContext.equals(sourceContext))
			return this;

		TaintAbstraction abs = clone();
		abs.sourceContext = sourceContext;
		abs.currentStmt = this.currentStmt;
		return abs;
	}

	/**
	 * For internal use by memory manager only
	 */
	void setAccessPath(AccessPath accessPath) {
		this.accessPath = accessPath;
	}

	void setCurrentStmt(Stmt currentStmt) {
		this.currentStmt = currentStmt;
	}

	@Override
	public int getPathLength() {
		return propagationPathLength;
	}

	@Override
	public TaintAbstraction reduce() {
		TaintAbstraction abs = clone();
		abs.currentStmt = currentStmt;
		abs.sourceContext = null;
		return abs;
	}

	public static void setPropagateSourceContext(boolean propagateSourceContext) {
		TaintAbstraction.propagateSourceContext = propagateSourceContext;
	}

}
