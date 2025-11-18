package soot.jimple.infoflow.problems.rules;

import java.util.Arrays;
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
	protected IArrayContextProvider arrayRule;

	public PropagationRuleManager(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results,
			ITaintPropagationRule[] rules) {
		this.manager = manager;
		this.zeroValue = zeroValue;
		this.results = results;
		this.rules = rules;

		if (rules != null) {
			for (ITaintPropagationRule rule : rules) {
				rule.init(manager, zeroValue, results);
				if (rule instanceof IArrayContextProvider) {
					arrayRule = (IArrayContextProvider) rule;
				}
			}
		}
		if (arrayRule == null)
			arrayRule = new DummyArrayContext();
	}

	public PropagationRuleManager(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results,
			Class<? extends ITaintPropagationRule>[] rules) {
		this(manager, zeroValue, results, instantiate(Arrays.asList(rules)));
	}

	public PropagationRuleManager(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results,
			List<Class<? extends ITaintPropagationRule>> rules) {
		this(manager, zeroValue, results, instantiate(rules));
	}

	private static ITaintPropagationRule[] instantiate(List<Class<? extends ITaintPropagationRule>> rules) {
		ITaintPropagationRule[] r = new ITaintPropagationRule[rules.size()];
		for (int i = 0; i < r.length; i++) {
			Class<? extends ITaintPropagationRule> ruleC = rules.get(i);
			r[i] = instantiateSingle(ruleC);
		}
		return r;
	}

	private static ITaintPropagationRule instantiateSingle(Class<? extends ITaintPropagationRule> ruleClass) {
		try {
			return ruleClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Could not instantiate rule %s", ruleClass.getName()), e);
		}
	}

	/**
	 * Swaps out an existing rule.
	 * @param oldRule the class of the old rule implementation
	 * @param newRule the class of the new implementation
	 */
	public void swapRule(Class<? extends ITaintPropagationRule> oldRule,
			Class<? extends ITaintPropagationRule> newRule) {
		swapRule(oldRule, instantiateSingle(newRule));
	}

	/**
	 * Swaps out an existing rule.
	 * @param oldRule the class of the old rule implementation
	 * @param newRule the new implementation
	 */
	public void swapRule(Class<? extends ITaintPropagationRule> oldRule, ITaintPropagationRule newRule) {
		if (rules == null)
			throw new IllegalStateException("No rules configured");

		for (int i = 0; i < rules.length; i++) {
			ITaintPropagationRule r = rules[i];
			if (r.getClass() == oldRule) {
				r.init(manager, zeroValue, results);
				if (r instanceof IArrayContextProvider) {
					arrayRule = (IArrayContextProvider) r;
				}
				rules[i] = newRule;
				return;
			}
		}
		throw new IllegalArgumentException(
				String.format("Could not find %s in the rules: %s", oldRule.getName(), Arrays.toString(rules)));
	}

	/**
	 * Applies all rules to the normal flow function
	 * 
	 * @param d1       The context abstraction
	 * @param source   The incoming taint to propagate over the given statement
	 * @param stmt     The statement to which to apply the rules
	 * @param destStmt The next statement to which control flow will continue after
	 *                 processing stmt
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyNormalFlowFunction(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt) {
		return applyNormalFlowFunction(d1, source, stmt, destStmt, null, null);
	}

	/**
	 * Applies all rules to the normal flow function
	 * 
	 * @param d1         The context abstraction
	 * @param source     The incoming taint to propagate over the given statement
	 * @param stmt       The statement to which to apply the rules
	 * @param destStmt   The next statement to which control flow will continue
	 *                   after processing stmt
	 * @param killSource Outgoing value for the rule to indicate whether the
	 *                   incoming taint abstraction shall be killed
	 * @param killAll    Outgoing value that receives whether all taints shall be
	 *                   killed and nothing shall be propagated onwards
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
			}
			res.add(source);
		}
		return res;
	}

	/**
	 * Propagates a flow across a call site
	 * 
	 * @param d1      The context abstraction
	 * @param source  The abstraction to propagate over the statement
	 * @param stmt    The statement at which to propagate the abstraction
	 * @param dest    The destination method into which to propagate the abstraction
	 * @param killAll Outgoing value for the rule to specify whether all taints
	 *                shall be killed, i.e., nothing shall be propagated
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
	 * @param d1     The context abstraction
	 * @param source The incoming taint to propagate over the given statement
	 * @param stmt   The statement to which to apply the rules
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyCallToReturnFlowFunction(Abstraction d1, Abstraction source, Stmt stmt) {
		return applyCallToReturnFlowFunction(d1, source, stmt, new ByReferenceBoolean(), null, false);
	}

	/**
	 * Applies all rules to the call-to-return flow function
	 * 
	 * @param d1         The context abstraction
	 * @param source     The incoming taint to propagate over the given statement
	 * @param stmt       The statement to which to apply the rules
	 * @param killSource Outgoing value for the rule to indicate whether the
	 *                   incoming taint abstraction shall be killed
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
			}
			res.add(source);
		}
		return res;
	}

	/**
	 * Applies all rules to the return flow function
	 * 
	 * @param callerD1s The context abstraction at the caller side
	 * @param calleeD1
	 * @param source    The incoming taint to propagate over the given statement
	 * @param stmt      The statement to which to apply the rules
	 * @param retSite   The return site to which the execution returns after leaving
	 *                  the current method
	 * @param callSite  The call site of the call from which we return
	 * @param killAll   Outgoing value for the rule to specify whether all taints
	 *                  shall be killed, i.e., nothing shall be propagated
	 * @return The collection of outgoing taints
	 */
	public Set<Abstraction> applyReturnFlowFunction(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		Set<Abstraction> res = null;
		for (ITaintPropagationRule rule : rules) {
			Collection<Abstraction> ruleOut = rule.propagateReturnFlow(callerD1s, calleeD1, source, stmt, retSite,
					callSite, killAll);
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

	public IArrayContextProvider getArrayContextProvider() {
		return arrayRule;
	}
}
