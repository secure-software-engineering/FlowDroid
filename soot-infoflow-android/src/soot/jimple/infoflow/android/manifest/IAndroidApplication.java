package soot.jimple.infoflow.android.manifest;

/**
 * The application object inside an Android app
 * 
 * @author Steven Arzt
 *
 */
public interface IAndroidApplication {

	/**
	 * Gets whether this Android application is enabled
	 *
	 * @return True if this application is enabled, otherwise false
	 */
	public boolean isEnabled();

	/**
	 * Gets the fully-qualified class name of the application class
	 * 
	 * @return The fully-qualified class name of the application class
	 */
	public String getName();

}
