package soot.jimple.infoflow.methodSummary.data.provider;

import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

/**
 * Common interface for all classes that can provide method summaries
 * 
 * @author Steven Arzt
 *
 */
public interface IMethodSummaryProvider {

	/**
	 * Gets the names of all classes for which summaries have not yet been loaded,
	 * but are available on some external storage
	 * 
	 * @return The set of classes for which summaries can be loaded
	 */
	public Set<String> getLoadableClasses();

	/**
	 * Gets the names of all classes for which this provider can directly return
	 * method summaries
	 * 
	 * @return The set of classes for which summaries have been loaded
	 */
	public Set<String> getSupportedClasses();

	/**
	 * Gets whether the given class is supported by this provider, i.e., whether
	 * summaries for that class have either already been loaded or can be loaded
	 * 
	 * @param clazz
	 *            The class to check
	 * @return True if the given class is supported b this provider, otherwise false
	 */
	public boolean supportsClass(String clazz);

	/**
	 * Gets the data flows inside the given method for the given class.
	 * 
	 * @param className
	 *            The class containing the method. If two classes B and C inherit
	 *            from some class A, methods in A can either be evaluated in the
	 *            context of B or C.
	 * @param methodSignature
	 *            The signature of the method for which to get the flow summaries.
	 * @return The flow summaries for the given method in the given class
	 */
	public MethodSummaries getMethodFlows(String className, String methodSubSignature);

	/**
	 * Gets all flows for the given method signature in the given set of classes
	 * 
	 * @param classes
	 *            The classes in which to look for flow summaries
	 * @param methodSignature
	 *            The signature of the method for which to get the flow summaries
	 * @return The flow summaries for the given method in the given set of classes
	 */
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature);

	/**
	 * Gets all summaries for all methods in the given class
	 * 
	 * @param clazz
	 *            The class for which to get all method summaries
	 * @return All method summaries for all methods in the given class
	 */
	public MethodSummaries getClassFlows(String clazz);

}
