package soot.jimple.infoflow.aliasing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import heros.solver.PathEdge;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.solver.IInfoflowSolver;

/**
 * A simple points-to-based aliasing strategy for FlowDroid
 * 
 * @author Steven Arzt
 */
public class PtsBasedAliasStrategy extends AbstractBulkAliasStrategy {

	private final Table<SootMethod, Abstraction, Set<Abstraction>> aliases = HashBasedTable.create();

	public PtsBasedAliasStrategy(InfoflowManager manager) {
		super(manager);
	}

	@Override
	public void computeAliasTaints(Abstraction d1, Stmt src, Value targetValue, Set<Abstraction> taintSet,
			SootMethod method, Abstraction newAbs) {
		computeAliasTaintsInternal(d1, method, newAbs, Collections.emptyList(),
				newAbs.getAccessPath().getTaintSubFields(), src);
	}

	public void computeAliasTaintsInternal(Abstraction d1, SootMethod method, Abstraction newAbs,
			List<AccessPathFragment> appendFragments, boolean taintSubFields, Stmt actStmt) {

		actStmt = newAbs.getActivationUnit() == null ? actStmt : (Stmt) newAbs.getActivationUnit();

		// Record the incoming abstraction
		synchronized (aliases) {
			if (aliases.contains(method, newAbs)) {
				Set<Abstraction> d1s = aliases.get(method, newAbs);
				if (d1s.contains(d1))
					return;
				d1s.add(d1);
			} else {
				Set<Abstraction> d1s = Sets.newIdentityHashSet();
				d1s.add(d1);
				aliases.put(method, newAbs, d1s);
			}
		}

		// Also check for aliases for parts of the access path
		final AccessPath ap = newAbs.getAccessPath();
		if ((ap.isInstanceFieldRef() && ap.getFirstField() != null)
				|| (ap.isStaticFieldRef() && ap.getFragmentCount() > 1)) {
			List<AccessPathFragment> appendList = new LinkedList<>(appendFragments);
			appendList.add(0, newAbs.getAccessPath().getLastFragment());

			computeAliasTaintsInternal(d1, method,
					newAbs.deriveNewAbstraction(newAbs.getAccessPath().dropLastField(), null), appendList,
					taintSubFields, actStmt);
		}

		// Do not try to compute points-to-sets on complex access paths
		if (ap.getFragmentCount() > 1)
			return;

		PointsToSet ptsTaint = getPointsToSet(newAbs.getAccessPath());
		AccessPathFragment[] appendFragmentsA = appendFragments.toArray(new AccessPathFragment[appendFragments.size()]);

		// We run once per method and we are flow-insensitive anyway, so we
		// can just say that every use of a variable aliased with a tainted
		// one automatically taints the corresponding def set.
		boolean beforeActUnit = method.getActiveBody().getUnits().contains(actStmt);
		final AccessPathFactory apFactory = manager.getAccessPathFactory();
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt == actStmt)
				beforeActUnit = false;

			// Generic check for relevant variables
			PointsToSet ptsBaseOrg = getPointsToSet(newAbs.getAccessPath().getPlainValue());
			for (ValueBox vb : stmt.getUseAndDefBoxes()) {
				PointsToSet ptsBase = getPointsToSet(vb.getValue());
				if (ptsBase != null && ptsBase.hasNonEmptyIntersection(ptsBaseOrg)) {
					// Schedule the AP at the location where we found the alias
					AccessPath newAP = apFactory.appendFields(
							apFactory.copyWithNewValue(newAbs.getAccessPath(), vb.getValue()), appendFragmentsA,
							taintSubFields);
					Abstraction absCallee = newAbs.deriveNewAbstraction(newAP, stmt);
					if (beforeActUnit)
						absCallee = absCallee.deriveInactiveAbstraction(actStmt);
					else
						absCallee = absCallee.getActiveCopy();
					manager.getMainSolver().processEdge(new PathEdge<Unit, Abstraction>(d1, u, absCallee));

					// One alias is enough
					break;
				}
			}

