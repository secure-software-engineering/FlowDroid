package soot.jimple.infoflow.sourcesSinks.manager;

/**
 * Abstract base class for source/sink information
 * 
 * @author Steven Arzt
 *
 */
abstract class AbstractSourceSinkInfo {

	protected final Object userData;

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class
	 * 
	 * @param userData Additional user data to be propagated with the source or sink
	 */
	public AbstractSourceSinkInfo(Object userData) {
		this.userData = userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
}
