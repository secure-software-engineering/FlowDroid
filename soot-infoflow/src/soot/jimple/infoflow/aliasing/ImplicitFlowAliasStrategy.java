package soot.jimple.infoflow.aliasing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.IInfoflowSolver;

/**
 * Aliasing strategy to be used for conditionally-called methods when analyzing
 * implicit flows
 * 
 * @author Steven Arzt
 */
public class ImplicitFlowAliasStrategy extends AbstractBulkAliasStrategy {
	
	protected final LoadingCache<SootMethod,Map<AccessPath, Set<AccessPath>>> methodToAliases =
			IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Map<AccessPath, Set<AccessPath>>>() {
				@Override
				public Map<AccessPath, Set<AccessPath>> load(SootMethod method) throws Exception {
					return computeGlobalAliases(method);
				}
			});
	
	public ImplicitFlowAliasStrategy(InfoflowManager manager) {
		super(manager);
	}
    
	/**
	 * Computes the global non-flow-sensitive alias information for the given
	 * method
	 * @param method The method for which to compute the alias information
	 */
	private Map<AccessPath, Set<AccessPath>> computeGlobalAliases(SootMethod method) {
		Map<AccessPath, Set<AccessPath>> res = new HashMap<AccessPath, Set<AccessPath>>();

		// Find the aliases
		for (Unit u : method.getActiveBody().getUnits()) {
			if (!(u instanceof AssignStmt))
				continue;
			final AssignStmt assign = (AssignStmt) u;
			
			// Aliases can only be generated on the heap
			if (!(assign.getLeftOp() instanceof FieldRef
					&& (assign.getRightOp() instanceof FieldRef
							|| assign.getRightOp() instanceof Local)))
				if (!(assign.getRightOp() instanceof FieldRef
						&& (assign.getLeftOp() instanceof FieldRef
								|| assign.getLeftOp() instanceof Local)))
					continue;
			
			final AccessPath apLeft = manager.getAccessPathFactory().createAccessPath(assign.getLeftOp(), true);
			final AccessPath apRight = manager.getAccessPathFactory().createAccessPath(assign.getRightOp(), true);
			
			Set<AccessPath> mapLeft = res.get(apLeft);
			if (mapLeft == null) {
				mapLeft = new HashSet<AccessPath>();
				res.put(apLeft, mapLeft);
			}
			mapLeft.add(apRight);
			
			Set<AccessPath> mapRight = res.get(apRight);
			if (mapRight == null) {
				mapRight = new HashSet<AccessPath>();
				res.put(apRight, mapRight);
			}
			mapLeft.add(apLeft);
		}
		return res;
	}

	@Override
	public void computeAliasTaints(Abstraction d1, Stmt src, Value targetValue,
			Set<Abstraction> taintSet, SootMethod method, Abstraction newAbs) {
		// Use global aliasing
		Value baseValue = ((InstanceFieldRef) targetValue).getBase();
		Set<AccessPath> aliases = methodToAliases.getUnchecked(method).get
				(manager.getAccessPathFactory().createAccessPath(baseValue, true));
		if (aliases != null)
			for (AccessPath ap : aliases) {
				AccessPath newAP = manager.getAccessPathFactory().merge(ap, newAbs.getAccessPath());
				Abstraction aliasAbs = newAbs.deriveNewAbstraction(newAP, null);
				if (taintSet.add(aliasAbs))
					// We have found a new alias. This new base object may however yet
					// again alias with something, so we need to check again
					if (ap.isInstanceFieldRef()) {
						InstanceFieldRef aliasBaseVal = Jimple.v().newInstanceFieldRef
								(ap.getPlainValue(), ap.getFirstField().makeRef());
						computeAliasTaints(d1, src, aliasBaseVal, taintSet, method, aliasAbs);
					}
			}
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
	public boolean hasProcessedMethod(SootMethod method) {
		return methodToAliases.getIfPresent(method) != null;
	}

	@Override
	public IInfoflowSolver getSolver() {
		return null;
	}

	@Override
	public void cleanup() {
		methodToAliases.invalidateAll();
		methodToAliases.cleanUp();
	}
	
}
