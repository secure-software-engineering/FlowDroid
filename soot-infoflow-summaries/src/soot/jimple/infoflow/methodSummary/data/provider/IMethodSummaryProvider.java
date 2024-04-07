package soot.jimple.infoflow.methodSummary.data.provider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;

/**
 * Common interface for all classes that can provide method summaries
 * 
 * @author Steven Arzt
 *
 */
public interface IMethodSummaryProvider {

	/**
	 * Gets the names of all classes for which this provider can directly return
	 * method summaries
	 * 
	 * @return The set of classes for which summaries have been loaded
	 */
	public Set<String> getSupportedClasses();

	/**
	 * Gets the names of all classes for which this provider can obtain method
	 * summaries, either directly, or via lazy loading
	 * 
	 * @return The names of all classes for which this provider can obtain method
	 *         summaries
	 */
	public Set<String> getAllClassesWithSummaries();

	/**
	 * Gets whether the given class is supported by this provider, i.e., whether
	 * summaries for that class have either already been loaded or can be loaded
	 * 
	 * @param clazz The class to check
	 * @return True if the given class is supported b this provider, otherwise false
	 */
	public boolean supportsClass(String clazz);

	/**
	 * Gets the data flows inside the given method for the given class. The
	 * different overloads are functionally equivalent, but may use the different
	 * types in order to be faster
	 * 
	 * @param sootClass          The class containing the method. If two classes B
	 *                           and C inherit from some class A, methods in A can
	 *                           either be evaluated in the context of B or C.
	 * @param methodSubSignature The signature of the method for which to get the
	 *                           flow summaries.
	 * @return The flow summaries for the given method in the given class
	 */
	public default ClassMethodSummaries getMethodFlows(SootClass sootClass, String methodSubSignature) {
		return getMethodFlows(sootClass.getName(), methodSubSignature);
	}

	/**
	 * Gets the data flows inside the given method for the given class. The
	 * different overloads are functionally equivalent, but may use the different
	 * types in order to be faster
	 * 
	 * @param className       The class containing the method. If two classes B and
	 *                        C inherit from some class A, methods in A can either
	 *                        be evaluated in the context of B or C.
	 * @param methodSignature The signature of the method for which to get the flow
	 *                        summaries.
	 * @return The flow summaries for the given method in the given class
	 */
	public ClassMethodSummaries getMethodFlows(String className, String methodSignature);

	/**
	 * Gets the data flows inside the given method immediately for the class in
	 * which it is defined.
	 * 
	 * @param method The method for which to get the summaries
	 * @return The flow summaries for the given method
	 */
	public default ClassMethodSummaries getMethodFlows(SootMethod method) {
		return getMethodFlows(method.getDeclaringClass(), method.getSubSignature());
	}

	/**
	 * Gets all flows for the given method signature in the given set of classes
	 * 
	 * @param classes         The classes in which to look for flow summaries
	 * @param methodSignature The signature of the method for which to get the flow
	 *                        summaries
	 * @return The flow summaries for the given method in the given set of classes
	 */
	public ClassSummaries getMethodFlows(Set<String> classes, String methodSignature);

	/**
	 * Gets all summaries for all methods in the given class
	 * 
	 * @param clazz The class for which to get all method summaries
	 * @return All method summaries for all methods in the given class
	 */
	public ClassMethodSummaries getClassFlows(String clazz);

	/**
	 * Returns true iff there exists a summary for a given subsignature
	 * 
	 * @param subsig the sub signature
	 * @return true iff there exists a summary for a given subsignature
	 */
	public boolean mayHaveSummaryForMethod(String subsig);

	/**
	 * Gets all summaries that have been loaded so far. Note that this object may
	 * not include all lazily loaded summaries yet.
	 * 
	 * @return The summaries that have been loaded so far
	 */
	public ClassSummaries getSummaries();

	/**
	 * Gets whether the given method has been excluded from the data flow analysis
	 * 
	 * @param className    The name of the class that contains the method to check
	 * @param subSignature The method to check
	 * @return True if the given method has been excluded from the data flow
	 *         analysis, false otherwise
	 */
	public boolean isMethodExcluded(String className, String subSignature);

	/**
	 * Gets all superclasses of the given class, where the first entry in the list
	 * is the parent of the given class
	 * 
	 * @param className The name of the class for which to get the superclasses
	 * @return A list with all superclasses of the given class
	 */
	public List<String> getSuperclassesOf(String className);

	/**
	 * Gets all super interfaces of the given interface, i.e., the interfaces
	 * implemented by the given class, and recursively for each interface the
	 * interfaces implemented by it
	 * 
	 * @param className The class name of the interface to start from
	 * @return The super interfaces of the given interface
	 */
	public Collection<String> getSuperinterfacesOf(String className);

	/**
	 * Gets all subclasses of the given class
	 * 
	 * @param className The name of the class for which to get the subclasses
	 * @return The transitive subclasses of the given class
	 */
	public List<String> getSubclassesOf(String className);

	/**
	 * Gets all classes that implement the given interface
	 * 
	 * @param className The class name of the interface for which to get the
	 *                  implementers
	 * @return The classes that implement the given interface
	 */
	public List<String> getImplementersOfInterface(String className);

	/**
	 * Gets all interfaces that are derived from the given interface
	 * 
	 * @param className The class name of the interface from which to start
	 * @return The interfaces that transitively implement the given interface
	 */
	public Set<String> getSubInterfacesOf(String className);

}
