package soot.jimple.infoflow.android.data;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;

/**
 * A category definition for a source or sink method
 * 
 * @author Steven Arzt
 *
 */
public class CategoryDefinition implements ISourceSinkCategory {

	public static enum CATEGORY {
		// all categories
		ALL,

		// SOURCES
		NO_CATEGORY, HARDWARE_INFO, UNIQUE_IDENTIFIER, LOCATION_INFORMATION, NETWORK_INFORMATION, ACCOUNT_INFORMATION,
		EMAIL_INFORMATION, FILE_INFORMATION, BLUETOOTH_INFORMATION, VOIP_INFORMATION, DATABASE_INFORMATION,
		PHONE_INFORMATION,

		// SINKS
		PHONE_CONNECTION, INTER_APP_COMMUNICATION, VOIP, PHONE_STATE, EMAIL, BLUETOOTH, ACCOUNT_SETTINGS, VIDEO,
		SYNCHRONIZATION_DATA, NETWORK, EMAIL_SETTINGS, FILE, LOG,

		// SHARED
		AUDIO, SMS_MMS, CONTACT_INFORMATION, CALENDAR_INFORMATION, SYSTEM_SETTINGS, IMAGE, BROWSER_INFORMATION, NFC
	}

	private CATEGORY systemCategory = null;
	private String customCategory;
	private String customDescription;

	public static final CategoryDefinition NO_CATEGORY = new CategoryDefinition(CATEGORY.NO_CATEGORY);

	/**
	 * Creates a new instance of the {@link CategoryDefinition} class
	 * 
	 * @param systemCategory The system-defined category
	 */
	public CategoryDefinition(CATEGORY systemCategory) {
		this(systemCategory, null);
	}

	/**
	 * Creates a new instance of the {@link CategoryDefinition} class
	 * 
	 * @param systemCategory The system-defined category
	 * @param customCategory A user-defined category ID. This parameter may only be
	 *                       used if the system-defined category is set to
	 *                       NO_CATEGORY.
	 */
	public CategoryDefinition(CATEGORY systemCategory, String customCategory) {
		this(systemCategory, customCategory, null);
	}

	/**
	 * Creates a new instance of the {@link CategoryDefinition} class
	 * 
	 * @param systemCategory    The system-defined category
	 * @param customCategory    A user-defined category ID. This parameter may only
	 *                          be used if the system-defined category is set to
	 *                          NO_CATEGORY.
	 * @param customDescription An optional description for the custom category
	 */
	public CategoryDefinition(CATEGORY systemCategory, String customCategory, String customDescription) {
		this.systemCategory = systemCategory;
		this.customCategory = customCategory;
		this.customDescription = customDescription;
	}

	/**
	 * Gets the system-defined category name. If this value is NO_CATEGORY, there
	 * may be a user-defined custom category name.
	 * 
	 * @return The system-defined category name
	 */
	public CATEGORY getSystemCategory() {
		return systemCategory;
	}

	public void setSystemCategory(CATEGORY systemCategory) {
		this.systemCategory = systemCategory;
	}

	/**
	 * Gets the user-defined category name. User-defined categories must have a
	 * system category of NO_CATEGORY.
	 * 
	 * @return The user-defined category name
	 */
	public String getCustomCategory() {
		return customCategory;
	}

	/**
	 * Sets the user-defined category id. This is an identifier for the system. To
	 * define a human-readable category description, use the setCustomDescription()
	 * instead.
	 * 
	 * @param customCategory The user-defined category id. Category identifiers must
	 *                       be unique.
	 */
	public void setCustomCategory(String customCategory) {
		this.customCategory = customCategory;
	}

	/**
	 * Gets the description of the user-defined category
	 * 
	 * @return The description of the user-defined category
	 */
	public String getCustomDescription() {
		return customDescription;
	}

	/**
	 * Sets the description of the user-defined category
	 * 
	 * @param customDescription The description of the user-defined category
	 */
	public void setCustomDescription(String customDescription) {
		this.customDescription = customDescription;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customCategory == null) ? 0 : customCategory.hashCode());
		result = prime * result + ((customDescription == null) ? 0 : customDescription.hashCode());
		result = prime * result + ((systemCategory == null) ? 0 : systemCategory.hashCode());
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
		if (customCategory == null) {
			if (other.customCategory != null)
				return false;
		} else if (!customCategory.equals(other.customCategory))
			return false;
		if (customDescription == null) {
			if (other.customDescription != null)
				return false;
		} else if (!customDescription.equals(other.customDescription))
			return false;
		if (systemCategory != other.systemCategory)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (customCategory == null || customCategory.isEmpty()) {
			if (systemCategory != null)
				return systemCategory.toString();
			else
				return "<invalid>";
		} else {
			return customCategory;
		}
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
		if (customCategory != null && !customCategory.isEmpty()) {
			// Prefer the description text over the id
			if (customDescription != null && !customDescription.isEmpty())
				return customDescription;
			return customCategory;
		} else if (systemCategory != null) {
			switch (systemCategory) {
			case ALL:
				return "All Categories";
			case NO_CATEGORY:
				return "No Category";
			case HARDWARE_INFO:
				return "Hardware Information";
			case UNIQUE_IDENTIFIER:
				return "Unique Identifier";
			case LOCATION_INFORMATION:
				return "Location Information";
			case NETWORK_INFORMATION:
				return "Network Information";
			case ACCOUNT_INFORMATION:
				return "Account Information";
			case EMAIL_INFORMATION:
				return "E-Mail Information";
			case FILE_INFORMATION:
				return "File Information";
			case BLUETOOTH_INFORMATION:
				return "Bluetooth Information";
			case VOIP_INFORMATION:
				return "Voice-over-IP Information";
			case DATABASE_INFORMATION:
				return "Database Information";
			case PHONE_INFORMATION:
				return "Phone Information";
			case PHONE_CONNECTION:
				return "Phone (Line) Connection";
			case INTER_APP_COMMUNICATION:
				return "Inter-App Communication";
			case VOIP:
				return "Voice-over-IP";
			case PHONE_STATE:
				return "Phone State";
			case EMAIL:
				return "E-Mail";
			case BLUETOOTH:
				return "Bluetooth";
			case ACCOUNT_SETTINGS:
				return "Account Settings";
			case VIDEO:
				return "Video";
			case SYNCHRONIZATION_DATA:
				return "Synchronization Data";
			case NETWORK:
				return "Network";
			case EMAIL_SETTINGS:
				return "E-Mail Settings";
			case FILE:
				return "File";
			case LOG:
				return "Log Files";
			case AUDIO:
				return "Audio";
			case SMS_MMS:
				return "SMS / MMS";
			case CONTACT_INFORMATION:
				return "Contact Information";
			case CALENDAR_INFORMATION:
				return "Calendar Information";
			case SYSTEM_SETTINGS:
				return "System Settings";
			case IMAGE:
				return "Image";
			case BROWSER_INFORMATION:
				return "Browser Information";
			case NFC:
				return "NFC";
			default:
				return "<invalid system category>";
			}
		} else
			return "<invalid>";
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
		return new CategoryDefinition(systemCategory, customCategory);
	}

	@Override
	public String getID() {
		if (systemCategory == null || systemCategory == CATEGORY.NO_CATEGORY)
			return customCategory;
		return systemCategory.name();
	}

}
