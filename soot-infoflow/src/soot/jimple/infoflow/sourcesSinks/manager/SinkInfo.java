package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

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
	 * @param definition
	 *            The original definition of the source or sink
	 * @param userData
	 *            Additional user data to be propagated with the source
	 */
	public SinkInfo(SourceSinkDefinition definition, Object userData) {
		super(definition, userData);
	}

	/**
	 * Creates a new instance of the {@link AbstractSourceSinkInfo} class.
	 * 
	 * @param definition
	 *            The original definition of the source or sink
	 */
	public SinkInfo(SourceSinkDefinition definition) {
		super(definition);
	}

}
