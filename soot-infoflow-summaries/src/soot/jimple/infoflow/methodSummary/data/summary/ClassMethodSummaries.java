package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.HashSet;
import java.util.Set;

/**
 * Class representing the method summaries for a single class
 * 
 * @author Steven Arzt
 *
 */
public class ClassMethodSummaries {

	private final String className;
	private final MethodSummaries methodSummaries;
	private final Set<String> interfaces = new HashSet<>();
	private String superClass;
	private Boolean isInterface = null;

	private boolean isExclusiveForClass = true;

	public ClassMethodSummaries(String className) {
		this.className = className;
		this.methodSummaries = new MethodSummaries();
	}

	public ClassMethodSummaries(String className, MethodSummaries methodSummaries) {
		this.className = className;
		this.methodSummaries = methodSummaries;
	}

	public String getClassName() {
		return className;
	}

	public MethodSummaries getMethodSummaries() {
		return methodSummaries;
	}

	/**
	 * Checks whether there are any flows for any method in this class
	 * 
	 * @return True if there is at least one flow for at least one method in this
	 *         class, false otherwise
	 */
	public boolean isEmpty() {
		return (methodSummaries == null || methodSummaries.isEmpty()) && !hasInterfaces() && !hasSuperclass()
				&& isInterface == null;
	}

	/**
	 * Filters this summary definition to only contain flows for the method with the
	 * given signature
	 * 
	 * @param signature The method signature to filter for
	 * @return A class summary object that only contains flows for the method with
	 *         the given signature
	 */
	public ClassMethodSummaries filterForMethod(String signature) {
		if (isEmpty())
			return null;

		MethodSummaries summaries = methodSummaries.filterForMethod(signature);
		return summaries == null ? null : new ClassMethodSummaries(className, summaries);
	}

	/**
	 * Merges the given method summaries into this data object
	 * 
	 * @param toMerge The method summaries to merge
	 * @return True if new data was added to this summary data object during the
	 *         merge, false otherwise
	 */
	public boolean merge(MethodSummaries toMerge) {
		return methodSummaries.merge(toMerge);
	}

	/**
	 * Validates all summaries for this class
	 */
	public void validate() {
		if (className == null || className.isEmpty())
			throw new RuntimeException("No class name given");
		methodSummaries.validate();
	}

	/**
	 * Merges the given summaries into this class. This operation fails if the two
	 * summary objects refer to different classes.
	 * 
	 * @param methodFlows The summaries to merge into this data object
	 * @return True if new data has been added to this summary as a result of the
	 *         merge, false otherwise
	 */
	public boolean merge(ClassMethodSummaries methodFlows) {
		if (methodFlows == null || methodFlows.isEmpty())
			return false;

		// This summary and the given one must refer to the same class
		String otherClassName = methodFlows.getClassName();
		if (otherClassName == null && this.className != null)
			throw new RuntimeException("Class name mismatch");
		if (otherClassName != null && this.className == null)
			throw new RuntimeException("Class name mismatch");
		if (otherClassName != null && !otherClassName.equals(this.className))
			throw new RuntimeException("Class name mismatch");
		if (hasSuperclass() && methodFlows.hasSuperclass()) {
			if (!superClass.equals(methodFlows.getSuperClass()))
				throw new RuntimeException("Class name mismatch");
		}

		boolean hasNewData = false;
		if (methodFlows.hasSuperclass() && !hasSuperclass()) {
			setSuperClass(methodFlows.getSuperClass());
			hasNewData = true;
		}

		// Merge the summaries
		if (this.methodSummaries.merge(methodFlows.getMethodSummaries()))
			hasNewData = true;

		if (methodFlows.hasInterfaces())
			if (this.interfaces.addAll(methodFlows.getInterfaces()))
				hasNewData = true;

		if (isInterface == null && methodFlows.isInterface != null)
			this.isInterface = methodFlows.isInterface;

		return hasNewData;
	}

	/**
	 * Gets whether there are any clears (kill taints) inside this summary object
	 * 
	 * @return True if there are any clears (kill taints) inside this summary
	 *         object, otherwise false
	 */
	public boolean hasClears() {
		return methodSummaries.hasClears();
	}

