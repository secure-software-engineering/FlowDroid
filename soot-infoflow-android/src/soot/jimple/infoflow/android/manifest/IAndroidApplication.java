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
	 * Gets whether a debugger can be attached to this Android application
	 * 
	 * @return True if debugging is enabled, false otherwise
	 */
	public boolean isDebuggable();

	/**
	 * Gets whether the app can be backed up
	 * 
	 * @return True if the app can be backed up, false otherwise
	 */
	public boolean isAllowBackup();

	/**
	 * Gets the fully-qualified class name of the application class
	 * 
	 * @return The fully-qualified class name of the application class
	 */
	public String getName();

	/**
	 * Gets whether cleartext traffic is allowed in this Android application
	 * 
	 * @return True if cleartext traffic is allowed in this Android application,
	 *         false if cleartext traffic is disabled, and <code>null</code> if no
	 *         such setting exists
	 */
	public Boolean isUsesCleartextTraffic();

}