			// If we have a = b and our taint is an alias to b, we must add
			// a taint for a.
			if (u instanceof DefinitionStmt) {
				DefinitionStmt assign = (DefinitionStmt) u;
				Value rop = assign.getRightOp();
				Value lop = assign.getLeftOp();
				if (isAliasedAtStmt(ptsTaint, rop) && (appendFragments != null && appendFragments.size() > 0)) {
					Abstraction aliasAbsLeft = newAbs.deriveNewAbstraction(
							manager.getAccessPathFactory().createAccessPath(lop, appendFragmentsA, taintSubFields),
							stmt);
					if (aliasAbsLeft != null) {
						if (beforeActUnit)
							aliasAbsLeft = aliasAbsLeft.deriveInactiveAbstraction(actStmt);
						else
							aliasAbsLeft = aliasAbsLeft.getActiveCopy();
						computeAliasTaints(d1, stmt, lop, Collections.<Abstraction>emptySet(), method, aliasAbsLeft);
					}
				}

				// If we have a = b and our taint is an alias to a, we must add
				// a taint for b.
				if (isAliasedAtStmt(ptsTaint, lop) && isValidAccessPathRoot(rop)) {
					Abstraction aliasAbsRight = newAbs.deriveNewAbstraction(
							manager.getAccessPathFactory().createAccessPath(rop, appendFragmentsA, taintSubFields),
							stmt);
					if (aliasAbsRight != null) {
						if (beforeActUnit)
							aliasAbsRight = aliasAbsRight.deriveInactiveAbstraction(actStmt);
						else
							aliasAbsRight = aliasAbsRight.getActiveCopy();
						manager.getMainSolver().processEdge(new PathEdge<Unit, Abstraction>(d1, u, aliasAbsRight));
					}
				}
			}
		}
	}

	private boolean isValidAccessPathRoot(Value op) {
		return op instanceof FieldRef || op instanceof Local || op instanceof ArrayRef;
	}

	private boolean isAliasedAtStmt(PointsToSet ptsTaint, Value val) {
		if (ptsTaint != null) {
			PointsToSet ptsRight = getPointsToSet(val);
			return ptsRight != null && ptsTaint.hasNonEmptyIntersection(ptsRight);
		}
		return false;
	}

	/**
	 * Gets the points-to-set for the given value
	 * 
	 * @param targetValue The value for which to get the points-to-set
	 * @return The points-to-set for the given value
	 */
	private PointsToSet getPointsToSet(Value targetValue) {
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		synchronized (pta) {
			if (targetValue instanceof Local)
				return pta.reachingObjects((Local) targetValue);
			else if (targetValue instanceof InstanceFieldRef) {
				InstanceFieldRef iref = (InstanceFieldRef) targetValue;
				return pta.reachingObjects((Local) iref.getBase(), iref.getField());
			} else if (targetValue instanceof StaticFieldRef) {
				StaticFieldRef sref = (StaticFieldRef) targetValue;
				return pta.reachingObjects(sref.getField());
			} else if (targetValue instanceof ArrayRef) {
				ArrayRef aref = (ArrayRef) targetValue;
				return pta.reachingObjects((Local) aref.getBase());
			} else
				return null;
		}
	}

	/**
	 * Gets the points-to-set for the given access path
	 * 
	 * @param accessPath The access path for which to get the points-to-set
	 * @return The points-to-set for the given access path
	 */
	private PointsToSet getPointsToSet(AccessPath accessPath) {
		if (accessPath.isLocal())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue());
		else if (accessPath.isInstanceFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue(),
					accessPath.getFirstField());
		else if (accessPath.isStaticFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getFirstField());
		else
			throw new RuntimeException("Unexepected access path type");
	}

	@Override
	public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver, SootMethod callee, Unit callSite,
			Abstraction source, Abstraction d1) {

	}

	@Override
	public boolean isFlowSensitive() {
		return false;
	}

	@Override
	public boolean requiresAnalysisOnReturn() {
		return true;
	}

	@Override
	public IInfoflowSolver getSolver() {
		return null;
	}

	@Override
	public void cleanup() {
		aliases.clear();
	}

}
