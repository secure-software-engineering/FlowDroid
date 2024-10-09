package soot.jimple.infoflow.solver.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import soot.Body;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;

/**
 * Inverse interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class BackwardsInfoflowCFG extends InfoflowCFG {

	private final IInfoflowCFG baseCFG;

	public BackwardsInfoflowCFG(IInfoflowCFG baseCFG) {
		super(new BackwardsInterproceduralCFG(baseCFG));
		this.baseCFG = baseCFG;
	}

	public IInfoflowCFG getBaseCFG() {
		return this.baseCFG;
	}

	@Override
	public boolean isStaticFieldRead(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldRead(method, variable);
	}

	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldUsed(method, variable);
	}

	@Override
	public boolean hasSideEffects(SootMethod method) {
		return baseCFG.hasSideEffects(method);
	}

	@Override
	public boolean methodReadsValue(SootMethod m, Value v) {
		return baseCFG.methodReadsValue(m, v);
	}

	@Override
	public boolean methodWritesValue(SootMethod m, Value v) {
		return baseCFG.methodWritesValue(m, v);
	}

	@Override
	public UnitContainer getPostdominatorOf(Unit u) {
		return baseCFG.getPostdominatorOf(u);
	}

	@Override
	public UnitContainer getDominatorOf(Unit u) {
		return baseCFG.getDominatorOf(u);
	}

	@Override
	public boolean isExceptionalEdgeBetween(Unit u1, Unit u2) {
		return super.isExceptionalEdgeBetween(u2, u1);
	}

	@Override
	public List<Unit> getConditionalBranchIntraprocedural(Unit unit) {
		SootMethod sm = getMethodOf(unit);
		// Exclude the dummy method
		if (sm.getDeclaringClass().getName().equals("dummyMainClass") && sm.getName().equals("dummy"))
			return null;

		DirectedGraph<Unit> graph = getOrCreateUnitGraph(sm);
		List<Unit> worklist = new ArrayList<>(sameLevelPredecessors(graph, unit));
		Set<Unit> doneSet = new HashSet<>();
		List<Unit> conditionals = new ArrayList<>();
		while (worklist.size() > 0) {
			Unit item = worklist.remove(0);
			doneSet.add(item);
			if (item instanceof IfStmt || item instanceof SwitchStmt) {
				conditionals.add(item);
			}

			List<Unit> preds = new ArrayList<>(sameLevelPredecessors(graph, item));
			preds.removeIf(doneSet::contains);
			worklist.addAll(preds);
		}
		return conditionals;
	}

	@Override
	public List<Unit> getConditionalBranchesInterprocedural(Unit unit) {
		List<Unit> conditionals = new ArrayList<>();
		Set<Unit> doneSet = new HashSet<>();
		getConditionalsRecursive(unit, conditionals, doneSet);
		return conditionals;
	}

	/**
	 * Finds all possible interprocedural conditionals recursive
	 * 
	 * @param unit         start unit
	 * @param conditionals result list
	 * @param doneSet      already processed units
	 */
	private void getConditionalsRecursive(Unit unit, List<Unit> conditionals, Set<Unit> doneSet) {
		SootMethod sm = getMethodOf(unit);
		// Exclude the dummy method
		if (sm.getDeclaringClass().getName().equals("dummyMainClass") && sm.getName().equals("dummy"))
			return;

		DirectedGraph<Unit> graph = getOrCreateUnitGraph(sm);
		List<Unit> worklist = new ArrayList<>(sameLevelPredecessors(graph, unit));
		worklist.removeIf(doneSet::contains);
		doneSet.addAll(worklist);
		// firstRun prevents taking the queried statement as a call site
		while (worklist.size() > 0) {
			Unit item = worklist.remove(0);

			if (item instanceof IfStmt || item instanceof SwitchStmt)
				conditionals.add(item);

			// call sites
			if (item instanceof Stmt && ((Stmt) item).containsInvokeExpr()) {
				List<Unit> entryPoints = getPredsOf(item).stream().filter(pred -> getMethodOf(pred) != sm)
						.collect(Collectors.toList());
				entryPoints.removeIf(doneSet::contains);
				for (Unit entryPoint : entryPoints) {
					getConditionalsRecursive(entryPoint, conditionals, doneSet);
				}
			}
			// returns
			if (isExitStmt(item)) {
				List<Unit> entryPoints = new ArrayList<>(getCallersOf(sm));
				entryPoints.removeIf(doneSet::contains);
				for (Unit entryPoint : entryPoints) {
					getConditionalsRecursive(entryPoint, conditionals, doneSet);
				}
			}

			List<Unit> preds = new ArrayList<>(sameLevelPredecessors(graph, item));
			preds.removeIf(doneSet::contains);
			worklist.addAll(preds);
			doneSet.addAll(preds);
		}
	}

	private List<Unit> sameLevelPredecessors(DirectedGraph<Unit> graph, Unit u) {
		List<Unit> preds = graph.getPredsOf(u);
		if (preds.size() <= 1) {
			if (preds.size() == 0)
				return preds;
			// maybe we encountered an if with no body
			Unit pred = preds.get(0);
			if (!pred.branches())
				return preds;

			UnitContainer postdom = getPostdominatorOf(pred);
			if (postdom.getUnit() != u)
				return preds;
		}

		UnitContainer dom = getDominatorOf(u);
		if (dom.getUnit() != null)
			return graph.getPredsOf(dom.getUnit());
		return Collections.emptyList();
	}

	@Override
	public void notifyMethodChanged(SootMethod m) {
		baseCFG.notifyMethodChanged(m);
	}

	@Override
	public void notifyNewBody(Body b) {
		baseCFG.notifyNewBody(b);
	}

	@Override
	public void purge() {
		baseCFG.purge();
		super.purge();
	}

}
