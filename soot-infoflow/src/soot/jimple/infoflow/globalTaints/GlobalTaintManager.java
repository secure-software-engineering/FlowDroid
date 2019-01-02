package soot.jimple.infoflow.globalTaints;

import java.util.HashSet;
import java.util.Set;

import heros.solver.PathEdge;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.util.queue.QueueReader;

/**
 * Manager class for storing and processing global taints, i.e., taints that
 * shall be valid regardless of context and statement. This class is useful for
 * handling field-based taints outside of IFDS.
 * 
 * @author Steven Arzt
 *
 */
public class GlobalTaintManager {

	private final Set<Abstraction> globalTaintState = new HashSet<>();
	private final Set<IInfoflowSolver> solvers;

	public GlobalTaintManager(Set<IInfoflowSolver> solvers) {
		this.solvers = solvers;
	}

	/**
	 * Adds an abstraction to the global taint state. The global taint models taint
	 * abstractions that shall be valid regardless of context and statement. This
	 * feature can be used to handle, e.g., field-based data flow analyses for
	 * static fields.
	 * 
	 * @param abs The abstraction to add to the global taint state
	 * @return True if the abstraction was added, false if an equal abstraction has
	 *         been recorded before
	 */
	public boolean addToGlobalTaintState(Abstraction abs) {
		if (globalTaintState.add(abs) && solvers != null && !solvers.isEmpty()) {
			// Find statements that read the given taint. At the moment, we only support
			// taints on static field here.
			Set<Stmt> injectionPoints = new HashSet<>();
			QueueReader<MethodOrMethodContext> methodListener = Scene.v().getReachableMethods().listener();
			MethodOrMethodContext mmoc;
			while (methodListener.hasNext() && (mmoc = methodListener.next()) != null) {
				SootMethod sm = mmoc.method();
				if (sm != null && sm.isConcrete()) {
					for (Unit u : sm.getActiveBody().getUnits()) {
						if (u instanceof Stmt) {
							Stmt stmt = (Stmt) u;
							for (ValueBox vb : stmt.getUseBoxes()) {
								if (vb.getValue() instanceof StaticFieldRef) {
									StaticFieldRef fieldRef = (StaticFieldRef) vb.getValue();
									if (abs.getAccessPath().firstFieldMatches(fieldRef.getField())) {
										injectionPoints.add(stmt);
									}
								}
							}
						}
					}
				}
			}

			// Notify the solvers of the new taint abstraction
			if (!injectionPoints.isEmpty()) {
				for (IInfoflowSolver solver : solvers) {
					for (Stmt stmt : injectionPoints)
						solver.processEdge(new PathEdge<>(solver.getTabulationProblem().zeroValue(), stmt, abs));
				}
			}

			return true;
		}
		return false;
	}

}