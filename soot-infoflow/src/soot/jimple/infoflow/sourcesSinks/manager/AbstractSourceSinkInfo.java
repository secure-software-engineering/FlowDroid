package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.Collection;

/**
 * Abstract base class for source/sink information
 * 
 * @author Steven Arzt
 *
 */
abstract class AbstractSourceSinkInfo {

	protected final Collection<ISourceSinkDefinition> definitions;
	protected final Object userData;

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class
	 * 
	 * @param definitions The original definitions of the source or sink
	 */
	public AbstractSourceSinkInfo(Collection<ISourceSinkDefinition> definitions) {
		this(definitions, null);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class
	 * 
	 * @param definitions The original definitions of the source or sink
	 * @param userData   Additional user data to be propagated with the source or
	 *                   sink
	 */
	public AbstractSourceSinkInfo(Collection<ISourceSinkDefinition> definitions, Object userData) {
		this.definitions = definitions;
		this.userData = userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definitions == null) ? 0 : definitions.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
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
		AbstractSourceSinkInfo other = (AbstractSourceSinkInfo) obj;
		if (definitions == null) {
			if (other.definitions != null)
				return false;
		} else if (!definitions.equals(other.definitions))
			return false;
		if (userData == null) {
			if (other.userData != null)
				return false;
		} else if (!userData.equals(other.userData))
			return false;
		return true;
	}

	/**
	 * Gets the user data to be tracked together with this source
	 * 
	 * @return The user data to be tracked together with this source
	 */
	public Object getUserData() {
		return this.userData;
	}

	/**
	 * Gets the original definition of this data flow source or sink
	 * 
	 * @return The original definition of the source or sink. The return value may
	 *         be null if this source is not modeled for a specific method or field.
	 */
	public Collection<ISourceSinkDefinition> getDefinitions() {
		return definitions;
	}
}
