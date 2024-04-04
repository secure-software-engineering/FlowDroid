package soot.jimple.infoflow.android.data;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;

/**
 * A category definition for a source or sink method
 * 
 * @author Steven Arzt
 *
 */
public class CategoryDefinition implements ISourceSinkCategory {

	public static final CategoryDefinition ALL_CATEGORIES = new CategoryDefinition("all");
	private String categoryId;
	private String humanReadableName;

	/**
	 * Creates a new instance of the {@link CategoryDefinition} class
	 * 
	 * @param categoryId  user-defined category ID
	 */
	public CategoryDefinition(String categoryId) {
		this(categoryId, null);
	}

	/**
	 * Creates a new instance of the {@link CategoryDefinition} class
	 * 
	 * @param customCategoryId    A user-defined category ID
	 * @param humanReadableNmae An optional human-readable name
	 */
	public CategoryDefinition(String customCategoryId, String humanReadableNmae) {
		this.categoryId = customCategoryId;
		this.humanReadableName = humanReadableNmae;
	}

	/**
	 * Gets the user-defined category id.
	 * 
	 * @return The user-defined category id
	 */
	public String getCategoryId() {
		return categoryId;
	}

	/**
	 * Sets the user-defined category id. This is an identifier for the system. To
	 * define a human-readable category description, use the setCustomDescription()
	 * instead.
	 * 
	 * @param id The user-defined category id. Category identifiers must
	 *                       be unique.
	 */
	public void setCategoryId(String id) {
		this.categoryId = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + ((humanReadableName == null) ? 0 : humanReadableName.hashCode());
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
		CategoryDefinition other = (CategoryDefinition) obj;
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId))
			return false;
		if (humanReadableName == null) {
			if (other.humanReadableName != null)
				return false;
		} else if (!humanReadableName.equals(other.humanReadableName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return categoryId;
	}

	/**
	 * Gets the category description as a string. This method either returns a
	 * human-readable description for the current system category or the
	 * user-defined category if the system-defined category is "NO_CATEGORY", but a
	 * user-defined category is available.
	 * 
	 * @return The category description as a string
	 */
	@Override
	public String getHumanReadableDescription() {
		return humanReadableName;
	}

	/**
	 * Gets a variant of this description that only contains identifiers. Such a
	 * variant is helpful when trying to identify the category in a collection in
	 * which not necessary all meta data is available.
	 * 
	 * @return A variant of this category definition that only contains identifiers,
	 *         but not, e.g., descriptions
	 */
	public CategoryDefinition getIdOnlyDescription() {
		return new CategoryDefinition(categoryId);
	}

	@Override
	public String getID() {
		return categoryId;
	}

}
