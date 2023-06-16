package soot.jimple.infoflow.solver.gcSolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Abstract base class for reference providers
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractReferenceProvider<A, N> implements IGCReferenceProvider<A> {

	protected final BiDiInterproceduralCFG<N, SootMethod> icfg;

	public AbstractReferenceProvider(BiDiInterproceduralCFG<N, SootMethod> icfg) {
		this.icfg = icfg;
	}

	/**
	 * Computes the set of transitive callees of the given method
	 * 
	 * @param method The method for which to compute callees
	 * @return The set of transitive callees of the given method
	 */
	protected Set<SootMethod> getTransitiveCallees(SootMethod method) {
		Set<SootMethod> callees = new HashSet<>();
		List<SootMethod> workList = new ArrayList<>();
		workList.add(method);

		while (!workList.isEmpty()) {
			SootMethod sm = workList.remove(0);
			if (sm.isConcrete()) {
				// We can only look for callees if we have a body
				if (sm.hasActiveBody()) {
					// Schedule the callees
					for (N callSite : icfg.getCallsFromWithin(sm)) {
						for (SootMethod callee : icfg.getCalleesOfCallAt(callSite)) {
							if (callees.add(callee))
								workList.add(callee);
						}
					}
				}
			}
		}

		return callees;
	}

}
