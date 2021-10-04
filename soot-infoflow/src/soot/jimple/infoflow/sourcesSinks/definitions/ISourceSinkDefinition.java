package soot.jimple.infoflow.sourcesSinks.definitions;

/**
 * Abstract interface for soure/sink definitions
 * 
 * @author Steven Arzt
 *
 */
public interface ISourceSinkDefinition {

	/**
	 * Sets the category to which this source or sink belongs
	 * 
	 * @param category The category to which this source or sink belongs
	 */
	public void setCategory(ISourceSinkCategory category);

	/**
	 * Gets the category to which this source or sink belonga
	 * 
	 * @return The category to which this source or sink belonga
	 */
	public ISourceSinkCategory getCategory();

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sources
	 * 
	 * @return The source-only subset of this definition
	 */
	public abstract ISourceSinkDefinition getSourceOnlyDefinition();

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sinks
	 * 
	 * @return The sink-only subset of this definition
	 */
	public abstract ISourceSinkDefinition getSinkOnlyDefinition();

	/**
	 * Merges the source and sink definitions of the given definition object into
	 * this definition object
	 * 
	 * @param other The definition object to merge
	 */
	public abstract void merge(ISourceSinkDefinition other);

	/**
	 * Indicates if the definition contains any sources or sinks
	 * 
	 * @return The boolean if this definition is empty
	 */
	public abstract boolean isEmpty();

}
