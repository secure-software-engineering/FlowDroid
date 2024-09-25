package soot.jimple.infoflow.sourcesSinks.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import heros.solver.Pair;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.util.HashMultiMap;

/**
 * Class containing additional information about a source. Users of FlowDroid
 * can derive from this class when implementing their own SourceSinkManager to
 * associate additional information with a source.
 * 
 * @author Steven Arzt, Daniel Magin
 */
public class SourceInfo extends AbstractSourceSinkInfo {
	protected final HashMultiMap<AccessPath, ISourceSinkDefinition> accessPathsToDefinitions;

	/**
	 * Creates a new instance of the {@link SourceInfo} class. This is a convenience
	 * constructor to allow for the simple use of a single access path.
	 * 
	 * * @param definition The original definition of the source or sink
	 * 
	 * @param ap The single access path that shall be tainted at this source
	 */
	public SourceInfo(ISourceSinkDefinition definition, AccessPath ap) {
		this(definition, ap, null);
	}

	public SourceInfo(AccessPath ap) {
		this(Collections.singleton(new Pair<>(ap, null)));
	}

	public SourceInfo(ISourceSinkDefinition definition, Collection<AccessPath> aps) {
		this(aps.stream().map(ap -> new Pair<>(ap, definition)).collect(Collectors.toSet()), null);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class. This is a
	 * convenience constructor to allow for the simple use of a single access path.
	 * 
	 * @param definition The original definition of the source or sink
	 * @param ap         The single access path that shall be tainted at this source
	 * @param userData   Additional user data to be propagated with the source
	 */
	public SourceInfo(ISourceSinkDefinition definition, AccessPath ap, Object userData) {
		this(Collections.singleton(new Pair<>(ap, definition)), userData);
	}

	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 * 
	 * @param apAndDefs Pairs of access paths and defintions
	 */
	public SourceInfo(Collection<Pair<AccessPath, ISourceSinkDefinition>> apAndDefs) {
		this(apAndDefs, null);
	}

	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 *
	 * @param apAndDefs Pairs of access paths and defintions
	 * @param userData  Additional user data to be propagated with the source
	 */
	public SourceInfo(Collection<Pair<AccessPath, ISourceSinkDefinition>> apAndDefs, Object userData) {
		super(userData);

		// Make sure we don't accidentally create empty SourceInfos
		assert apAndDefs.size() > 0;

		this.accessPathsToDefinitions = new HashMultiMap<>();
		for (Pair<AccessPath, ISourceSinkDefinition> apAndDef : apAndDefs) {
			accessPathsToDefinitions.put(apAndDef.getO1(), apAndDef.getO2());
		}
	}

	/**
	 * Returns all access paths which are tainted by this source
	 * 
	 * @return All access paths tainted by this source
	 */
	public Set<AccessPath> getAccessPaths() {
		return this.accessPathsToDefinitions.keySet();
	}

	/**
	 * Get all definitions that match the access path Precondition: This SourceInfo
	 * contains the access path
	 *
	 * @param ap access path
	 * @return all definitions which match the access path
	 */
	public Collection<ISourceSinkDefinition> getDefinitionsForAccessPath(AccessPath ap) {
		return this.accessPathsToDefinitions.get(ap);
	}

	public Collection<ISourceSinkDefinition> getAllDefinitions() {
		return this.accessPathsToDefinitions.values();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((accessPathsToDefinitions == null) ? 0 : accessPathsToDefinitions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceInfo other = (SourceInfo) obj;
		if (accessPathsToDefinitions == null) {
			if (other.accessPathsToDefinitions != null)
				return false;
		} else if (!accessPathsToDefinitions.equals(other.accessPathsToDefinitions))
			return false;
		return true;
	}

}
