package soot.jimple.infoflow.solver.functions;

import soot.jimple.infoflow.solver.ngsolver.FlowFunction;

/**
 * A special implementation of the normal flow function that allows access to
 * the fact associated with the method's start point (i.e. the current context).
 * 
 * @author Steven Arzt
 */
public interface SolverNormalFlowFunction<N, D> extends FlowFunction<N, D> {

}
