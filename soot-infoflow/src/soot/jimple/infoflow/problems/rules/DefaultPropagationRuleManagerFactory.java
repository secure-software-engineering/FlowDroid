package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

/**
 * Default implementation of the {@link IPropagationRuleManagerFactory} class
 * 
 * @author Steven Arzt
 *
 */
public class DefaultPropagationRuleManagerFactory implements IPropagationRuleManagerFactory {

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		return new PropagationRuleManager(manager, zeroValue, results);
	}

}