	/**
	 * Gets a set containing all clears (kill taints) in this summary object
	 * regardless of the method they are in
	 * 
	 * @return A flat set of all clears (kill taints) contained in this summary
	 *         object
	 */
	public Set<MethodClear> getAllClears() {
		return methodSummaries.getAllClears();
	}

	/**
	 * Gets the total number of flows in this summary object
	 * 
	 * @return The total number of flows in this summary object
	 */
	public int getFlowCount() {
		return methodSummaries.getFlowCount();
	}

	/**
	 * Gets whether this model is exclusive for the class, even if it does not
	 * contain all methods. In other words, this defines whether we shall explicitly
	 * ignore all methods not modeled in this summary.
	 * 
	 * @return True if this model is exclusive for the class, false otherwise
	 */
	public boolean isExclusiveForClass() {
		return isExclusiveForClass;
	}

	/**
	 * Sets whether this model is exclusive for the class, even if it does not
	 * contain all methods. In other words, this defines whether we shall explicitly
	 * ignore all methods not modeled in this summary.
	 * 
	 * @param isExclusiveForClass True if this model is exclusive for the class,
	 *                            false otherwise
	 */
	public void setExclusiveForClass(boolean isExclusiveForClass) {
		this.isExclusiveForClass = isExclusiveForClass;
	}

	/**
	 * Gets the interfaces implemented by this class
	 * 
	 * @return The interfaces implemented by this class
	 */
	public Set<String> getInterfaces() {
		return interfaces;
	}

	/**
	 * Adds an interface that is implemented by this class
	 * 
	 * @param className The name of the interface
	 */
	public void addInterface(String className) {
		interfaces.add(className);
	}

	/**
	 * Gets whether this summary object has any interfaces that are implemented by
	 * the target class
	 * 
	 * @return True if we know about the interfaces implemented by the target class,
	 *         false otherwise
	 */
	public boolean hasInterfaces() {
		return interfaces != null && !interfaces.isEmpty();
	}

	/**
	 * Gets whether this summary object contains a reference to the superclass of
	 * the target class
	 * 
	 * @return True if we know the superclass of the target class, false otherwise
	 */
	public boolean hasSuperclass() {
		return superClass != null && !superClass.isEmpty();
	}

	@Override
	public String toString() {
		return String.format("Summaries for %s", className);
	}

	/**
	 * Gets the super class from which this class is derived
	 * 
	 * @return The super class of this class
	 */
	public String getSuperClass() {
		return superClass;
	}

	/**
	 * Sets the super class from which this class is derived
	 * 
	 * @param superClass The super class of this class
	 */
	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	/**
	 * Gets whether this summary models an interface
	 * 
	 * @return True if this summary models an interface, false if it models a normal
	 *         class
	 */
	public boolean isInterface() {
		return isInterface != null && isInterface.booleanValue();
	}

	/**
	 * Gets whether this summary holds information about whether the target class is
	 * an interface or not
	 * 
	 * @return True if interface status data is available, false otherwise
	 */
	public boolean hasInterfaceInfo() {
		return isInterface != null;
	}

	/**
	 * Sets whether this summary models an interface
	 * 
	 * @param isInterface True if this summary models an interface, false if it
	 *                    models a normal class
	 */
	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((interfaces == null) ? 0 : interfaces.hashCode());
		result = prime * result + (isExclusiveForClass ? 1231 : 1237);
		result = prime * result + ((isInterface == null) ? 0 : isInterface.hashCode());
		result = prime * result + ((methodSummaries == null) ? 0 : methodSummaries.hashCode());
		result = prime * result + ((superClass == null) ? 0 : superClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClassMethodSummaries other = (ClassMethodSummaries) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (interfaces == null) {
			if (other.interfaces != null)
				return false;
		} else if (!interfaces.equals(other.interfaces))
			return false;
		if (isExclusiveForClass != other.isExclusiveForClass)
			return false;
		if (isInterface == null) {
			if (other.isInterface != null)
				return false;
		} else if (!isInterface.equals(other.isInterface))
			return false;
		if (methodSummaries == null) {
			if (other.methodSummaries != null)
				return false;
		} else if (!methodSummaries.equals(other.methodSummaries))
			return false;
		if (superClass == null) {
			if (other.superClass != null)
				return false;
		} else if (!superClass.equals(other.superClass))
			return false;
		return true;
	}

}
