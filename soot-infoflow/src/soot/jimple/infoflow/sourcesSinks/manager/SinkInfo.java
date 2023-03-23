package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.Collection;
import java.util.Collections;

/**
 * Class containing additional information about a sink. Users of FlowDroid can
 * derive from this class when implementing their own SourceSinkManager to
 * associate additional information with a sink.
 * 
 * @author Steven Arzt, Daniel Magin
 */
public class SinkInfo extends AbstractSourceSinkInfo {

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 * 
	 * @param definitions The original definition of the source or sink
	 * @param userData    Additional user data to be propagated with the source
	 */
	public SinkInfo(Collection<ISourceSinkDefinition> definitions, Object userData) {
		super(definitions, userData);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 * 
	 * @param definitions The original definition of the source or sink
	 */
	public SinkInfo(Collection<ISourceSinkDefinition> definitions) {
		super(definitions);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 *
	 * @param definition The original definition of the source or sink
	 */
	public SinkInfo(ISourceSinkDefinition definition) {
		super(Collections.singleton(definition));
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 */
	public SinkInfo() {
		super(null);
	}
}
