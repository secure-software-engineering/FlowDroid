package soot.jimple.infoflow.problems.rules.backward;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.solver.PathEdge;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Implicit flows for the backward direction.
 *
 * @author Tim Lange
 */
public class BackwardsImplicitFlowRule extends AbstractTaintPropagationRule {
	private final MyConcurrentHashMap<Unit, Set<Abstraction>> implicitTargets = new MyConcurrentHashMap<Unit, Set<Abstraction>>();

	public BackwardsImplicitFlowRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

		if (source == getZeroValue())
			return null;

		// We leave a conditional and taint the condition
		if (source.isDominator(stmt)) {
			// never let empty ap out of conditional
			if (!source.getAccessPath().isEmpty()) {
				// A non-empty ap means the taint entered the conditional
				// and is now exiting it. The dominator needs to be removed.
				// We do not want to pollute the main flow functions, so we
				// chose to killAll and inject the taint again.
				Abstraction abs = source.removeDominator(stmt);
				if (abs != null)
					manager.getMainSolver().processEdge(new PathEdge<>(d1, stmt, abs));

				killAll.value = true;
				return null;
			}

			// If an empty taint reached the end of a conditional,
			// we had an update inside and need to taint the condition.
			killSource.value = true;

			Value condition;
			if (stmt instanceof IfStmt)
				condition = ((IfStmt) stmt).getCondition();
			else if (stmt instanceof SwitchStmt)
				condition = ((SwitchStmt) stmt).getKey();
			else
				return null;

			UnitContainer condUnit = manager.getICFG().getDominatorOf(stmt);
			Set<Abstraction> res = new HashSet<>();
			// We observe the condition at leaving and taint the conditions here.
			if (condition instanceof Local) {
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(condition, false);
				Abstraction abs = source.deriveCondition(ap, stmt);
				res.add(abs);
				if (condUnit.getUnit() != null)
					res.add(abs.deriveNewAbstractionWithDominator(condUnit.getUnit()));
				return res;
			} else {
				for (ValueBox box : condition.getUseBoxes()) {
					if (box.getValue() instanceof Constant)
						continue;

					AccessPath ap = manager.getAccessPathFactory().createAccessPath(box.getValue(), false);
					Abstraction abs = source.deriveCondition(ap, stmt);
					res.add(abs);
					if (condUnit.getUnit() != null)
						res.add(abs.deriveNewAbstractionWithDominator(condUnit.getUnit()));
				}
				return res;
			}
		}

		// All exceptional edges are considered implicit
		if (source.getAccessPath().isEmpty() && manager.getICFG().isExceptionalEdgeBetween(stmt, destStmt)) {
			// We look forward and taint the left side of the next statement if possible
			if (destStmt instanceof AssignStmt) {
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(((AssignStmt) destStmt).getLeftOp(),
						false);
				Abstraction abs = source.deriveNewAbstraction(ap, stmt);
				return Collections.singleton(abs);
			}
			return null;
		}

		// Already empty APs stay the same
		if (source.getAccessPath().isEmpty())
			return null;

		UnitContainer dominator = manager.getICFG().getDominatorOf(stmt);

