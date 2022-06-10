package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

/**
 * A factory that creates a rule manager factory without any rules
 * 
 * @author Steven Arzt
 *
 */
public class EmptyPropagationRuleManagerFactory implements IPropagationRuleManagerFactory {

	public static EmptyPropagationRuleManagerFactory INSTANCE = new EmptyPropagationRuleManagerFactory();

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		return IdentityPropagationRuleManager.INSTANCE;
	}

}
