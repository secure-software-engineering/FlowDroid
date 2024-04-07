package soot.jimple.infoflow.methodSummary.generator;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

/**
 * Handler that gets invoked when summaries have been generated for the methods
 * inside a class.
 * 
 * @author Steven Arzt
 */
public interface IClassSummaryHandler {

	/**
	 * Callback that is invoked before summaries for the given class are created.
	 * Implementers can use this callback to skip over certain classes.
	 * 
	 * @param className
	 * @return false to skip, true to analyze
	 */
	public boolean onBeforeAnalyzeClass(String className);

	/**
	 * Callback that is invoked when a methods inside a class has been summarized
	 * 
	 * @param methodSignature The signature of the method that has been summarized
	 * @param summaries       The method summary
	 */
	public void onMethodFinished(String methodSignature, MethodSummaries summaries);

	/**
	 * Callback that is invoked when all methods inside a class have been summarized
	 * 
	 * @param summaries The method summaries
	 */
	public void onClassFinished(ClassMethodSummaries summaries);

}
