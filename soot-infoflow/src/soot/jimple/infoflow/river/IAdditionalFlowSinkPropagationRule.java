package soot.jimple.infoflow.river;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Interface for Backward SourcePropagationRules that support conditional flows.
 * Note that in Secondary Flows the "source" is the sink of the primary flow and the
 * "sinks" are the statements reached in between
 * 
 * @author Tim Lange
 */
public interface IAdditionalFlowSinkPropagationRule {
	/**
	 * Records a secondary flow taint reaching a statement.
	 * Important: This method does not check whether the secondary flow should be recorded.
	 * 
	 * @param d1     The calling context
	 * @param source The current taint abstraction
	 * @param stmt   The current statement, which is assumed to be a sink
	 */
	void processSecondaryFlowSink(Abstraction d1, Abstraction source, Stmt stmt);
}
