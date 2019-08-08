package soot.jimple.infoflow.methodSummary.data.provider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;

/**
 * Summary provider for combining summaries from different sources
 * 
 * @author Steven Arzt
 *
 */
public class MergingSummaryProvider implements IMethodSummaryProvider {

	private final Collection<IMethodSummaryProvider> innerProviders;

	public MergingSummaryProvider(Collection<IMethodSummaryProvider> innerProviders) {
		this.innerProviders = innerProviders;
	}

	@Override
	public Set<String> getLoadableClasses() {
		Set<String> loadableClasses = new HashSet<>();
		for (IMethodSummaryProvider provider : innerProviders) {
			Set<String> curClasses = provider.getLoadableClasses();
			if (curClasses != null && !curClasses.isEmpty())
				loadableClasses.addAll(curClasses);
		}
		return loadableClasses;
	}

	@Override
	public Set<String> getSupportedClasses() {
		Set<String> supportedClasses = new HashSet<>();
		for (IMethodSummaryProvider provider : innerProviders) {
			Set<String> curClasses = provider.getSupportedClasses();
			if (curClasses != null && !curClasses.isEmpty())
				supportedClasses.addAll(curClasses);
		}
		return supportedClasses;
	}

	@Override
	public boolean supportsClass(String clazz) {
		for (IMethodSummaryProvider provider : innerProviders) {
			if (provider.supportsClass(clazz))
				return true;
		}
		return false;
	}

	@Override
	public ClassMethodSummaries getMethodFlows(String className, String methodSignature) {
		ClassMethodSummaries summaries = null;
		for (IMethodSummaryProvider provider : innerProviders) {
			ClassMethodSummaries providerSummaries = provider.getMethodFlows(className, methodSignature);
			if (providerSummaries != null) {
				if (summaries == null)
					summaries = new ClassMethodSummaries(className);
				summaries.merge(providerSummaries);
			}
		}
		return summaries;
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		ClassSummaries summaries = null;
		for (IMethodSummaryProvider provider : innerProviders) {
			ClassSummaries providerSummaries = provider.getMethodFlows(classes, methodSignature);
			if (providerSummaries != null) {
				if (summaries == null)
					summaries = new ClassSummaries();
				summaries.merge(providerSummaries);
			}
		}
		return summaries;
	}

	@Override
	public ClassMethodSummaries getClassFlows(String clazz) {
		ClassMethodSummaries summaries = null;
		for (IMethodSummaryProvider provider : innerProviders) {
			ClassMethodSummaries providerSummaries = provider.getClassFlows(clazz);
			if (providerSummaries != null) {
				if (summaries == null)
					summaries = new ClassMethodSummaries(clazz);
				summaries.merge(providerSummaries);
			}
		}
		return summaries;
	}

	@Override
	public boolean mayHaveSummaryForMethod(String subsig) {
		for (IMethodSummaryProvider provider : innerProviders) {
			if (provider.mayHaveSummaryForMethod(subsig))
				return true;
		}
		return false;
	}

	/**
	 * Gets the individual providers that this merging provider queries. Changes to
	 * this collection will directly be reflected in the merging provider.
	 * 
	 * @return The individual providers that this merging provider queries
	 */
	public Collection<IMethodSummaryProvider> getInnerProviders() {
		return innerProviders;
	}

	@Override
	public ClassSummaries getSummaries() {
		ClassSummaries summaries = null;
		for (IMethodSummaryProvider provider : innerProviders) {
			ClassSummaries providerSummaries = provider.getSummaries();
			if (providerSummaries != null) {
				if (summaries == null)
					summaries = new ClassSummaries();
				summaries.merge(providerSummaries);
			}
		}
		return summaries;
	}

}
