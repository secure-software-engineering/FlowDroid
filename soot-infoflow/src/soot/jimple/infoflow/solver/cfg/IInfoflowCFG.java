/**
 * (c) 2013 Tata Consultancy Services & Ecole Polytechnique de Montreal
 * All rights reserved
 */
package soot.jimple.infoflow.solver.cfg;

import java.util.Collection;
import java.util.List;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public interface IInfoflowCFG extends BiDiInterproceduralCFG<Unit, SootMethod> {

	/**
	 * Abstraction of a postdominator. This is normally a unit. In cases in which a
	 * statement does not have a postdominator, we record the statement's containing
	 * method and say that the postdominator is reached when the method is left.
	 * This class MUST be immutable.
	 *
	 * @author Steven Arzt
	 */
	public static class UnitContainer {

		private final Unit unit;
		private final SootMethod method;

		public UnitContainer(Unit u) {
			unit = u;
			method = null;
		}

		public UnitContainer(SootMethod sm) {
			unit = null;
			method = sm;
		}

		@Override
		public int hashCode() {
			return 31 * (unit == null ? 0 : unit.hashCode()) + 31 * (method == null ? 0 : method.hashCode());
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof UnitContainer))
				return false;
			UnitContainer container = (UnitContainer) other;
			if (this.unit == null) {
				if (container.unit != null)
					return false;
			} else if (!this.unit.equals(container.unit))
				return false;
			if (this.method == null) {
				if (container.method != null)
					return false;
			} else if (!this.method.equals(container.method))
				return false;

			assert this.hashCode() == container.hashCode();
			return true;
		}

		public Unit getUnit() {
			return unit;
		}

		public SootMethod getMethod() {
			return method;
		}

		@Override
		public String toString() {
			if (method != null)
				return "(Method) " + method.toString();
			else
				return "(Unit) " + unit.toString();
		}

	}

	/**
	 * Gets the postdominator of the given unit. If this unit is a conditional, the
	 * postdominator is the join point behind both branches of the conditional.
	 * 
	 * @param u
	 *            The unit for which to get the postdominator.
	 * @return The postdominator of the given unit
	 */
	public UnitContainer getPostdominatorOf(Unit u);

	/**
	 * Gets the dominator of the given unit. If this unit is a conditional, the
	 * dominator is the join point above both branches of the conditional.
	 *
	 * @param u
	 *            The unit for which to get the postdominator.
	 * @return The postdominator of the given unit
	 */
	public UnitContainer getDominatorOf(Unit u);

	/**
	 * Get all dominators of the given unit.
	 *
	 * @param u The unit for which tot get the dominators
	 * @return The dominators of the given unit
	 */
	public List<Unit> getAllDominators(Unit u);

	/**
	 * Reconstructs if the unit is inside a conditional.
	 * Needed for implicit backwards on an unbalanced return into a method.
	 *
	 * @param callSite The unit to start the search at
	 * @return The same-level conditional
	 */
	public List<Unit> getConditionalBranchIntraprocedural(Unit callSite);

	/**
	 * Reconstructs the conditionals a taint could possibly reach.
	 * Needed for implicit backward flows to model sink calls inside a callee
	 *
	 * @param unit start unit, possibly a sink
	 * @return List of units with the context appended
	 */
	public List<Unit> getConditionalBranchesInterprocedural(Unit unit);

	/**
	 * Checks whether the given static field is read inside the given method or one
	 * of its transitive callees.
	 *
	 * @param method
	 *            The method to check
	 * @param variable
	 *            The static field to check
	 * @return True if the given static field is read inside the given method,
	 *         otherwise false
	 */
	public boolean isStaticFieldRead(SootMethod method, SootField variable);

	/**
	 * Checks whether the given static field is used (read or written) inside the
	 * given method or one of its transitive callees.
	 * 
	 * @param method
	 *            The method to check
	 * @param variable
	 *            The static field to check
	 * @return True if the given static field is used inside the given method,
	 *         otherwise false
	 */
	public boolean isStaticFieldUsed(SootMethod method, SootField variable);

	/**
	 * Checks whether the given method or any of its transitive callees has side
	 * effects
	 * 
	 * @param method
	 *            The method to check
	 * @return True if the given method or one of its transitive callees has side
	 *         effects, otherwise false
	 */
	public boolean hasSideEffects(SootMethod method);

	/**
	 * Re-initializes the mapping between statements and owning methods after a
	 * method has changed.
	 * 
	 * @param m
	 *            The method for which to re-initialize the mapping
	 */
	public void notifyMethodChanged(SootMethod m);

	/**
	 * Initializes the mapping between statements and owning body.
	 *
	 * @param b The body for which to initialize the mapping
	 */
	public void notifyNewBody(Body b);

	/**
	 * Checks whether the given method reads the given value
	 *
	 * @param m
	 *            The method to check
	 * @param v
	 *            The value to check
	 * @return True if the given method reads the given value, otherwise false
	 */
	public boolean methodReadsValue(SootMethod m, Value v);

	/**
	 * Checks whether the given method writes the given value
	 * 
	 * @param m
	 *            The method to check
	 * @param v
	 *            The value to check
	 * @return True if the given method writes the given value, otherwise false
	 */
	public boolean methodWritesValue(SootMethod m, Value v);

	/**
	 * Gets whether the two given units are connected by an exceptional control flow
	 * edge
	 * 
	 * @param u1
	 *            The first unit
	 * @param u2
	 *            The second unit
	 * @return True if the two given units are directly connected by an exceptional
	 *         control flow edge, otherwise false
	 */
	public boolean isExceptionalEdgeBetween(Unit u1, Unit u2);

	/**
	 * Gets all ordinary callees of the call at call site u, i.e., those that are
	 * not \<clinit\> or a fake edge.
	 * 
	 * @param u
	 *            The call site
	 * @return The set of ordinary callees of the given call site
	 */
	public Collection<SootMethod> getOrdinaryCalleesOfCallAt(Unit u);

	/**
	 * Checks whether the given call is a call to Executor.execute() or
	 * AccessController.doPrivileged() and whether the callee matches the expected
	 * method signature
	 * 
	 * @param ie
	 *            The invocation expression to check
	 * @param dest
	 *            The callee of the given invocation expression
	 * @return True if the given invocation expression and callee are a valid call
	 *         to Executor.execute() or AccessController.doPrivileged()
	 */
	public boolean isExecutorExecute(InvokeExpr ie, SootMethod dest);

	/**
	 * Checks whether the given call site is a reflective method call
	 * 
	 * @param u
	 *            The call site to check
	 * @return True if the given call site contains a reflective method call,
	 *         otherwise false
	 */
	public boolean isReflectiveCallSite(Unit u);

	/**
	 * Checks whether the given call site is a reflective method call
	 * 
	 * @param iexpr
	 *            The call site to check
	 * @return True if the given call site contains a reflective method call,
	 *         otherwise false
	 */
	public boolean isReflectiveCallSite(InvokeExpr iexpr);

	/**
	 * Clears all caches and temporary data from memory. This method has no effect
	 * on the functional behavior of the class.
	 */
	public void purge();
}
