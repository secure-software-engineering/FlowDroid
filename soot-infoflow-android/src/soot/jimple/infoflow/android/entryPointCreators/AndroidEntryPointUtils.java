package soot.jimple.infoflow.android.entryPointCreators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.FastHierarchy;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Class containing common utility methods for dealing with Android entry points
 * 
 * @author Steven Arzt
 *
 */
public class AndroidEntryPointUtils {

	private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointUtils.class);

	private Map<SootClass, ComponentType> componentTypeCache = new HashMap<>();

	private SootClass osClassApplication;
	private SootClass osClassActivity;
	private SootClass osClassMapActivity;
	private SootClass osClassService;
	private SootClass osClassFragment;
	private SootClass osClassSupportFragment;
	private SootClass osClassAndroidXFragment;
	private SootClass osClassBroadcastReceiver;
	private SootClass osClassContentProvider;
	private SootClass osClassGCMBaseIntentService;
	private SootClass osClassGCMListenerService;
	private SootClass osClassHostApduService;
	private SootClass osInterfaceServiceConnection;

	/**
	 * Array containing all types of components supported in Android lifecycles
	 */
	public enum ComponentType {
		Application, Activity, Service, Fragment, BroadcastReceiver, ContentProvider, GCMBaseIntentService,
		GCMListenerService, HostApduService, ServiceConnection, Plain
	}

	/**
	 * Creates a new instance of the {@link AndroidEntryPointUtils} class. Soot must
	 * already be running when this constructor is invoked.
	 */
	public AndroidEntryPointUtils() {
		// Get some commonly used OS classes
		osClassApplication = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.APPLICATIONCLASS);
		osClassActivity = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.ACTIVITYCLASS);
		osClassService = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.SERVICECLASS);
		osClassFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.FRAGMENTCLASS);
		osClassSupportFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.SUPPORTFRAGMENTCLASS);
		osClassAndroidXFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.ANDROIDXFRAGMENTCLASS);
		osClassBroadcastReceiver = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS);
		osClassContentProvider = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.CONTENTPROVIDERCLASS);
		osClassGCMBaseIntentService = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS);
		osClassGCMListenerService = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.GCMLISTENERSERVICECLASS);
		osClassHostApduService = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.HOSTAPDUSERVICECLASS);
		osInterfaceServiceConnection = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.SERVICECONNECTIONINTERFACE);
		osClassMapActivity = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.MAPACTIVITYCLASS);
	}

	/**
	 * Gets the type of component represented by the given Soot class
	 * 
	 * @param currentClass The class for which to get the component type
	 * @return The component type of the given class
	 */
	public ComponentType getComponentType(SootClass currentClass) {
		if (componentTypeCache.containsKey(currentClass))
			return componentTypeCache.get(currentClass);

		// Check the type of this class
		ComponentType ctype = ComponentType.Plain;
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

		if (fh != null) {
			// We first look for the specialized types

			// (a1) android.app.Fragment
			if (osClassFragment != null && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(),
					osClassFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassSupportFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassSupportFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassAndroidXFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassAndroidXFragment.getType()))
				ctype = ComponentType.Fragment;
			// (a2) com.google.android.gcm.GCMBaseIntentService
			else if (osClassGCMBaseIntentService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMBaseIntentService.getType()))
				ctype = ComponentType.GCMBaseIntentService;
			// (a3) com.google.android.gms.gcm.GcmListenerService
			else if (osClassGCMListenerService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMListenerService.getType()))
				ctype = ComponentType.GCMListenerService;
			// (a4) android.nfc.cardemulation.HostApduService
			else if (osClassHostApduService != null
					&& fh.canStoreType(currentClass.getType(), osClassHostApduService.getType()))
				ctype = ComponentType.HostApduService;
			// (a5) android.content.ServiceConnection
			else if (osInterfaceServiceConnection != null
					&& fh.canStoreType(currentClass.getType(), osInterfaceServiceConnection.getType()))
				ctype = ComponentType.ServiceConnection;
			// (a6) com.google.android.maps.MapActivity
			else if (osClassMapActivity != null
					&& fh.canStoreType(currentClass.getType(), osClassMapActivity.getType()))
				ctype = ComponentType.Activity;

			// If the given class is not a specific type of component, we look upwards in
			// the hierarchy to see if we have something more generic
			// (b1) android.app.Application
			else if (osClassApplication != null
					&& fh.canStoreType(currentClass.getType(), osClassApplication.getType()))
				ctype = ComponentType.Application;
			// (b2) android.app.Service
			else if (osClassService != null && fh.canStoreType(currentClass.getType(), osClassService.getType()))
				ctype = ComponentType.Service;
			// (b3) android.app.Activity
			else if (osClassActivity != null && fh.canStoreType(currentClass.getType(), osClassActivity.getType()))
				ctype = ComponentType.Activity;
			// (b4) android.app.BroadcastReceiver
			else if (osClassBroadcastReceiver != null
					&& fh.canStoreType(currentClass.getType(), osClassBroadcastReceiver.getType()))
				ctype = ComponentType.BroadcastReceiver;
			// (b5) android.app.ContentProvider
			else if (osClassContentProvider != null
					&& fh.canStoreType(currentClass.getType(), osClassContentProvider.getType()))
				ctype = ComponentType.ContentProvider;
		} else
			logger.warn(String.format("No FastHierarchy, assuming %s is a plain class", currentClass.getName()));

		componentTypeCache.put(currentClass, ctype);
		return ctype;
	}

	/**
	 * Checks whether the given class is derived from android.app.Application
	 * 
	 * @param clazz The class to check
	 * @return True if the given class is derived from android.app.Application,
	 *         otherwise false
	 */
	public boolean isApplicationClass(SootClass clazz) {
		return osClassApplication != null
				&& Scene.v().getOrMakeFastHierarchy().canStoreType(clazz.getType(), osClassApplication.getType());
	}

	/**
	 * Checks whether the given method is an Android entry point, i.e., a lifecycle
	 * method
	 * 
	 * @param method The method to check
	 * @return True if the given method is a lifecycle method, otherwise false
	 */
	public boolean isEntryPointMethod(SootMethod method) {
		if (method == null)
			throw new IllegalArgumentException("Given method is null");
		ComponentType componentType = getComponentType(method.getDeclaringClass());
		String subsignature = method.getSubSignature();

		if (componentType == ComponentType.Activity
				&& AndroidEntryPointConstants.getActivityLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Service
				&& AndroidEntryPointConstants.getServiceLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Application
				&& AndroidEntryPointConstants.getApplicationLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Fragment
				&& AndroidEntryPointConstants.getFragmentLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.BroadcastReceiver
				&& AndroidEntryPointConstants.getBroadcastLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ContentProvider
				&& AndroidEntryPointConstants.getContentproviderLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMBaseIntentService
				&& AndroidEntryPointConstants.getGCMIntentServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMListenerService
				&& AndroidEntryPointConstants.getGCMListenerServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ServiceConnection
				&& AndroidEntryPointConstants.getServiceConnectionMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.HostApduService
				&& AndroidEntryPointConstants.getHostApduServiceMethods().contains(subsignature))
			return true;

		return false;
	}

	/**
	 * Gets all lifecycle methods in the given entry point class
	 * 
	 * @param sc The class in which to look for lifecycle methods
	 * @return The set of lifecycle methods in the given class
	 */
	public Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc) {
		return getLifecycleMethods(getComponentType(sc), sc);
	}

	/**
	 * Gets all lifecycle methods in the given entry point class
	 * 
	 * @param componentType the component type
	 * @param sc            The class in which to look for lifecycle methods
	 * @return The set of lifecycle methods in the given class
	 */
	public static Collection<? extends MethodOrMethodContext> getLifecycleMethods(ComponentType componentType,
			SootClass sc) {
		switch (componentType) {
		case Activity:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getActivityLifecycleMethods());
		case Service:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getServiceLifecycleMethods());
		case Application:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getApplicationLifecycleMethods());
		case BroadcastReceiver:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getBroadcastLifecycleMethods());
		case Fragment:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getFragmentLifecycleMethods());
		case ContentProvider:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getContentproviderLifecycleMethods());
		case GCMBaseIntentService:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getGCMIntentServiceMethods());
		case GCMListenerService:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getGCMListenerServiceMethods());
		case ServiceConnection:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getServiceConnectionMethods());
		case Plain:
			return Collections.emptySet();
		}
		return Collections.emptySet();
	}

	/**
	 * This method takes a lifecycle class and the list of lifecycle method
	 * subsignatures. For each subsignature, it checks whether the given class or
	 * one of its superclass overwrites the respective methods. All findings are
	 * collected in a set and returned.
	 * 
	 * @param sc      The class in which to look for lifecycle method
	 *                implementations
	 * @param methods The list of lifecycle method subsignatures for the type of
	 *                component that the given class corresponds to
	 * @return The set of implemented lifecycle methods in the given class
	 */
	private static Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc, List<String> methods) {
		Set<MethodOrMethodContext> lifecycleMethods = new HashSet<>();
		SootClass currentClass = sc;
		while (currentClass != null) {
			for (String sig : methods) {
				SootMethod sm = currentClass.getMethodUnsafe(sig);
				if (sm != null)
					if (!SystemClassHandler.v().isClassInSystemPackage(sm.getDeclaringClass().getName()))
						lifecycleMethods.add(sm);
			}
			currentClass = currentClass.hasSuperclass() ? currentClass.getSuperclass() : null;
		}
		return lifecycleMethods;
	}

}
