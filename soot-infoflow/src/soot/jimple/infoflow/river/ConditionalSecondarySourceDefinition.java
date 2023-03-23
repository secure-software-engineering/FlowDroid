package soot.jimple.infoflow.river;

import soot.jimple.infoflow.sourcesSinks.definitions.AbstractSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Special source definition for sources of secondary flows that are also conditional sinks.
 *
 * @author Tim Lange
 */
public class ConditionalSecondarySourceDefinition extends AbstractSourceSinkDefinition {
    public static ConditionalSecondarySourceDefinition INSTANCE = new ConditionalSecondarySourceDefinition();

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
