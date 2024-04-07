package soot.jimple.infoflow.methodSummary.taintWrappers.resolvers;

import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

/**
 * Query that retrieves summaries for a given class and method.
 * 
 * @author Steven Arzt
 *
 */
public class SummaryQuery {

	/**
	 * The actual class, we search first for this class
	 */
	public final SootClass calleeClass;
	/**
	 * Fallback class (declared class type)
	 */
	public final SootClass declaredClass;
	public final String subsignature;

	public SummaryQuery(SootClass calleeClass, SootClass declaredClass, String subsignature) {
		this.calleeClass = calleeClass;
		this.declaredClass = declaredClass;
		this.subsignature = subsignature;
	}

	/**
	 * Creates a new summary query from a given call site. This method can be used
	 * to query summaries for all potential callees of the given call site.
	 * 
	 * @param stmt The call site
	 * @return The summary query that corresponds to the given call site
	 */
	public static SummaryQuery fromStmt(Stmt stmt) {
		if (stmt != null && stmt.containsInvokeExpr()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			SootClass declaredClass = null;
			if (iexpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) iexpr;
				Type baseType = iiexpr.getBase().getType();
				if (baseType instanceof RefType)
					declaredClass = ((RefType) baseType).getSootClass();
			}
			SootMethod callee = iexpr.getMethod();
			SootClass calleeClass = callee.getDeclaringClass();
			return new SummaryQuery(calleeClass, declaredClass, callee.getSubSignature());
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(calleeClass.getName());
		sb.append(": ");
		sb.append(subsignature);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calleeClass == null) ? 0 : calleeClass.hashCode());
		result = prime * result + ((declaredClass == null) ? 0 : declaredClass.hashCode());
		result = prime * result + ((subsignature == null) ? 0 : subsignature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SummaryQuery other = (SummaryQuery) obj;
		if (calleeClass == null) {
			if (other.calleeClass != null)
				return false;
		} else if (!calleeClass.equals(other.calleeClass))
			return false;
		if (declaredClass == null) {
			if (other.declaredClass != null)
				return false;
		} else if (!declaredClass.equals(other.declaredClass))
			return false;
		if (subsignature == null) {
			if (other.subsignature != null)
				return false;
		} else if (!subsignature.equals(other.subsignature))
			return false;
		return true;
	}

}
