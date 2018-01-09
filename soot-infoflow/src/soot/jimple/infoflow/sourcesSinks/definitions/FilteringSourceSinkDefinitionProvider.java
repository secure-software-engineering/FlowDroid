package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.HashSet;
import java.util.Set;

/**
 * Source/sink definition provider that wraps another provider, but only passes
 * on definitions that match a given filter
 * 
 * @author Steven Arzt
 *
 */
public class FilteringSourceSinkDefinitionProvider implements ISourceSinkDefinitionProvider {

	/**
	 * Interface for filtering sources and sinks according to a user-implemented
	 * criterion
	 * 
	 * @author Steven Arzt
	 *
	 */
	public interface ISourceSinkFilter {

		/**
		 * Checks whether the filter accepts the given source/sink definition
		 * 
		 * @param def
		 *            The source/sink definition to check
		 * @return True if the filter accepts the given source/sink definition,
		 *         false otherwise
		 */
		public boolean accepts(SourceSinkDefinition def);

	}

	private final ISourceSinkDefinitionProvider innerProvider;
	private final ISourceSinkFilter filter;

	/**
	 * Creates a new instance of the
	 * {@link FilteringSourceSinkDefinitionProvider} class
	 * 
	 * @param innerProvider
	 *            The inner provider that creates the source/sink definitions
	 *            which are then filtered by this provider
	 * @param filter
	 *            The filter that defines which sources and sinks to include
	 */
	public FilteringSourceSinkDefinitionProvider(ISourceSinkDefinitionProvider innerProvider,
			ISourceSinkFilter filter) {
		this.innerProvider = innerProvider;
		this.filter = filter;
	}

	/**
	 * Filters the given set of source/sink definitions
	 * 
	 * @param input
	 *            The input set
	 * @return The filtered set of source/sink definitions
	 */
	private Set<SourceSinkDefinition> filter(Set<SourceSinkDefinition> input) {
		Set<SourceSinkDefinition> filtered = new HashSet<>(input.size());
		for (SourceSinkDefinition def : input)
			if (filter.accepts(def))
				filtered.add(def);
		return filtered;

	}

	@Override
	public Set<SourceSinkDefinition> getSources() {
		return filter(this.innerProvider.getSources());
	}

	@Override
	public Set<SourceSinkDefinition> getSinks() {
		return filter(this.innerProvider.getSinks());
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		return filter(this.innerProvider.getAllMethods());
	}

}
