package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data class encapsulating all method summaries for a certain class
 * 
 * @author Steven Arzt
 *
 */
public class ClassSummaries {

	public static final ClassSummaries EMPTY_SUMMARIES = new ImmutableClassSummaries();

	private final Map<String, ClassMethodSummaries> summaries;
	private final Set<String> dependencies = new HashSet<>();
	private SummaryMetaData metaData = null;

	/**
	 * Creates a new instance of the ClassSummaries class
	 */
	public ClassSummaries() {
		summaries = createSummariesMap();
	}

	/**
	 * Creates the map for storing the summaries
	 * 
	 * @return The map for storing the summaries
	 */
	protected Map<String, ClassMethodSummaries> createSummariesMap() {
		return new HashMap<>();
	}

	/**
	 * Gets the flow summaries for the given class
	 * 
	 * @return The flow summaries for the given class
	 */
	public ClassMethodSummaries getClassSummaries(String className) {
		return summaries.get(className);
	}

	/**
	 * Gets or creates a summary object for the given class
	 * 
	 * @param className The name of the class for which to get a summary object
	 * @return The summary object for the class with the given name
	 */
	public ClassMethodSummaries getOrCreateClassSummaries(String className) {
		return summaries.computeIfAbsent(className, n -> new ClassMethodSummaries(className));
	}

	/**
	 * Gets the flow summaries for the methods in the given class
	 * 
	 * @return The flow summaries for the methods in the given class
	 */
	public MethodSummaries getMethodSummaries(String className) {
		ClassMethodSummaries cms = summaries.get(className);
		if (cms == null)
			return null;

		return cms.getMethodSummaries();
	}

	/**
	 * Gets all method summaries for all classes in this data object
	 * 
	 * @return A set containing all method summaries in this data object
	 */
	public Collection<ClassMethodSummaries> getAllSummaries() {
		return this.summaries.values();
	}

	/**
	 * Gets all method summaries for all classes in this data object
	 * 
	 * @return A set containing all method summaries in this data object
	 */
	public Collection<MethodSummaries> getAllMethodSummaries() {
		return this.summaries.values().stream().map(v -> v.getMethodSummaries()).collect(Collectors.toSet());
	}

	/**
	 * Gets all flows for the method with the given signature, regardless of the
	 * class for which they are defined
	 * 
	 * @param signature The signature of the method for which to get the flows
	 * @return The union of all flows in methods with the given signature over all
	 *         classes
	 */
	public Set<MethodFlow> getAllFlowsForMethod(String signature) {
		Set<MethodFlow> flows = new HashSet<>();
		for (String className : this.summaries.keySet()) {
			ClassMethodSummaries classSummaries = this.summaries.get(className);
			if (classSummaries != null) {
				Set<MethodFlow> methodFlows = classSummaries.getMethodSummaries().getFlowsForMethod(signature);
				if (methodFlows != null && !methodFlows.isEmpty())
					flows.addAll(methodFlows);
			}
		}
		return flows;
	}

	/**
	 * Gets all summaries for the method with the given signature, regardless of the
	 * class for which they are defined
	 * 
	 * @param signature The signature of the method for which to get the flows
	 * @return The union of all flows in methods with the given signature over all
	 *         classes
	 */
	public MethodSummaries getAllSummariesForMethod(String signature) {
		MethodSummaries summaries = new MethodSummaries();
		for (String className : this.summaries.keySet()) {
			ClassMethodSummaries classSummaries = this.summaries.get(className);
			if (classSummaries != null) {
				summaries.merge(classSummaries.getMethodSummaries().filterForMethod(signature));
			}
		}
		return summaries;
	}

