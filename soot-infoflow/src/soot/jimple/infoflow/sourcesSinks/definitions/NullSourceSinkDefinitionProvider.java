package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Collections;
import java.util.Set;

/**
 * Empty provider that simulates empty lists of sources and sinks
 * 
 * @author Steven Arzt
 *
 */
public class NullSourceSinkDefinitionProvider implements ISourceSinkDefinitionProvider {

	@Override
	public Set<ISourceSinkDefinition> getSources() {
		return Collections.emptySet();
	}

	@Override
	public Set<ISourceSinkDefinition> getSinks() {
		return Collections.emptySet();
	}

	@Override
	public Set<ISourceSinkDefinition> getAllMethods() {
		return Collections.emptySet();
	}

}
