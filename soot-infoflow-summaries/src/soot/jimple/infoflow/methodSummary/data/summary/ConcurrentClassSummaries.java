package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent version of the {@link ClassSummaries} class
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentClassSummaries extends ClassSummaries {

	@Override
	protected Map<String, ClassMethodSummaries> createSummariesMap() {
		return new ConcurrentHashMap<>();
	}

}
