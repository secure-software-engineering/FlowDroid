package soot.jimple.infoflow.methodSummary.data.provider;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ConcurrentClassSummaries;

/**
 * Concurrent version of the {@link MergingSummaryProvider} class
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentMergingSummaryProvider extends MergingSummaryProvider {

	@Override
	protected ClassSummaries createClassSummaries() {
		return new ConcurrentClassSummaries();
	}

}
