package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Abstract base class for source/sink information
 * 
 * @author Steven Arzt
 *
 */
abstract class AbstractSourceSinkInfo {

	protected final ISourceSinkDefinition definition;
	protected final Object userData;

	/**
	 * Creates a new instance of the {@link ISourceSinkInfo} class
	 * 
	 * @param definition The original definition of the source or sink
	 */
	public AbstractSourceSinkInfo(ISourceSinkDefinition definition) {
		this(definition, null);
	}

	/**
	 * Creates a new instance of the {@link ISourceSinkInfo} class
	 * 
	 * @param definition The original definition of the source or sink
	 * @param userData   Additional user data to be propagated with the source or
	 *                   sink
	 */
	public AbstractSourceSinkInfo(ISourceSinkDefinition definition, Object userData) {
		this.definition = definition;
		this.userData = userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
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
		SourceInfo other = (SourceInfo) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
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
	public ISourceSinkDefinition getDefinition() {
		return definition;
	}

}
