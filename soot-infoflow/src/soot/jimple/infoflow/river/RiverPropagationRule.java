package soot.jimple.infoflow.river;

import java.util.Collection;
import java.util.Collections;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Contains river-specific code that i.e. adds dataflow results when secondary
 * sinks are reached.
 */
public class RiverPropagationRule extends AbstractTaintPropagationRule {

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	// Note: Do not get confused with on the terms source/sink. In the general case,
	// the backward
	// analysis starts the analysis at sinks and records results at the source. For
	// secondary flows,
	// the secondary source is equal to the primary sink and the secondary sink is
	// an interesting
	// statement (an additional flow condition or a usage context) at which we
	// record a result.
	// That's why the backward source rule is also the secondary flow sink rule. */
	/**
	 * Records a secondary flow taint reaching a statement. Important: This method
	 * does not check whether the secondary flow should be recorded.
	 * 
	 * @param d1     The calling context
	 * @param source The current taint abstraction
	 * @param stmt   The current statement, which is assumed to be a sink
	 */
	public void processSecondaryFlowSink(Abstraction d1, Abstraction source, Stmt stmt) {
		// Static fields are not part of the conditional flow model.
		if (!source.isAbstractionActive() || source.getAccessPath().isStaticFieldRef())
			return;

		// Only proceed if stmt could influence the taint
		if (!stmt.containsInvokeExpr() || !Utils.isTaintVisibleInCallee(stmt, source, getAliasing()))
			return;

		getResults().addResult(
				new AbstractionAtSink(Collections.singleton(SecondarySinkDefinition.INSTANCE), source, stmt));
	}

	/**
	 * Records a secondary flow taint reaching a turn-around point. Important: This
	 * method does not check whether the secondary flow should be recorded.
	 * 
	 * @param d1     The calling context
	 * @param source The current taint abstraction
	 * @param stmt   The current statement, which is assumed to be a sink
	 */
	public void processTurnAroundSink(Abstraction d1, Abstraction source, Stmt stmt) {

		// Static fields are not part of the conditional flow model.
		if (!source.isAbstractionActive() || source.getAccessPath().isStaticFieldRef())
			return;

		getResults().addResult(
				new AbstractionAtSink(Collections.singleton(TurnAroundSecondarySinkDefinition.INSTANCE), source, stmt));
	}

}
