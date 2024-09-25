package soot.jimple.infoflow.sourcesSinks.manager;

import java.util.Collection;
import java.util.Collections;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Class containing additional information about a sink. Users of FlowDroid can
 * derive from this class when implementing their own SourceSinkManager to
 * associate additional information with a sink.
 * 
 * @author Steven Arzt, Daniel Magin
 */
public class SinkInfo extends AbstractSourceSinkInfo {
	private Collection<ISourceSinkDefinition> definitions;

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 * 
	 * @param definitions The original definition of the source or sink
	 * @param userData    Additional user data to be propagated with the source
	 */
	public SinkInfo(Collection<ISourceSinkDefinition> definitions, Object userData) {
		super(userData);
		assert definitions != null;
		this.definitions = definitions;
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 * 
	 * @param definitions The original definition of the source or sink
	 */
	public SinkInfo(Collection<ISourceSinkDefinition> definitions) {
		this(definitions, null);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 *
	 * @param definition The original definition of the source or sink
	 */
	public SinkInfo(ISourceSinkDefinition definition) {
		this(Collections.singleton(definition));
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 */
	public SinkInfo() {
		super(null);
		this.definitions = Collections.singleton(null);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((definitions == null) ? 0 : definitions.hashCode());
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
		SinkInfo other = (SinkInfo) obj;
		if (definitions == null) {
			if (other.definitions != null)
				return false;
		} else if (!definitions.equals(other.definitions))
			return false;
		return true;
	}
}
