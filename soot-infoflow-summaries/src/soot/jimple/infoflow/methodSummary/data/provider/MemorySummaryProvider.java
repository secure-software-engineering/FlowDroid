package soot.jimple.infoflow.methodSummary.data.provider;

import java.util.Collections;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

/**
 * Provider class that reads method summaries from in-memory data structures
 * 
 * @author Steven Arzt
 *
 */
public class MemorySummaryProvider implements IMethodSummaryProvider {

	private final ClassSummaries summaries;

	/**
	 * Creates a new instance of the MemorySummaryProvider class
	 * 
	 * @param summaries
	 *            The summaries to provide to the taint wrapper
	 */
	public MemorySummaryProvider(ClassSummaries summaries) {
		this.summaries = summaries;
	}

	@Override
	public Set<String> getLoadableClasses() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getSupportedClasses() {
		return summaries.getClasses();
	}

	@Override
	public boolean supportsClass(String clazz) {
		return summaries.hasSummariesForClass(clazz);
	}

	@Override
	public MethodSummaries getMethodFlows(String className, String methodSubSignature) {
		MethodSummaries methodSummaries = summaries.getClassSummaries(className);
		if (methodSummaries == null)
			return null;
		return methodSummaries.filterForMethod(methodSubSignature);
	}

	@Override
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature) {
		return summaries.filterForMethod(classes, methodSignature);
	}

	@Override
	public MethodSummaries getClassFlows(String clazz) {
		return summaries.getClassSummaries(clazz);
	}

}
