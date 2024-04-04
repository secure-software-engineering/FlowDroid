package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

/**
 * A class to handle all access paths of sources and sinks for a certain method.
 * 
 * @author Daniel Magin
 * @author Steven Arzt
 *
 */
public abstract class AbstractSourceSinkDefinition implements ISourceSinkDefinition {

	protected ISourceSinkCategory category;
	protected Set<SourceSinkCondition> conditions;

	public AbstractSourceSinkDefinition() {
	}

	public AbstractSourceSinkDefinition(ISourceSinkCategory category) {
		this.category = category;
	}

	@Override
	public void setCategory(ISourceSinkCategory category) {
		this.category = category;
	}

	@Override
	public ISourceSinkCategory getCategory() {
		return category;
	}

	@Override
	public Set<SourceSinkCondition> getConditions() {
		return conditions;
	}

	@Override
	public void setConditions(Set<SourceSinkCondition> conditions) {
		this.conditions = conditions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractSourceSinkDefinition other = (AbstractSourceSinkDefinition) obj;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
				return false;
		return true;
	}

}
