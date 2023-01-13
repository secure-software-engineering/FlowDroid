package soot.jimple.infoflow.river;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Interface for SourcePropagationRules that support complex flows
 * 
 * @author Tim Lange
 */
public interface IComplexFlowSourcePropagationRule {
	/**
	 * Processes a secondary flow taint reaching complex sink.
	 * 
	 * @param d1     The calling context
	 * @param source The current taint abstraction
	 * @param stmt   The current statement, which is assumed to be a sink
	 */
	void processComplexFlowSource(Abstraction d1, Abstraction source, Stmt stmt);
}
