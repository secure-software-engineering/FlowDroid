package soot.jimple.infoflow.methodSummary.generator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.SootMethod;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Class that manages the creation of gaps during the taint propagation and the
 * summary generation
 * 
 * @author Steven Arzt
 *
 */
public class GapManager {

	private final Map<Stmt, GapDefinition> gaps = new HashMap<Stmt, GapDefinition>();
	private int lastGapID = 0;
	
	/**
	 * Gets the data object of the given call into a gap method. If no gap
	 * definition exists, a new one is created.
	 * @param flows The flow set in which to register the gap
	 * @param gapCall The gap to be called
	 * @return The data object of the given gap call. If this call site has
	 * already been processed, the old object is returned. Otherwise, a new
	 * object is generated.
	 */
	public synchronized GapDefinition getOrCreateGapForCall(
			MethodSummaries flows, Stmt gapCall) {
		GapDefinition gd = this.gaps.get(gapCall);
		if (gd == null) {
			// Generate a new gap ID
			// Register it in the summary object
			gd = flows.getOrCreateGap(lastGapID++,
					gapCall.getInvokeExpr().getMethod().getSignature());
			this.gaps.put(gapCall, gd);
		}
		return gd;
	}

	/**
	 * Gets the data object of the given call into a gap method
	 * @param gapCall The gap to be called
	 * @return The data object of the given gap call if it exists, otherwise
	 * null
	 */
	public GapDefinition getGapForCall(Stmt gapCall) {
		return this.gaps.get(gapCall);
	}
	
	/**
	 * Gets whether the given local is referenced in any gap. This can either be
	 * as a parameter, a base object, or a return value
	 * @param local The local to check
	 * @return True if the given local is referenced in at least one gap,
	 * otherwise false
	 */
	public synchronized boolean isLocalReferencedInGap(Local local) {
		for (Stmt stmt : gaps.keySet()) {
			for (ValueBox vb : stmt.getUseBoxes())
				if (vb.getValue() == local)
					return true;
			if (stmt instanceof DefinitionStmt)
				if (((DefinitionStmt) stmt).getLeftOp() == local)
					return true;
		}
		return false;
	}
	
	/**
	 * Gets the gap definitions that references the given local as parameters
	 * or base objects 
	 * @param local The local for which to find the gap references
	 * @return The gaps that reference the given local
	 */
	public Set<GapDefinition> getGapDefinitionsForLocalUse(Local local) {
		Set<GapDefinition> res = null;
		stmt : for (Stmt stmt : gaps.keySet()) {
			for (ValueBox vb : stmt.getUseBoxes())
				if (vb.getValue() == local) {
					if (res == null)
						res = new HashSet<>();
					res.add(gaps.get(stmt));
					continue stmt;
				}
		}
		return res;
 	}
	
	/**
	 * Gets the gap definitions that references the given local as return value. 
	 * @param local The local for which to find the gap references
	 * @return The gaps that reference the given local
	 */
	public Set<GapDefinition> getGapDefinitionsForLocalDef(Local local) {
		Set<GapDefinition> res = null;
		stmt : for (Stmt stmt : gaps.keySet()) {
			if (stmt instanceof DefinitionStmt)
				if (((DefinitionStmt) stmt).getLeftOp() == local) {
					if (res == null)
						res = new HashSet<>();
					res.add(gaps.get(stmt));
					continue stmt;
				}
		}
		return res;
 	}
	
	/**
	 * Checks whether we need to produce a gap for the given method call
	 * @param stmt The call statement
	 * @param abs The abstraction that reaches the given call
	 * @param icfg The interprocedural control flow graph
	 * @return True if we need to create a gap, otherwise false
	 */
	public boolean needsGapConstruction(Stmt stmt, Abstraction abs, IInfoflowCFG icfg) {
		SootMethod targetMethod = stmt.getInvokeExpr().getMethod();
		
		// Do not report inactive flows into gaps
		if (!abs.isAbstractionActive())
			return false;
		
		// If the callee is native, there is no need for a gap
		if (targetMethod.isNative())
			return false;
		
		// Do not construct a gap for constructors or static initializers
		if (targetMethod.isConstructor() || targetMethod.isStaticInitializer())
			return false;
		
		// Do not produce flows from one statement to itself
		if (abs.getSourceContext() != null
				&& abs.getSourceContext().getStmt() == stmt)
			return false;
		
		// Is the given incoming value even used in the gap statement?
		if (!isValueUsedInStmt(stmt, abs))
			return false;
		
		// We always construct a gap if we have no callees
		SootMethod sm = icfg.getMethodOf(stmt);
		Collection<SootMethod> callees = icfg.getCalleesOfCallAt(stmt);
		if (callees != null && !callees.isEmpty()) {
			// If we have a call to an abstract method, this might instead of an 
			// empty callee list give  us one with a self-loop. Semantically, this
			// is however still an unknown callee.
			if (!(callees.size() == 1 && callees.contains(sm)
					&& stmt.getInvokeExpr().getMethod().isAbstract())) {
				return false;
			}
		}
		
		// Do not build gap flows for the java.lang.System class
		if (sm.getDeclaringClass().getName().equals("java.lang.System"))
			return false;
		if (targetMethod.getDeclaringClass().getName().startsWith("sun."))
			return false;
		
		// We create a gap
		return true;
	}
	
	/**
	 * Checks whether the given value is used in the given statement
	 * @param stmt The statement to check
	 * @param abs The value to check
	 * @return True if the given value is used in the given statement, otherwise
	 * false
	 */
	private boolean isValueUsedInStmt(Stmt stmt, Abstraction abs) {
		if (!stmt.containsInvokeExpr())
			return false;
		InvokeExpr iexpr = stmt.getInvokeExpr();
		
		// If this value is a parameter, we take it
		for (int i = 0; i < iexpr.getArgCount(); i++)
			if (abs.getAccessPath().getPlainValue() == iexpr.getArg(i))
				return true;
		
		// If this is the base local, we take it
		return iexpr instanceof InstanceInvokeExpr
				&& ((InstanceInvokeExpr) iexpr).getBase() == abs.getAccessPath().getPlainValue();
	}
	
}
