package soot.jimple.infoflow.collections.taintWrappers;

import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.MergingSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;

import java.util.List;
import java.util.Set;

/**
 * Summary Provider that holds multiple summary providers and returns the first match
 *
 * @author Tim Lange
 */
public class PrioritizingMethodSummaryProvider extends MergingSummaryProvider {
    public PrioritizingMethodSummaryProvider(List<IMethodSummaryProvider> innerProviders) {
        super(innerProviders);
    }

    @Override
    public ClassMethodSummaries getMethodFlows(String className, String methodSignature) {
        for (IMethodSummaryProvider provider : innerProviders) {
            ClassMethodSummaries providerSummaries = provider.getMethodFlows(className, methodSignature);
            if (providerSummaries != null)
                return providerSummaries;
        }
        return null;
    }

    @Override
    public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
        for (IMethodSummaryProvider provider : innerProviders) {
            ClassSummaries providerSummaries = provider.getMethodFlows(classes, methodSignature);
            if (providerSummaries != null)
                return providerSummaries;
        }
        return null;
    }

    @Override
    public ClassMethodSummaries getClassFlows(String clazz) {
        for (IMethodSummaryProvider provider : innerProviders) {
            ClassMethodSummaries providerSummaries = provider.getClassFlows(clazz);
            if (providerSummaries != null)
                return providerSummaries;
        }
        return null;
    }

    @Override
    public boolean isMethodExcluded(String className, String subSignature) {
        for (IMethodSummaryProvider provider : innerProviders) {
            ClassMethodSummaries summaries = provider.getClassFlows(className);
            if (summaries != null)
                return summaries.getMethodSummaries().isExcluded(subSignature);
        }
        return false;
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
                for (ClassMethodSummaries s : providerSummaries.getAllSummaries()) {
                    if (!summaries.hasSummariesForClass(s.getClassName()))
                        summaries.merge(s);
                }
            }
        }
        this.cachedSummaries = summaries;
        return summaries;
    }
}
