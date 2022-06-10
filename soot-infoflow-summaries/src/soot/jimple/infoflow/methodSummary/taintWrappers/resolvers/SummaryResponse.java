package soot.jimple.infoflow.methodSummary.taintWrappers.resolvers;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;

/**
 * Response from a cache that provides flows for a given combination of class
 * and method
 * 
 * @author Steven Arzt
 *
 */
public class SummaryResponse {

	public final static SummaryResponse NOT_SUPPORTED = new SummaryResponse(null, false);
	public final static SummaryResponse EMPTY_BUT_SUPPORTED = new SummaryResponse(null, true);

	private final ClassSummaries classSummaries;
	private final boolean isClassSupported;

	public SummaryResponse(ClassSummaries classSummaries, boolean isClassSupported) {
		this.classSummaries = classSummaries;
		this.isClassSupported = isClassSupported;
	}

	/**
	 * Gets the summaries that the resolver has identified for the given request
	 * 
	 * @return The summaries
	 */
	public ClassSummaries getClassSummaries() {
		return classSummaries;
	}

	/**
	 * Checks whether the class for which the summary was requested is supported,
	 * i.e., summaries for this class are available
	 * 
	 * @return True if summaries are available for this class, false otherwise
	 */
	public boolean isClassSupported() {
		return isClassSupported;
	}

	@Override
	public String toString() {
		if (isClassSupported) {
			if (classSummaries == null)
				return "<Empty summary>";
			else
				return classSummaries.toString();
		} else
			return "<Class not supported>";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classSummaries == null) ? 0 : classSummaries.hashCode());
		result = prime * result + (isClassSupported ? 1231 : 1237);
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
		SummaryResponse other = (SummaryResponse) obj;
		if (classSummaries == null) {
			if (other.classSummaries != null)
				return false;
		} else if (!classSummaries.equals(other.classSummaries))
			return false;
		if (isClassSupported != other.isClassSupported)
			return false;
		return true;
	}

}
