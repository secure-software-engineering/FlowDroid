package soot.jimple.infoflow.methodSummary.taintWrappers.resolvers;

import soot.SootClass;

/**
 * Query that retrieves summaries for a given class and method.
 * 
 * @author Steven Arzt
 *
 */
public class SummaryQuery {

	public final SootClass calleeClass;
	public final SootClass declaredClass;
	public final String subsignature;

	public SummaryQuery(SootClass calleeClass, SootClass declaredClass, String subsignature) {
		this.calleeClass = calleeClass;
		this.declaredClass = declaredClass;
		this.subsignature = subsignature;
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
