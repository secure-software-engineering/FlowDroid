package soot.jimple.infoflow.solver.sparseSolver;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import heros.FlowFunction;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.solver.sparseSolver.propagation.DensePropagation;
import soot.jimple.infoflow.solver.sparseSolver.propagation.IPropagationStrategy;
import soot.jimple.infoflow.solver.sparseSolver.propagation.PreciseSparsePropagation;
import soot.jimple.infoflow.solver.sparseSolver.propagation.SimpleSparsePropagation;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * InfoflowSolver that computes the successor statements for each abstraction in the outgoing set
 *
 * @author Tim Lange
 */
public class SparseInfoflowSolver extends InfoflowSolver {
    private final IPropagationStrategy<Unit, Abstraction, BiDiInterproceduralCFG<Unit, SootMethod>> propagationStrategy;

    public SparseInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor,
                                InfoflowConfiguration.SparsePropagationStrategy propStrategyOpt) {
        super(problem, executor);
        switch (propStrategyOpt) {
            case Dense:
                propagationStrategy = new DensePropagation<>(problem.interproceduralCFG());
                break;
            case Simple:
                propagationStrategy = new SimpleSparsePropagation(problem);
                break;
            case Precise:
                propagationStrategy = new PreciseSparsePropagation(problem);
                break;
            default:
                throw new RuntimeException("Unknown option!");
        }
    }

    @Override
    protected void processNormalFlow(PathEdge<Unit, Abstraction> edge) {
        // Fallback for implicit flows, which always need the successor statement
        if (this.problem.getManager().getConfig().getImplicitFlowMode().trackControlFlowDependencies()) {
            super.processNormalFlow(edge);
            return;
        }

        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget();
        final Abstraction d2 = edge.factAtTarget();

        // Early termination check
        if (killFlag != null)
            return;

        // The successor is a chicken-and-egg problem because the successor statement depends on the output
        // of the flow function. We set it to null, knowing only the implicit flow rule uses them.
        FlowFunction<Abstraction> flowFunction = flowFunctions.getNormalFlowFunction(n, null);
        Set<Abstraction> res = computeNormalFlowFunction(flowFunction, d1, d2);
        if (res != null && !res.isEmpty()) {
            for (Abstraction d3 : res) {
                // Difference: choose the successor for each abstraction in the outgoing set
                for (Unit m : propagationStrategy.getSuccsOf(n, d3)) {
                    if (memoryManager != null && d2 != d3)
                        d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                    if (d3 != null)
                        schedulingStrategy.propagateNormalFlow(d1, m, d3, null, false);
                }
            }
        }
    }

    @Override
    protected void processCall(PathEdge<Unit, Abstraction> edge) {
        // Fallback for implicit flows, which always need the successor statement
        if (this.problem.getManager().getConfig().getImplicitFlowMode().trackControlFlowDependencies()) {
            super.processCall(edge);
            return;
        }

        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget(); // a call node; line 14...

        final Abstraction d2 = edge.factAtTarget();
        assert d2 != null;

        Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(n);

        // for each possible callee
        Collection<SootMethod> callees = icfg.getCalleesOfCallAt(n);
        if (callees != null && !callees.isEmpty()) {
            if (maxCalleesPerCallSite < 0 || callees.size() <= maxCalleesPerCallSite) {
                callees.stream().filter(m -> m.isConcrete()).forEach(new Consumer<SootMethod>() {

                    @Override
                    public void accept(SootMethod sCalledProcN) {
                        // Early termination check
                        if (killFlag != null)
                            return;

                        // compute the call-flow function
                        FlowFunction<Abstraction> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
                        Set<Abstraction> res = computeCallFlowFunction(function, d1, d2);

                        if (res != null && !res.isEmpty()) {
                            // for each result node of the call-flow function
                            for (Abstraction d3 : res) {
                                // Difference: Decide start points per abstractions
                                Collection<Unit> startPointsOf = propagationStrategy.getStartPointsOf(sCalledProcN, d3);

                                if (memoryManager != null)
                                    d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                                if (d3 == null)
                                    continue;

                                // for each callee's start point(s)
                                for (Unit sP : startPointsOf) {
                                    // create initial self-loop
                                    schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                                }

                                // register the fact that <sp,d3> has an incoming edge from
                                // <n,d2>
                                // line 15.1 of Naeem/Lhotak/Rodriguez
                                if (!addIncoming(sCalledProcN, d3, n, d1, d2))
                                    continue;

                                applyEndSummaryOnCall(d1, n, d2, returnSiteNs, sCalledProcN, d3);
                            }
                        }
                    }

                });
            }
        }

        // line 17-19 of Naeem/Lhotak/Rodriguez
        // process intra-procedural flows along call-to-return flow functions
        FlowFunction<Abstraction> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, null);
        Set<Abstraction> res = computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2);
        if (res != null && !res.isEmpty()) {
            for (Abstraction d3 : res) {
                // Difference: get return site per abstraction
                for (Unit returnSiteN : propagationStrategy.getSuccsOf(n, d3)) {
                    if (memoryManager != null)
                        d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                    if (d3 != null)
                        schedulingStrategy.propagateCallToReturnFlow(d1, returnSiteN, d3, n, false);
                }
            }
        }
    }

    // Note: The ReturnFlowFunction depends on the successor to determine whether the return is exceptional or not.
    //       To keep this solver compatible with the default flow functions, we do not override processExit().
}
