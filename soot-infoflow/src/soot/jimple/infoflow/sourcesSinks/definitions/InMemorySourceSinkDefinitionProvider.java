package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Collection;

/**
 * A provider for source/sink definitions that takes its data from sets provided
 * during provider initialization
 * 
 * @author Steven Arzt
 */
public class InMemorySourceSinkDefinitionProvider implements ISourceSinkDefinitionProvider {

	protected Collection<? extends ISourceSinkDefinition> sources;
	protected Collection<? extends ISourceSinkDefinition> sinks;

	public InMemorySourceSinkDefinitionProvider(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks) {
		this.sources = sources;
		this.sinks = sinks;
	}

	@Override
	public Collection<? extends ISourceSinkDefinition> getSources() {
		return sources;
	}

	@Override
	public Collection<? extends ISourceSinkDefinition> getSinks() {
		return sinks;
	}

}
