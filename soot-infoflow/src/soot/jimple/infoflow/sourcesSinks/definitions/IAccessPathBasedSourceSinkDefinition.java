package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Collection;
import java.util.Set;

/**
 * Common interface for all source/sink definitions based on access paths
 * 
 * @author Steven Arzt
 *
 */
public interface IAccessPathBasedSourceSinkDefinition extends ISourceSinkDefinition {

	/**
	 * Gets all access paths referenced in this source or sink definition,
	 * regardless of their respective semantics
	 * 
	 * @return All access paths referenced in this source or sink definition. The
	 *         return value can be null if this definition does not reference any
	 *         access paths.
	 */
	public Set<AccessPathTuple> getAllAccessPaths();

	/**
	 * Filters the source/sink definition to only reference the given access paths
	 * 
	 * @param accessPaths The access path to which to limit the scope of this
	 *                    source/sink definition
	 * @return A copy of this source/sink definition that only references the given
	 *         access paths
	 */
	public IAccessPathBasedSourceSinkDefinition filter(Collection<AccessPathTuple> accessPaths);

	/**
	 * Checks whether this source/sink definition is empty, i.e., has no concrete
	 * access paths
	 * 
	 * @return True if this source/sink definition is empty, i.e., has no concrete
	 *         access paths, otherwise false
	 */
	public abstract boolean isEmpty();

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sources
	 * 
	 * @return The source-only subset of this definition
	 */
	public abstract IAccessPathBasedSourceSinkDefinition getSourceOnlyDefinition();

	/**
	 * Creates a definition which is a subset of this definition that only contains
	 * the sinks
	 * 
	 * @return The sink-only subset of this definition
	 */
	public abstract IAccessPathBasedSourceSinkDefinition getSinkOnlyDefinition();

}
