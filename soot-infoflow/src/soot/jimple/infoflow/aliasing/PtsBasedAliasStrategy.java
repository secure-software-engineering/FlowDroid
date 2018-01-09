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
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
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
	public void computeAliasTaints(Abstraction d1, Stmt src, Value targetValue,
			Set<Abstraction> taintSet, SootMethod method, Abstraction newAbs) {
		computeAliasTaintsInternal(d1, method, newAbs, Collections.<SootField>emptyList(),
				Collections.<Type>emptyList(), newAbs.getAccessPath().getTaintSubFields(), src);
	}
	
	public void computeAliasTaintsInternal(Abstraction d1, SootMethod method,
			Abstraction newAbs, List<SootField> appendFields, List<Type> appendTypes,
			boolean taintSubFields, Stmt actStmt) {
		// Record the incoming abstraction
		synchronized(aliases) {
			if (aliases.contains(method, newAbs)) {
				Set<Abstraction> d1s = aliases.get(method, newAbs);
				if (d1s.contains(d1))
					return;
				d1s.add(d1);
			}
			else {
				Set<Abstraction> d1s = Sets.newIdentityHashSet();
				d1s.add(d1);
				aliases.put(method, newAbs, d1s);
			}
		}
		
		// Also check for aliases for parts of the access path
		final AccessPath ap = newAbs.getAccessPath();
		if ((ap.isInstanceFieldRef() && ap.getFirstField() != null)
				|| (ap.isStaticFieldRef() && ap.getFieldCount() > 1)) {
			List<SootField> appendList = new LinkedList<SootField>(appendFields);
			appendList.add(0, newAbs.getAccessPath().getLastField());
			List<Type> typesList = new LinkedList<Type>(appendTypes);
			typesList.add(0, newAbs.getAccessPath().getLastFieldType());
			
			computeAliasTaintsInternal(d1, method, newAbs.deriveNewAbstraction
					(newAbs.getAccessPath().dropLastField(), null), appendList, typesList, taintSubFields, actStmt);
		}
		
		// Do not try to compute points-to-sets on complex access paths
		if (ap.getFieldCount() > 1)
			return;
		
		PointsToSet ptsTaint = getPointsToSet(newAbs.getAccessPath());
		SootField[] appendFieldsA = appendFields.toArray(new SootField[appendFields.size()]);
		Type[] appendTypesA = appendTypes.toArray(new Type[appendTypes.size()]);
		
		// We run once per method and we are flow-insensitive anyway, so we
		// can just say that every use of a variable aliased with a tainted
		// one automatically taints the corresponding def set.
		boolean beforeActUnit = method.getActiveBody().getUnits().contains(actStmt);
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt == actStmt)
				beforeActUnit = false;
			
			if (stmt.containsInvokeExpr()) {
				// If we have a call, we must check whether the base or one of
				// the parameter aliases with the given taint
				InvokeExpr invExpr = (InvokeExpr) stmt.getInvokeExpr();
				boolean baseAliases = false;
				if (invExpr instanceof InstanceInvokeExpr && !newAbs.getAccessPath().isStaticFieldRef()) {
					InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;
					PointsToSet ptsBase = getPointsToSet((Local) iinvExpr.getBase());
					PointsToSet ptsBaseOrg = getPointsToSet(newAbs.getAccessPath().getPlainValue());
					baseAliases = ptsBase.hasNonEmptyIntersection(ptsBaseOrg);
				}
				
				boolean parameterAliases = false;
				for (Value arg : invExpr.getArgs())
					if (arg instanceof Local)
						if (getPointsToSet(arg).hasNonEmptyIntersection(ptsTaint)) {
							parameterAliases = true;
							break;
						}
					
				if (baseAliases || parameterAliases) {
					AccessPath newAP = manager.getAccessPathFactory().appendFields
							(newAbs.getAccessPath(), appendFieldsA, appendTypesA, taintSubFields);
					Abstraction absCallee = newAbs.deriveNewAbstraction(newAP, stmt);
					if (beforeActUnit)
						absCallee = absCallee.deriveInactiveAbstraction(actStmt);
					manager.getForwardSolver().processEdge(new PathEdge<Unit, Abstraction>(d1, u, absCallee));
				}
			}
			else if (u instanceof DefinitionStmt) {
				DefinitionStmt assign = (DefinitionStmt) u;
				
				// If we have a = b and our taint is an alias to b, we must add
				// a taint for a.
				if (assign.getRightOp() instanceof FieldRef || assign.getRightOp() instanceof Local
						|| assign.getRightOp() instanceof ArrayRef) {
					if (isAliasedAtStmt(ptsTaint, assign.getRightOp())
							&& (appendFields != null && appendFields.size() > 0)) {
						Abstraction aliasAbsLeft = newAbs.deriveNewAbstraction(
								manager.getAccessPathFactory().createAccessPath(assign.getLeftOp(),
										appendFieldsA, taintSubFields), stmt);
						if (beforeActUnit)
							aliasAbsLeft = aliasAbsLeft.deriveInactiveAbstraction(actStmt);
						
						computeAliasTaints(d1, stmt, assign.getLeftOp(), Collections.<Abstraction>emptySet(),
								method, aliasAbsLeft);
					}
				}

				// If we have a = b and our taint is an alias to a, we must add
				// a taint for b.
				if (assign.getLeftOp() instanceof FieldRef
						|| assign.getLeftOp() instanceof Local
						|| assign.getLeftOp() instanceof ArrayRef)
					if (assign.getRightOp() instanceof FieldRef
							|| assign.getRightOp() instanceof Local
							|| assign.getRightOp() instanceof ArrayRef) {
						if (isAliasedAtStmt(ptsTaint, assign.getLeftOp())) {
							Abstraction aliasAbsRight = newAbs.deriveNewAbstraction(
									manager.getAccessPathFactory().createAccessPath(assign.getRightOp(),
											appendFieldsA, taintSubFields), stmt);
							if (beforeActUnit)
								aliasAbsRight = aliasAbsRight.deriveInactiveAbstraction(actStmt);
							manager.getForwardSolver().processEdge(new PathEdge<Unit, Abstraction>(d1, u, aliasAbsRight));
						}
					}
			}
		}
	}
	
	private boolean isAliasedAtStmt(PointsToSet ptsTaint, Value val) {
		PointsToSet ptsRight = getPointsToSet(val);
		return ptsTaint.hasNonEmptyIntersection(ptsRight);
	}
	
	/**
	 * Gets the points-to-set for the given value
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
			}
			else if (targetValue instanceof StaticFieldRef) {
				StaticFieldRef sref = (StaticFieldRef) targetValue;
				return pta.reachingObjects(sref.getField());
			}
			else if (targetValue instanceof ArrayRef) {
				ArrayRef aref = (ArrayRef) targetValue;
				return pta.reachingObjects((Local) aref.getBase());
			}
			else
				throw new RuntimeException("Unexpected value type for aliasing: " + targetValue.getClass());
		}
	}

	/**
	 * Gets the points-to-set for the given access path
	 * @param accessPath The access path for which to get the points-to-set
	 * @return The points-to-set for the given access path
	 */
	private PointsToSet getPointsToSet(AccessPath accessPath) {
		if (accessPath.isLocal())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue());
		else if (accessPath.isInstanceFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getPlainValue(), accessPath.getFirstField());
		else if (accessPath.isStaticFieldRef())
			return Scene.v().getPointsToAnalysis().reachingObjects(accessPath.getFirstField());
		else
			throw new RuntimeException("Unexepected access path type");
	}
	
	@Override
	public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver,
			SootMethod callee, Unit callSite, Abstraction source, Abstraction d1) {
		
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
