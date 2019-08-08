package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Collection;

import soot.util.MultiMap;

/**
 * Immutable version of the {@link MethodSummaries} class
 * 
 * @author Steven Arzt
 *
 */
public class ImmutableMethodSummaries extends MethodSummaries {

	@Override
	public boolean addClear(MethodClear clear) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public boolean addFlow(MethodFlow flow) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void clear() {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public GapDefinition createTemporaryGap(int gapID) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public boolean merge(MethodSummaries newFlows) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void merge(MultiMap<String, MethodFlow> newFlows) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void mergeClears(Collection<MethodClear> newClears) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void mergeFlows(Collection<MethodFlow> newFlows) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void mergeSummaries(Collection<MethodSummaries> newSummaries) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void remove(MethodFlow toRemove) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public void removeAll(Collection<MethodFlow> toRemove) {
		throw new RuntimeException("This object is immutable");
	}

	@Override
	public boolean removeGap(GapDefinition gap) {
		throw new RuntimeException("This object is immutable");
	}

}
