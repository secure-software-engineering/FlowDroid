package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Set;

/**
 * Immutable variant of the {@link ClassSummaries} class
 * 
 * @author Steven Arzt
 *
 */
public class ImmutableClassSummaries extends ClassSummaries {

	@Override
	public boolean addDependency(String className) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void clear() {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void merge(ClassSummaries summaries) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void merge(String className, MethodSummaries newSums) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void merge(String className, Set<MethodFlow> newSums) {
		throw new RuntimeException("This object is immutable");
	}

}
