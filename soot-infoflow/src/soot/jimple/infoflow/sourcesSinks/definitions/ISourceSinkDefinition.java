package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

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
	 * Indicates if the definition contains any sources or sinks
	 * 
	 * @return The boolean if this definition is empty
	 */
	public abstract boolean isEmpty();

	/**
	 * Gets the conditions under which the source/sink definition is valid
	 *
	 * @return A set with the conditions under which the source/sink definition is
	 *         valid, optionally <code>null</code> if no such conditions exist
	 */
	public Set<SourceSinkCondition> getConditions();

	/**
	 * Sets the conditions under which the source/sink definition is valid
	 *
	 * @param conditions
	 *            A set with the conditions under which the source/sink definition
	 *            is valid, optionally <code>null</code> if no such conditions exist
	 */
	public void setConditions(Set<SourceSinkCondition> conditions);

}
