package soot.jimple.infoflow.sourcesSinks.manager;

import java.util.Collections;
import java.util.Set;

import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

/**
 * Class containing additional information about a source. Users of FlowDroid
 * can derive from this class when implementing their own SourceSinkManager to
 * associate additional information with a source.
 * 
 * @author Steven Arzt, Daniel Magin
 */
public class SourceInfo extends AbstractSourceSinkInfo {

	protected final Set<AccessPath> accessPaths;

	/**
	 * Creates a new instance of the {@link SourceInfo} class. This is a
	 * convenience constructor to allow for the simple use of a single access
	 * path.
	 * 
	 * * @param definition The original definition of the source or sink
	 * 
	 * @param ap
	 *            The single access path that shall be tainted at this source
	 */
	public SourceInfo(SourceSinkDefinition definition, AccessPath ap) {
		this(definition, Collections.singleton(ap), null);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class. This
	 * is a convenience constructor to allow for the simple use of a single
	 * access path.
	 * 
	 * @param definition
	 *            The original definition of the source or sink
	 * @param ap
	 *            The single access path that shall be tainted at this source
	 * @param userData
	 *            Additional user data to be propagated with the source
	 */
	public SourceInfo(SourceSinkDefinition definition, AccessPath ap, Object userData) {
		this(definition, Collections.singleton(ap), userData);
	}

	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 * 
	 * @param definition
	 *            The original definition of the source or sink
	 * @param bundle
	 *            Information about access paths tainted by this source
	 */
	public SourceInfo(SourceSinkDefinition definition, Set<AccessPath> bundle) {
		this(definition, bundle, null);
	}

	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 * 
	 * @param definition
	 *            The original definition of the source or sink
	 * @param bundle
	 *            Information about access paths tainted by this source
	 * @param userData
	 *            Additional user data to be propagated with the source
	 */
	public SourceInfo(SourceSinkDefinition definition, Set<AccessPath> bundle, Object userData) {
		super(definition, userData);
		this.accessPaths = bundle;
	}

	/**
	 * Returns all access paths which are tainted by this source
	 * 
	 * @return All access paths tainted by this source
	 */
	public Set<AccessPath> getAccessPaths() {
		return this.accessPaths;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((accessPaths == null) ? 0 : accessPaths.hashCode());
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
		if (accessPaths == null) {
			if (other.accessPaths != null)
				return false;
		} else if (!accessPaths.equals(other.accessPaths))
			return false;
		return true;
	}

}
