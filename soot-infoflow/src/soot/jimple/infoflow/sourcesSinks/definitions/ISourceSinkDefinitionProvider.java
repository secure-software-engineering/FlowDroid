package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Collection;

/**
 * Common interface for all classes that support loading source and sink
 * definitions
 * 
 * @author Steven Arzt
 *
 */
public interface ISourceSinkDefinitionProvider {

	/**
	 * Gets a set of all sources registered in the provider
	 * 
	 * @return A set of all sources registered in the provider
	 */
	public Collection<? extends ISourceSinkDefinition> getSources();

	/**
	 * Gets a set of all sinks registered in the provider
	 * 
	 * @return A set of all sinks registered in the provider
	 */
	public Collection<? extends ISourceSinkDefinition> getSinks();

	/**
	 * Gets all methods for which there are source/sink definitions
	 * 
	 * @return A set containing all methods for which there is a source/sink
	 *         definition. This also includes methods explicitly labeled as
	 *         "neither".
	 */
	public Collection<? extends ISourceSinkDefinition> getAllMethods();

}
