package soot.jimple.infoflow.android.source;

import soot.jimple.infoflow.InfoflowConfiguration.CategoryMode;
import soot.jimple.infoflow.InfoflowConfiguration.SourceSinkFilterMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SourceSinkConfiguration;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.android.source.parsers.xml.AbstractXMLSourceSinkParser.ICategoryFilter;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Default class for filtering source and sink definitions based on the category
 * 
 * @author Steven Arzt
 *
 */
public class ConfigurationBasedCategoryFilter implements ICategoryFilter {

	private final SourceSinkConfiguration config;

	/**
	 * Creates a new instance of the {@link ConfigurationBasedCategoryFilter} class
	 * 
	 * @param config The configuration that defines which source and sink categories
	 *               to include and which ones to exclude
	 */
	public ConfigurationBasedCategoryFilter(SourceSinkConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean acceptsCategory(ISourceSinkCategory category) {
		// We cannot compare descriptions to the configuration file
		if (category instanceof CategoryDefinition) {
			CategoryDefinition catDef = (CategoryDefinition) category;
			catDef = catDef.getIdOnlyDescription();

			CategoryMode sourceType = config.getSourceCategoriesAndModes().get(catDef);
			CategoryMode sinkType = config.getSinkCategoriesAndModes().get(catDef);

			// Check whether the category is excluded for everything
			if (sourceType != null && sourceType == CategoryMode.Exclude)
				if (sinkType != null && sinkType == CategoryMode.Exclude)
					return false;

			// Check whether the category is included for something
			if (config.getSinkFilterMode() == SourceSinkFilterMode.UseOnlyIncluded) {
				if (sourceType != null && sourceType == CategoryMode.Include)
					return true;
				if (sinkType != null && sinkType == CategoryMode.Include)
					return true;

				return false;
			}
		}

		// There is no reason to exclude the category
		return true;
	}

	@Override
	public SourceSinkType filter(ISourceSinkCategory category, SourceSinkType sourceSinkType) {
		// We cannot compare descriptions to the configuration file
		if (category instanceof CategoryDefinition) {
			CategoryDefinition catDef = (CategoryDefinition) category;
			catDef = catDef.getIdOnlyDescription();
			CategoryMode sourceMode = config.getSourceCategoriesAndModes().get(category);
			CategoryMode sinkMode = config.getSinkCategoriesAndModes().get(category);
			if (sourceSinkType == SourceSinkType.Source || sourceSinkType == SourceSinkType.Both) {
				if (config.getSourceFilterMode() == SourceSinkFilterMode.UseAllButExcluded) {
					if (sourceMode != null && sourceMode == CategoryMode.Exclude)
						sourceSinkType = sourceSinkType.removeType(SourceSinkType.Source);
				} else if (config.getSourceFilterMode() == SourceSinkFilterMode.UseOnlyIncluded) {
					if (sourceMode == null || sourceMode != CategoryMode.Include)
						sourceSinkType = sourceSinkType.removeType(SourceSinkType.Source);
				}
			}

			if (sourceSinkType == SourceSinkType.Sink || sourceSinkType == SourceSinkType.Both) {
				if (config.getSinkFilterMode() == SourceSinkFilterMode.UseAllButExcluded) {
					if (sinkMode != null && sinkMode == CategoryMode.Exclude)
						sourceSinkType = sourceSinkType.removeType(SourceSinkType.Sink);
				} else if (config.getSinkFilterMode() == SourceSinkFilterMode.UseOnlyIncluded) {
					if (sinkMode == null || sinkMode != CategoryMode.Include)
						sourceSinkType = sourceSinkType.removeType(SourceSinkType.Sink);
				}
			}
			return sourceSinkType;
		}
		return SourceSinkType.Undefined;

	}

}
