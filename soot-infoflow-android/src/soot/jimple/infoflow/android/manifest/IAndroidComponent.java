package soot.jimple.infoflow.android.manifest;

/**
 * Base interface for all components inside an Android app
 * 
 * @author Steven Arzt
 *
 */
public interface IAndroidComponent {

	/**
	 * Gets whether this Android component is enabled
	 * 
	 * @return True if this Android component is enabled, false otherwise
	 */
	public boolean isEnabled();

	/**
	 * Gets whether this Android component is exported
	 * 
	 * @return True if this Android component is exported, false otherwise
	 */
	public boolean isExported();

	/**
	 * Gets the name of this component as a fully-qualified class name
	 * 
	 * @return The fully-qualified class name of this Android component
	 */
	public String getNameString();

}
