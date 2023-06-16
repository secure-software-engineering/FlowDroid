package soot.jimple.infoflow.solver.gcSolver;

import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Implementation of a reference provider that computes its dependencies ahead
 * of time, and over-approximates the possible references by considering all
 * transitively callees of a given method as possible locations for new analysis
 * tasks, regardless of context and taint state.
 * 
 * @author Steven Arzt
 */
public class AheadOfTimeReferenceProvider<N> extends AbstractReferenceProvider<SootMethod, N> {

	private final MultiMap<SootMethod, SootMethod> methodToCallees = new HashMultiMap<>();

	public AheadOfTimeReferenceProvider(BiDiInterproceduralCFG<N, SootMethod> icfg) {
		super(icfg);

		// Initialize the caller/callee relationships
		for (SootClass sc : Scene.v().getClasses()) {
			for (SootMethod sm : sc.getMethods())
				methodToCallees.putAll(sm, getTransitiveCallees(sm));
		}
	}

	@Override
	public Set<SootMethod> getAbstractionReferences(SootMethod method) {
		return methodToCallees.get(method);
	}

}
