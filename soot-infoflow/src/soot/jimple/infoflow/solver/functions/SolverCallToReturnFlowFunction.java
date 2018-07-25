package soot.jimple.infoflow.solver.functions;

import soot.jimple.infoflow.solver.ngsolver.FlowFunction;

/**
 * A special implementation of the call-to-return flow function that allows
 * access to the fact associated with the method's start point (i.e. the current
 * context).
 * 
 * @author Steven Arzt
 */
public interface SolverCallToReturnFlowFunction<N, D> extends FlowFunction<N, D> {

}
