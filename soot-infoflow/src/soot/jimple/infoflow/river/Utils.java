package soot.jimple.infoflow.river;

import java.util.Iterator;
import java.util.Set;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

public class Utils {

	/**
	 * Checks whether the given taint is visible inside the method called at the
	 * given call site
	 * 
	 * @param stmt     A call site where a sink method is called
	 * @param source   The taint that has arrived at the given statement
	 * @param aliasing The aliasing strategy
	 * @return True if the callee has access to the tainted value, false otherwise
	 */
	public static boolean isTaintVisibleInCallee(Stmt stmt, Abstraction source, Aliasing aliasing) {
		InvokeExpr iexpr = stmt.getInvokeExpr();

		// Is an argument tainted?
		final Value apBaseValue = source.getAccessPath().getPlainValue();
		if (apBaseValue != null && aliasing != null) {
			for (int i = 0; i < iexpr.getArgCount(); i++) {
				if (aliasing.mayAlias(iexpr.getArg(i), apBaseValue)) {
					if (source.getAccessPath().getTaintSubFields() || source.getAccessPath().isLocal())
						return true;
				}
			}
		}

		// Is the base object tainted?
		if (iexpr instanceof InstanceInvokeExpr) {
			if (((InstanceInvokeExpr) iexpr).getBase() == source.getAccessPath().getPlainValue())
				return true;
		}

		// Is return tainted?
		if (stmt instanceof AssignStmt && aliasing != null
				&& aliasing.mayAlias(apBaseValue, ((AssignStmt) stmt).getLeftOp()))
			return true;

		return false;
	}

	/**
	 * Check whether baseLocal is tainted in the outgoing set. Assumes baseLocal is
	 * an object and the check happens at a call site.
	 *
	 * @param outgoing  outgoing taint set
	 * @param baseLocal base local
	 * @return corresponding abstraction if baseLocal is tainted else null
	 */
	public static Abstraction getTaintFromLocal(Set<Abstraction> outgoing, Value baseLocal) {
		for (Abstraction abs : outgoing)
			if (abs.getAccessPath().getPlainValue() == baseLocal)
				return abs;

		return null;
	}

	/**
	 * Check whether the access path is read at unit.
	 *
	 * @param unit unit
	 * @param ap   access path
	 * @return true if ap is read at unit
	 */
	public static boolean isReadAt(Unit unit, AccessPath ap) {
		Iterator<ValueBox> it = unit.getUseBoxesIterator();
		while (it.hasNext())
			if (it.next().getValue() == ap.getPlainValue())
				return true;

		return false;
	}

}
