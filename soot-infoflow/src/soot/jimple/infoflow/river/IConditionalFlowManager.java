package soot.jimple.infoflow.river;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

/**
 * A source sink manager that is able to manage sinks with conditions, i.e. the sink context.
 *
 * @author Tim Lange
 */
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
     * @param stmt      Sink Statement
     * @param baseClass Class of the tainted base
     * @return true if stmt is a conditional sink
     */
    boolean isConditionalSink(Stmt stmt, SootClass baseClass);

    /**
     * Register a secondary sink at runtime.
     *
     * @param stmt Secondary sink statement
     */
    void registerSecondarySink(Stmt stmt);

    /**
     * Register a secondary sink at runtime.
     *
     * @param sm Secondary sink method
     */
    void registerSecondarySink(SootMethod sm);

}
