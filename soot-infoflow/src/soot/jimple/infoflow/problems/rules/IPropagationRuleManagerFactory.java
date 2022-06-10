package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

/**
 * Interface for constructing propagation rule managers
 * 
 * @author Steven Arzt
 *
 */
public interface IPropagationRuleManagerFactory {

	/**
	 * Creates a propagation rule manager
	 * 
	 * @param manager
	 *            The data flow manager for linking the various components of the
	 *            data flow analyzer
	 * @param zeroValue
	 *            The zero abstraction (the IFDS tautology)
	 * @param results
	 *            The container that receives all detected flows
	 * @return The new propagation rule manager
	 */
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
					TaintPropagationResults results);

}
