package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.Stmt;

public interface IConditionalFlowManager {
    /**
     * Checks if a sink is reached in the secondary flow
     *
     * @param stmt Sink Statement
     * @return true if stmt is a secondary sink
     */
    boolean isSecondarySink(Stmt stmt);

    /**
     * Checks whether stmt is a conditional sink and needs a secondary flow.
     * Ensures that the statement contains a InstanceInvokeExpr.
     *
     * @param stmt Sink Statement
     * @return true if stmt is a conditional sink
     */
    boolean isConditionalSink(Stmt stmt);
}
