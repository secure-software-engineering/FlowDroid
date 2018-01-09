package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

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
	 * @return A set of all sources registered in the provider
	 */
	public Set<SourceSinkDefinition> getSources();
	
	/**
	 * Gets a set of all sinks registered in the provider
	 * @return A set of all sinks registered in the provider
	 */
	public Set<SourceSinkDefinition> getSinks();
	
	/**
	 * Gets all methods for which there are source/sink definitions
	 * @return A set containing all methods for which there is a source/sink
	 * definition. This also includes methods explicitly labeled as "neither".
	 */
	public Set<SourceSinkDefinition> getAllMethods();
	
}
