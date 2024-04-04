package soot.jimple.infoflow.android.manifest;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Common interface for all classes that can deal with Android manifests
 * 
 * @author Steven Arzt
 *
 */
public interface IManifestHandler<A extends IActivity, S extends IService, C extends IContentProvider, B extends IBroadcastReceiver>
		extends Closeable {

	/**
	 * Gets the unique package name of this Android app
	 * 
	 * @return The package name of this app
	 */
	public String getPackageName();

	/**
	 * Returns all activities in the Android app
	 *
	 * @return list with all activities
	 */
	public IComponentContainer<? extends A> getActivities();

	/**
	 * Returns all content providers in the Android app
	 *
	 * @return list with all providers
	 */
	public IComponentContainer<? extends C> getContentProviders();

	/**
	 * Returns all services providers in the Android app
	 *
	 * @return list with all services
	 */
	public IComponentContainer<? extends S> getServices();

	/**
	 * Returns all broadcast receivers providers in the Android app
	 *
	 * @return list with all receivers
	 */
	public IComponentContainer<? extends B> getBroadcastReceivers();

	/**
	 * Gets the Android application object
	 * 
	 * @return The Android application object
	 */
	public IAndroidApplication getApplication();

	/**
	 * Gets all components inside this Android app
	 * 
	 * @return All components inside this Android app
	 */
	public default List<? extends IAndroidComponent> getAllComponents() {
		List<IAndroidComponent> components = new ArrayList<>();

		List<? extends IActivity> activities = getActivities().asList();
		if (activities != null && !activities.isEmpty())
			components.addAll(activities);

		List<? extends IContentProvider> providers = getContentProviders().asList();
		if (providers != null && !providers.isEmpty())
			components.addAll(providers);

		List<? extends IService> services = getServices().asList();
		if (services != null && !services.isEmpty())
			components.addAll(services);

		List<? extends IBroadcastReceiver> receivers = getBroadcastReceivers().asList();
		if (receivers != null && !receivers.isEmpty())
			components.addAll(receivers);

		return components;
	}

	/**
	 * Gets all classes the contain entry points in this applications
	 *
	 * @return All classes the contain entry points in this applications
	 */
	public default Set<String> getEntryPointClasses() {
		IAndroidApplication app = getApplication();

		// If the application is not enabled, there are no entry points
		if (app != null && !app.isEnabled())
			return Collections.emptySet();

		// Collect the components
		Set<String> entryPoints = new HashSet<String>();
		for (IAndroidComponent node : getAllComponents())
			checkAndAddComponent(entryPoints, node);

		if (entryPoints.isEmpty()){
			//if no entry point is detected at all, the app is likely be malware, add all components
			List<IAndroidComponent> allEnabled = getAllComponents().stream().filter(c -> c.isEnabled()).collect(Collectors.toList());
			allEnabled.forEach(e->entryPoints.add(e.getNameString()));
		}

		if (app != null) {
			String appName = app.getName();
			if (appName != null && !appName.isEmpty())
				entryPoints.add(appName);
		}

		return entryPoints;
	}

	default void checkAndAddComponent(Set<String> entryPoints, IAndroidComponent component) {
		final String packageName = getPackageName() + ".";
		if (component.isEnabled()) {
			String className = component.getNameString();
			if (className != null && !className.isEmpty()) {
				if (className.startsWith(packageName) || !SystemClassHandler.v().isClassInSystemPackage(className))
					entryPoints.add(className);
			}
		}
	}

}
