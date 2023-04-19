package soot.jimple.infoflow.river;

import soot.jimple.infoflow.sourcesSinks.definitions.AbstractSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Special source/sink definition for statements that are sinks merely because
 * we need the respective flow to activate another flow. In case something is
 * written to an output stream, we need a flow to know whether the output stream
 * is interesting, i.e., whether to keep or discard this main flow. This class
 * is used to model sinks for such secondary flows.
 * 
 * @author Steven Arzt
 *
 */
public class SecondarySinkDefinition extends AbstractSourceSinkDefinition {
	public static SecondarySinkDefinition INSTANCE = new SecondarySinkDefinition();

	@Override
	public ISourceSinkDefinition getSourceOnlyDefinition() {
		return null;
	}

	@Override
	public ISourceSinkDefinition getSinkOnlyDefinition() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

}
