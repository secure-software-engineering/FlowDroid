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
public class MergingSummaryProvider extends AbstractMethodSummaryProvider {

	protected final Collection<IMethodSummaryProvider> innerProviders;
	protected ClassSummaries cachedSummaries;

	protected MergingSummaryProvider() {
		this.innerProviders = new HashSet<>();
	}

	public MergingSummaryProvider(Collection<IMethodSummaryProvider> innerProviders) {
		this.innerProviders = innerProviders;
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
					summaries = createClassSummaries();
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
		ClassSummaries summaries = cachedSummaries;
		if (summaries != null)
			return summaries;
		for (IMethodSummaryProvider provider : innerProviders) {
			ClassSummaries providerSummaries = provider.getSummaries();
			if (providerSummaries != null) {
				if (summaries == null)
					summaries = createClassSummaries();
				summaries.merge(providerSummaries);
			}
		}
		this.cachedSummaries = summaries;
		return summaries;
	}

	/**
	 * Creates the {@link ClassSummaries} object to use for merging individual
	 * summaries
	 * 
	 * @return The new {@link ClassSummaries} object
	 */
	protected ClassSummaries createClassSummaries() {
		return new ClassSummaries();
	}

	@Override
	public boolean isMethodExcluded(String className, String subSignature) {
		for (IMethodSummaryProvider provider : innerProviders) {
			if (provider.isMethodExcluded(className, subSignature))
				return true;
		}
		return false;
	}

	@Override
	public Set<String> getAllClassesWithSummaries() {
		Set<String> classes = new HashSet<>();
		for (IMethodSummaryProvider provider : innerProviders) {
			Set<String> curClasses = provider.getAllClassesWithSummaries();
			if (curClasses != null && !curClasses.isEmpty())
				classes.addAll(curClasses);
		}
		return classes;
	}

}
