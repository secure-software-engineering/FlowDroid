package soot.jimple.infoflow.river;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

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
	 *
	 * @param d1     The calling context
	 * @param source The current taint abstraction
	 * @param stmt   The current statement, which is assumed to be a sink
	 * @param def	 The definition of the source sink.
	 */
	void processSecondaryFlowSink(Abstraction d1, Abstraction source, Stmt stmt, ISourceSinkDefinition def);
}
