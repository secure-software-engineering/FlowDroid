package soot.jimple.infoflow.problems.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Manager class for all propagation rules
 * 
 * @author Steven Arzt
 *
 */
public class PropagationRuleManager {

	protected final InfoflowManager manager;
	protected final Abstraction zeroValue;
	protected final TaintPropagationResults results;
	protected final ITaintPropagationRule[] rules;

	public PropagationRuleManager(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
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
	 * @param d1
	 *            The context abstraction
	 * @param source
	 *            The incoming taint to propagate over the given statement
	 * @param stmt
	 *            The statement to which to apply the rules
	 * @param destStmt
	 *            The next statement to which control flow will continue after
	 *            processing stmt
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyNormalFlowFunction(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt) {
		return applyNormalFlowFunction(d1, source, stmt, destStmt, null, null);
	}

	/**
	 * Applies all rules to the normal flow function
	 * 
	 * @param d1
	 *            The context abstraction
	 * @param source
	 *            The incoming taint to propagate over the given statement
	 * @param stmt
	 *            The statement to which to apply the rules
	 * @param destStmt
	 *            The next statement to which control flow will continue after
	 *            processing stmt
	 * @param killSource
	 *            Outgoing value for the rule to indicate whether the incoming taint
	 *            abstraction shall be killed
	 * @param killAll
	 *            Outgoing value that receives whether all taints shall be killed
	 *            and nothing shall be propagated onwards
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyNormalFlowFunction(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		Set<Abstraction> res = null;
		if (killSource == null)
			killSource = new ByReferenceBoolean();
		for (ITaintPropagationRule rule : rules) {
			Collection<Abstraction> ruleOut = rule.propagateNormalFlow(d1, source, stmt, destStmt, killSource, killAll);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<Abstraction>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}

		// Do we need to retain the source value?
		if ((killAll == null || !killAll.value) && !killSource.value) {
			if (res == null) {
				res = new HashSet<>();
				res.add(source);
			} else
				res.add(source);
		}
		return res;
	}

	/**
	 * Propagates a flow across a call site
	 * 
	 * @param d1
	 *            The context abstraction
	 * @param source
	 *            The abstraction to propagate over the statement
	 * @param stmt
	 *            The statement at which to propagate the abstraction
	 * @param dest
	 *            The destination method into which to propagate the abstraction
	 * @param killAll
	 *            Outgoing value for the rule to specify whether all taints shall be
	 *            killed, i.e., nothing shall be propagated
	 * @return The new abstractions to be propagated to the next statement
	 */
	public Set<Abstraction> applyCallFlowFunction(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		Set<Abstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<Abstraction> ruleOut = rule.propagateCallFlow(d1, source, stmt, dest, killAll);
			if (killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<Abstraction>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}
		return res;
	}

	/**
	 * Applies all rules to the call-to-return flow function
	 * 
	 * @param d1
	 *            The context abstraction
	 * @param source
	 *            The incoming taint to propagate over the given statement
	 * @param stmt
	 *            The statement to which to apply the rules
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyCallToReturnFlowFunction(Abstraction d1, Abstraction source, Stmt stmt) {
		return applyCallToReturnFlowFunction(d1, source, stmt, new ByReferenceBoolean(), null, false);
	}

	/**
	 * Applies all rules to the call-to-return flow function
	 * 
	 * @param d1
	 *            The context abstraction
	 * @param source
	 *            The incoming taint to propagate over the given statement
	 * @param stmt
	 *            The statement to which to apply the rules
	 * @param killSource
	 *            Outgoing value for the rule to indicate whether the incoming taint
	 *            abstraction shall be killed
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyCallToReturnFlowFunction(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll, boolean noAddSource) {
		Set<Abstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<Abstraction> ruleOut = rule.propagateCallToReturnFlow(d1, source, stmt, killSource, killAll);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<Abstraction>(ruleOut);
				else
					res.addAll(ruleOut);
			}
		}

		// Do we need to retain the source value?
		if (!noAddSource && !killSource.value) {
			if (res == null) {
				res = new HashSet<>();
				res.add(source);
			} else
				res.add(source);
		}
		return res;
	}

	/**
	 * Applies all rules to the return flow function
	 * 
	 * @param callerD1s
	 *            The context abstraction at the caller side
	 * @param source
	 *            The incoming taint to propagate over the given statement
	 * @param stmt
	 *            The statement to which to apply the rules
	 * @param retSite
	 *            The return site to which the execution returns after leaving the
	 *            current method
	 * @param callSite
	 *            The call site of the call from which we return
	 * @param killAll
	 *            Outgoing value for the rule to specify whether all taints shall be
	 *            killed, i.e., nothing shall be propagated
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyReturnFlowFunction(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		Set<Abstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<Abstraction> ruleOut = rule.propagateReturnFlow(callerD1s, source, stmt, retSite, callSite,
					killAll);
			if (killAll != null && killAll.value)
				return null;
			if (ruleOut != null && !ruleOut.isEmpty()) {
				if (res == null)
					res = new HashSet<Abstraction>(ruleOut);
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
