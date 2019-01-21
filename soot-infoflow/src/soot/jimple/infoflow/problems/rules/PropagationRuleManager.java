package soot.jimple.infoflow.problems.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Manager class for all propagation rules
 * 
 * @author Steven Arzt
 *
 */
public class PropagationRuleManager {

	public static int ABSTRACTION_ACTIVATED = 0x01;

	protected final InfoflowManager manager;
	protected final TaintAbstraction zeroValue;
	protected final TaintPropagationResults results;
	protected final ITaintPropagationRule[] rules;

	public PropagationRuleManager(InfoflowManager manager, TaintAbstraction zeroValue,
			TaintPropagationResults results) {
		this.manager = manager;
		this.zeroValue = zeroValue;
		this.results = results;

		List<ITaintPropagationRule> ruleList = new ArrayList<>();

		ruleList.add(new SourcePropagationRule(manager, zeroValue, results));
		ruleList.add(new SinkPropagationRule(manager, zeroValue, results));
		ruleList.add(new StaticPropagationRule(manager, zeroValue, results));

		if (manager.getConfig().getEnableArrayTracking())
			ruleList.add(new ArrayPropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getEnableExceptionTracking())
			ruleList.add(new ExceptionPropagationRule(manager, zeroValue, results));
		if (manager.getTaintWrapper() != null)
			ruleList.add(new WrapperPropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
			ruleList.add(new ImplicitPropagtionRule(manager, zeroValue, results));
		ruleList.add(new StrongUpdatePropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getEnableTypeChecking())
			ruleList.add(new TypingPropagationRule(manager, zeroValue, results));
		ruleList.add(new SkipSystemClassRule(manager, zeroValue, results));
		if (manager.getConfig().getStopAfterFirstKFlows() > 0)
			ruleList.add(new StopAfterFirstKFlowsPropagationRule(manager, zeroValue, results));

		this.rules = ruleList.toArray(new ITaintPropagationRule[ruleList.size()]);
	}

	/**
	 * Applies all rules to the normal flow function
	 * 
	 * @param state    The IFDS solver state
	 * @param destStmt The next statement to which control flow will continue after
	 *                 processing stmt
	 * @return The collection of outgoing taints
	 */
	public Set<AbstractDataFlowAbstraction> applyNormalFlowFunction(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt) {
		return applyNormalFlowFunction(state, destStmt, null, null, 0);
	}

	/**
	 * Applies all rules to the normal flow function
	 * 
	 * @param state      The IFDS solver state
	 * @param destStmt   The next statement to which control flow will continue
	 *                   after processing stmt
	 * @param killSource Outgoing value for the rule to indicate whether the
	 *                   incoming taint abstraction shall be killed
	 * @param killAll    Outgoing value that receives whether all taints shall be
	 *                   killed and nothing shall be propagated onwards
	 * @param flags      Optional flags that inform the rule manager about the
	 *                   status of the given taint abstraction or the data flow
	 *                   analysis as a whole
	 * @return The collection of outgoing taints
	 */
	public Set<AbstractDataFlowAbstraction> applyNormalFlowFunction(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		Set<AbstractDataFlowAbstraction> res = null;
		if (killSource == null)
			killSource = new ByReferenceBoolean();
		for (ITaintPropagationRule rule : rules) {
			Collection<AbstractDataFlowAbstraction> ruleOut = rule.propagateNormalFlow(state, destStmt, killSource,
					killAll, flags);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}

		// Do we need to retain the source value?
		if ((killAll == null || !killAll.value) && !killSource.value) {
			if (res == null) {
				res = new HashSet<>();
				res.add(state.getTargetVal());
			} else
				res.add(state.getTargetVal());
		}
		return res;
	}

	/**
	 * Propagates a flow across a call site
	 * 
	 * @param state   The IFDS solver state
	 * @param dest    The destination method into which to propagate the abstraction
	 * @param killAll Outgoing value for the rule to specify whether all taints
	 *                shall be killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Set<AbstractDataFlowAbstraction> applyCallFlowFunction(SolverState<Unit, AbstractDataFlowAbstraction> state,
			SootMethod dest, ByReferenceBoolean killAll) {
		Set<AbstractDataFlowAbstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<AbstractDataFlowAbstraction> ruleOut = rule.propagateCallFlow(state, dest, killAll);
			if (killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}
		return res;
	}

	/**
	 * Applies all rules to the call-to-return flow function
	 * 
	 * @param state The IFDS solver state
	 * @return The collection of outgoing taints
	 */
	public Set<AbstractDataFlowAbstraction> applyCallToReturnFlowFunction(
			SolverState<Unit, AbstractDataFlowAbstraction> state) {
		return applyCallToReturnFlowFunction(state, new ByReferenceBoolean(), null, false);
	}

	/**
	 * Applies all rules to the call-to-return flow function
	 * 
	 * @param state      The IFDS solver state
	 * @param killSource Outgoing value for the rule to indicate whether the
	 *                   incoming taint abstraction shall be killed
	 * @return The collection of outgoing taints
	 */
	public Set<AbstractDataFlowAbstraction> applyCallToReturnFlowFunction(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, boolean noAddSource) {
		Set<AbstractDataFlowAbstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<AbstractDataFlowAbstraction> ruleOut = rule.propagateCallToReturnFlow(state, killSource,
					killAll);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}

		// Do we need to retain the source value?
		if (!noAddSource && !killSource.value) {
			if (res == null) {
				res = new HashSet<>();
				res.add(state.getTargetVal());
			} else
				res.add(state.getTargetVal());
		}
		return res;
	}

	/**
	 * Applies all rules to the return flow function
	 * 
	 * @param callerD1s The context abstraction at the caller side
	 * @param source    The incoming taint to propagate over the given statement
	 * @param stmt      The statement to which to apply the rules
	 * @param retSite   The return site to which the execution returns after leaving
	 *                  the current method
	 * @param callSite  The call site of the call from which we return
	 * @param killAll   Outgoing value for the rule to specify whether all taints
	 *                  shall be killed, i.e., nothing shall be propagated
	 * @return The collection of outgoing taints
	 */
	public Set<AbstractDataFlowAbstraction> applyReturnFlowFunction(Collection<AbstractDataFlowAbstraction> callerD1s,
			TaintAbstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		Set<AbstractDataFlowAbstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<AbstractDataFlowAbstraction> ruleOut = rule.propagateReturnFlow(callerD1s, source, stmt, retSite,
					callSite, killAll);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}
		return res;
	}

	/**
	 * Gets the array of rules registered in this manager object
	 * 
	 * @return The array of rules registered in this manager object
	 */
	public ITaintPropagationRule[] getRules() {
		return rules;
	}

}
