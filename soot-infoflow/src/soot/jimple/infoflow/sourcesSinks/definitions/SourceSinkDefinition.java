package soot.jimple.infoflow.sourcesSinks.definitions;

/**
 * A class to handle all access paths of sources and sinks for a certain method.
 * 
 * @author Daniel Magin
 * @author Steven Arzt
 *
 */
public abstract class SourceSinkDefinition {

	protected ISourceSinkCategory category;

	/**
	 * Sets the category to which this source or sink belonga
	 * 
	 * @param category
	 *            The category to which this source or sink belonga
	 */
	public void setCategory(ISourceSinkCategory category) {
		this.category = category;
	}

	/**
	 * Gets the category to which this source or sink belonga
	 * 
	 * @return The category to which this source or sink belonga
	 */
	public ISourceSinkCategory getCategory() {
		return category;
	}

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sources
	 * 
	 * @return The source-only subset of this definition
	 */
	public abstract SourceSinkDefinition getSourceOnlyDefinition();

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sinks
	 * 
	 * @return The sink-only subset of this definition
	 */
	public abstract SourceSinkDefinition getSinkOnlyDefinition();

	/**
	 * Merges the source and sink definitions of the given definition object into
	 * this definition object
	 * 
	 * @param other
	 *            The definition object to merge
	 */
	public abstract void merge(SourceSinkDefinition other);

	/**
	 * Checks whether this source/sink definition is empty, i.e., has no concrete
	 * access paths
	 * 
	 * @return True if this source/sink definition is empty, i.e., has no concrete
	 *         access paths, otherwise false
	 */
	public abstract boolean isEmpty();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((category == null) ? 0 : category.hashCode());
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
		SourceSinkDefinition other = (SourceSinkDefinition) obj;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		return true;
	}

}