	/**
	 * Gets all method summaries for the sole class in this object. Note that this
	 * method will throw an exception in case there are summaries for more than one
	 * class.
	 * 
	 * @return The method summaries for all methods in the only class contained in
	 *         this object
	 */
	public MethodSummaries getMergedMethodSummaries() {
		if (getClasses().size() > 1)
			throw new RuntimeException("Summaries for different classes cannot be merged");
		MethodSummaries summaries = new MethodSummaries();
		for (ClassMethodSummaries classSummaries : this.summaries.values()) {
			if (classSummaries != null) {
				summaries.merge(classSummaries.getMethodSummaries());
			}
		}
		return summaries;
	}

	/**
	 * Gets all flows across all classes and methods
	 * 
	 * @return All flows registered in this data object
	 */
	public Set<MethodFlow> getAllFlows() {
		return summaries.values().stream().flatMap(cs -> cs.getMethodSummaries().getAllFlows().stream())
				.collect(Collectors.toSet());
	}

	/**
	 * Returns a filter this object that contains only flows for the given method
	 * signature
	 * 
	 * @param signature The method for which to filter the flows
	 * @return An object containing only flows for the given method
	 */
	public ClassSummaries filterForMethod(String signature) {
		return filterForMethod(this.summaries.keySet(), signature);
	}

	/**
	 * Returns a filter this object that contains only flows for the given method
	 * signature in only the given set of classes
	 * 
	 * @param classes   The classes in which to look for method summaries
	 * @param signature The method for which to filter the flows
	 * @return An object containing only flows for the given method
	 */
	public ClassSummaries filterForMethod(Set<String> classes, String signature) {
		ClassSummaries newSummaries = new ClassSummaries();
		for (String className : classes) {
			ClassMethodSummaries methodSummaries = this.summaries.get(className);
			if (methodSummaries != null && !methodSummaries.isEmpty())
				newSummaries.merge(methodSummaries.filterForMethod(signature));
		}
		return newSummaries;
	}

	/**
	 * Merges the given flows into the existing flow definitions for the given class
	 * 
	 * @param className The name of the class for which to store the given flows
	 * @param newSums   The flows to merge into this data store
	 */
	public void merge(String className, MethodSummaries newSums) {
		if (newSums == null || newSums.isEmpty())
			return;

		ClassMethodSummaries methodSummaries = summaries.get(className);
		if (methodSummaries == null)
			summaries.put(className, new ClassMethodSummaries(className, newSums));
		else
			methodSummaries.merge(newSums);
	}

	/**
	 * Merges the given flows into the existing flow definitions for the given class
	 * 
	 * @param className The name of the class for which to store the given flows
	 * @param newSums   The flows to merge into this data store
	 */
	public void merge(String className, Set<MethodFlow> newSums) {
		if (newSums == null || newSums.isEmpty())
			return;

		ClassMethodSummaries methodSummaries = summaries.get(className);
		MethodSummaries ms = new MethodSummaries(newSums);
		if (methodSummaries == null) {
			methodSummaries = new ClassMethodSummaries(className, ms);
			summaries.put(className, methodSummaries);
		} else
			methodSummaries.merge(ms);
	}

	/**
	 * Merges the given flows into the existing flow definitions for the given class
	 * 
	 * @param summaries The existing method summaries
	 */
	public void merge(ClassSummaries summaries) {
		if (summaries == null || summaries.isEmpty())
			return;

		for (String className : summaries.getClasses())
			merge(summaries.getClassSummaries(className));

		// Merge the meta data if required, otherwise simply copy it
		if (metaData != null)
			metaData.merge(summaries.metaData);
		else
			metaData = new SummaryMetaData(summaries.metaData);
	}

	/**
	 * Merges the given flows into the existing flow definitions
	 * 
	 * @param summaries The summaries to merge
	 * @return True if new data was added to this summary data object during the
	 *         merge, false otherwise
	 */
	public boolean merge(ClassMethodSummaries summaries) {
		if (summaries == null || summaries.isEmpty())
			return false;

		ClassMethodSummaries existingSummaries = this.summaries.get(summaries.getClassName());
		if (existingSummaries == null) {
			this.summaries.put(summaries.getClassName(), summaries);
			return true;
		} else
			return existingSummaries.merge(summaries);
	}

