package soot.jimple.infoflow.sourcesSinks.manager;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.Collection;

/**
 * Concrete BaseSourceSinkManager to use the XMLSourceSinkParser in simple Java test cases.
 *
 * @author Tim Lange
 */
public class SimpleSourceSinkManager extends BaseSourceSinkManager {
    public SimpleSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources, Collection<? extends ISourceSinkDefinition> sinks,
                                   InfoflowConfiguration config) {
        super(sources, sinks, config);
    }

    @Override
    protected boolean isEntryPointMethod(SootMethod method) {
        return false;
    }
}