		// Taint enters a conditional branch
		// Only handle cases where the taint is not part of the statement
		// Other cases are in the core flow functions to prevent code duplication
		boolean taintAffectedByStatement = stmt instanceof DefinitionStmt
				&& getAliasing().mayAlias(((DefinitionStmt) stmt).getLeftOp(), source.getAccessPath().getPlainValue());
		if (dominator.getUnit() != null && dominator.getUnit() != destStmt && !taintAffectedByStatement) {
			killSource.value = true;
			Abstraction abs = source.deriveNewAbstractionWithDominator(dominator.getUnit(), stmt);
			return Collections.singleton(abs);
		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

		if (source == getZeroValue())
			return null;
		Aliasing aliasing = getAliasing();
		if (aliasing == null)
			return null;

		// Make sure no conditional taint leaves the conditional branch
		// Kill conditional taints leaving the branch
		if (source.isDominator(stmt)) {
			killAll.value = true;
			if (!source.getAccessPath().isEmpty()) {
				// A non-empty ap means the taint entered the conditional
				// and is now exiting it. The dominator needs to be removed.
				// We do not want to pollute the main flow functions, so we
				// chose to killAll and inject the taint again.
				Abstraction abs = source.removeDominator(stmt);
				if (abs != null)
					manager.getMainSolver().processEdge(new PathEdge<>(d1, stmt, abs));
			}
			return null;
		}

		if (implicitTargets.containsKey(stmt) && (d1 == null || implicitTargets.get(stmt).contains(d1))) {
			if (killAll != null)
				killAll.value = true;
			return null;
		}

		// We do not propagate empty taints into methods
		// because backward no taints are derived from empty taints.
		if (source.getAccessPath().isEmpty()) {
			killAll.value = true;
			return null;
		}

		// Taint constant return values
		if (stmt instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) stmt;
			Value left = assignStmt.getLeftOp();

			boolean isImplicit = source.getDominator() != null;

			if (aliasing.mayAlias(left, source.getAccessPath().getPlainValue()) && !isImplicit) {
				Set<Abstraction> res = new HashSet<>();
				for (Unit unit : dest.getActiveBody().getUnits()) {
					if (unit instanceof ReturnStmt) {
						ReturnStmt returnStmt = (ReturnStmt) unit;
						Value retVal = returnStmt.getOp();

						if (retVal instanceof Constant) {
							Abstraction abs = source.deriveConditionalUpdate(stmt);
							abs = abs.deriveNewAbstractionWithTurnUnit(stmt);
							List<Unit> condUnits = manager.getICFG().getConditionalBranchIntraprocedural(returnStmt);
							for (Unit condUnit : condUnits) {
								Abstraction intraRet = abs.deriveNewAbstractionWithDominator(condUnit);
								res.add(intraRet);
							}
						}
					}
				}
				return res;
			}
		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// Every call to a sink inside a conditional is considered a taint
		if (source == getZeroValue() && manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager) {
			killSource.value = true;
			IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();

			SourceInfo sink = ssm.getInverseSinkInfo(stmt, manager);
			if (sink != null) {
				for (AccessPath ap : sink.getAccessPaths()) {
					HashSet<Abstraction> res = new HashSet<>();
					SootMethod sm = manager.getICFG().getMethodOf(stmt);

					List<Unit> condUnits = manager.getICFG().getConditionalBranchesInterprocedural(stmt);
					for (Unit condUnit : condUnits) {
						Abstraction abs = new Abstraction(sink.getAllDefinitions(), AccessPath.getEmptyAccessPath(), stmt,
								sink.getUserData(), false, false);
						abs.setCorrespondingCallSite(stmt);
						abs.setDominator(condUnit);
						res.add(abs);
					}

					if (!sm.isStatic()) {
						AccessPath thisAp = manager.getAccessPathFactory()
								.createAccessPath(sm.getActiveBody().getThisLocal(), false);
						Abstraction thisTaint = new Abstraction(sink.getDefinitionsForAccessPath(ap), thisAp, stmt, sink.getUserData(),
								false, false);
						thisTaint.setCorrespondingCallSite(stmt);
						res.add(thisTaint);
					}

					return res;
				}
			}
		}

		if (source == getZeroValue())
			return null;

		// Kill conditional taints leaving the branch
		if (source.isDominator(stmt)) {
			killAll.value = true;
			if (!source.getAccessPath().isEmpty()) {
				// A non-empty ap means the taint entered the conditional
				// and is now exiting it. The dominator needs to be removed.
				// We do not want to pollute the main flow functions, so we
				// chose to killAll and inject the taint again.
				Abstraction abs = source.removeDominator(stmt);
				if (abs != null)
					manager.getMainSolver().processEdge(new PathEdge<>(d1, stmt, abs));
			}
			return null;
		}

		// Conditional update
		if (stmt instanceof AssignStmt
				&& getAliasing().mayAlias(((AssignStmt) stmt).getLeftOp(), source.getAccessPath().getPlainValue())) {
			boolean isImplicit = source.getDominator() != null;
			if (isImplicit) {
//                if (d1 != null) {
//                    Set<Abstraction> callSites = implicitTargets.putIfAbsentElseGet(stmt,
//                            new ConcurrentHashSet<Abstraction>());
//                    callSites.add(d1);
//                }

				killSource.value = true;
				return Collections.singleton(source.deriveConditionalUpdate(stmt));
			}
		}

		if (source.getAccessPath().isEmpty())
			return null;

		// Taint entering a conditional is inside the main flow functions
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

		if (source == getZeroValue())
			return null;

		if (source.isDominator(stmt)) {
			killAll.value = true;
			if (!source.getAccessPath().isEmpty()) {
				// A non-empty ap means the taint entered the conditional
				// and is now exiting it. The dominator needs to be removed.
				// We do not want to pollute the main flow functions, so we
				// chose to killAll and inject the taint again.
				Abstraction abs = source.removeDominator(stmt);
				if (abs != null)
					manager.getMainSolver().processEdge(new PathEdge<>(calleeD1, stmt, abs));
			}
			return null;
		}

		if (source.getAccessPath().isEmpty()) {
			// Derived from a conditional taint inside the callee
			// Already has the right dominator
			return Collections.singleton(source.deriveNewAbstraction(source.getAccessPath(), stmt));
		}

		SootMethod callee = manager.getICFG().getMethodOf(stmt);
		List<Local> params = callee.getActiveBody().getParameterLocals();
		InvokeExpr ie = callSite.containsInvokeExpr() ? callSite.getInvokeExpr() : null;
		// In the callee, a parameter influenced a sink. If the argument was an constant
		// we need another implicit taint
		if (ie != null) {
			for (int i = 0; i < params.size() && i < ie.getArgCount(); i++) {
				if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(), params.get(i))
						&& ie.getArg(i) instanceof Constant) {
					List<Unit> condUnits = manager.getICFG().getConditionalBranchIntraprocedural(callSite);
					HashSet<Abstraction> res = new HashSet<>();
					for (Unit condUnit : condUnits) {
						Abstraction intraRet = source.deriveNewAbstractionWithDominator(condUnit, stmt);
						intraRet.setCorrespondingCallSite(callSite);
						res.add(intraRet);
					}
					return res;
				}
			}
		}

		return null;
	}
}
