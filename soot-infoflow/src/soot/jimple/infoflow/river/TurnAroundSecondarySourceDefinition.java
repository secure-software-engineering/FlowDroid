package soot.jimple.infoflow.river;

import soot.jimple.infoflow.sourcesSinks.definitions.AbstractSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * This is used as a source to denote a connection from a turnaround sink to a
 * secondary sink.
 *
 * @author Marc Miltenberger
 */
public class TurnAroundSecondarySourceDefinition extends AbstractSourceSinkDefinition {
	public static TurnAroundSecondarySourceDefinition INSTANCE = new TurnAroundSecondarySourceDefinition();

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