	/**
	 * Gets whether this data object is empty, i.e., does not contain any data flow
	 * summaries
	 * 
	 * @return True if this data object is empty, otherwise false
	 */
	public boolean isEmpty() {
		return summaries.isEmpty();
	}

	/**
	 * Gets the names of the classes for which this data object contains method flow
	 * summaries
	 * 
	 * @return The classes for which this object has summaries
	 */
	public Set<String> getClasses() {
		return this.summaries.keySet();
	}

	/**
	 * Gets whether this data object contains method summaries for the given class
	 * 
	 * @param className True if this data object contains method summaries for the
	 *                  given class, otherwise false
	 * @return true if this data object contains method summaries for the given
	 *         class
	 */
	public boolean hasSummariesForClass(String className) {
		return summaries.containsKey(className);
	}

	/**
	 * Adds a dependency to this flow set
	 * 
	 * @param className The name of the dependency clsas
	 * @return True if this dependency class has been added, otherwise (dependency
	 *         already registered or summaries loaded for this class) false
	 */
	public boolean addDependency(String className) {
		if (isPrimitiveType(className) || this.summaries.containsKey(className))
			return false;
		return this.dependencies.add(className);
	}

	/**
	 * Checks whether the given type name denotes a primitive
	 * 
	 * @param typeName The type name to check
	 * @return True if the given type name denotes a primitive, otherwise false
	 */
	private boolean isPrimitiveType(String typeName) {
		return typeName.equals("int") || typeName.equals("long") || typeName.equals("float")
				|| typeName.equals("double") || typeName.equals("char") || typeName.equals("byte")
				|| typeName.equals("short") || typeName.equals("boolean");
	}

	/**
	 * Gets all dependencies of the flows in this object. Dependencies are classes
	 * which are references in a flow summary (e.g., through a field type), but do
	 * not have summaries on their own in this object.
	 * 
	 * @return The set of depdendency objects for this flow set
	 */
	public Set<String> getDependencies() {
		return this.dependencies;
	}

	/**
	 * Gets the meta data for this collection of summaries
	 * 
	 * @return The meta data for these summaries
	 */
	public SummaryMetaData getMetaData() {
		return metaData;
	}

	/**
	 * Sets the meta data for this collection of summaries
	 * 
	 * @param metaData The meta data for these summaries
	 */
	public void setMetaData(SummaryMetaData metaData) {
		this.metaData = metaData;
	}

	/**
	 * Clears all summaries from this data object
	 */
	public void clear() {
		if (this.dependencies != null)
			this.dependencies.clear();
		if (this.summaries != null)
			this.summaries.clear();
	}

	/**
	 * Validates all summaries for this class
	 */
	public void validate() {
		for (String className : summaries.keySet())
			summaries.get(className).validate();
	}

	@Override
	public String toString() {
		if (summaries == null || summaries.isEmpty())
			return "<no class summaries>";

		StringBuilder sb = new StringBuilder();
		sb.append("Summaries for ");

		boolean isFirst = true;
		for (String className : summaries.keySet()) {
			if (!isFirst)
				sb.append(", ");
			sb.append(className);
			isFirst = false;
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
		result = prime * result + ((metaData == null) ? 0 : metaData.hashCode());
		result = prime * result + ((summaries == null) ? 0 : summaries.hashCode());
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
		ClassSummaries other = (ClassSummaries) obj;
		if (dependencies == null) {
			if (other.dependencies != null)
				return false;
		} else if (!dependencies.equals(other.dependencies))
			return false;
		if (metaData == null) {
			if (other.metaData != null)
				return false;
		} else if (!metaData.equals(other.metaData))
			return false;
		if (summaries == null) {
			if (other.summaries != null)
				return false;
		} else if (!summaries.equals(other.summaries))
			return false;
		return true;
	}

}
