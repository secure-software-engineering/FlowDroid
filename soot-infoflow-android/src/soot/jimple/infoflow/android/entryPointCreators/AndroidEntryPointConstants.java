/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.entryPointCreators;

import java.util.Arrays;
import java.util.List;

/**
 * Class containing constants for the well-known Android lifecycle methods
 */
public class AndroidEntryPointConstants {

	/*
	 * ========================================================================
	 */

	public static final String ACTIVITYCLASS = "android.app.Activity";
	public static final String SERVICECLASS = "android.app.Service";
	public static final String GCMBASEINTENTSERVICECLASS = "com.google.android.gcm.GCMBaseIntentService";
	public static final String GCMLISTENERSERVICECLASS = "com.google.android.gms.gcm.GcmListenerService";
	public static final String HOSTAPDUSERVICECLASS = "android.nfc.cardemulation.HostApduService";
	public static final String BROADCASTRECEIVERCLASS = "android.content.BroadcastReceiver";
	public static final String CONTENTPROVIDERCLASS = "android.content.ContentProvider";
	public static final String APPLICATIONCLASS = "android.app.Application";
	public static final String FRAGMENTCLASS = "android.app.Fragment";
	public static final String SUPPORTFRAGMENTCLASS = "android.support.v4.app.Fragment";
	public static final String ANDROIDXFRAGMENTCLASS = "androidx.fragment.app.Fragment";
	public static final String SERVICECONNECTIONINTERFACE = "android.content.ServiceConnection";
	public static final String MAPACTIVITYCLASS = "com.google.android.maps.MapActivity";

	public static final String APPCOMPATACTIVITYCLASS_V4 = "android.support.v4.app.AppCompatActivity";
	public static final String APPCOMPATACTIVITYCLASS_V7 = "android.support.v7.app.AppCompatActivity";
	public static final String APPCOMPATACTIVITYCLASS_X = "androidx.appcompat.app.AppCompatActivity";

	public static final String ACTIVITY_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONSTART = "void onStart()";
	public static final String ACTIVITY_ONRESTOREINSTANCESTATE = "void onRestoreInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPOSTCREATE = "void onPostCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONRESUME = "void onResume()";
	public static final String ACTIVITY_ONPOSTRESUME = "void onPostResume()";
	public static final String ACTIVITY_ONCREATEDESCRIPTION = "java.lang.CharSequence onCreateDescription()";
	public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPAUSE = "void onPause()";
	public static final String ACTIVITY_ONSTOP = "void onStop()";
	public static final String ACTIVITY_ONRESTART = "void onRestart()";
	public static final String ACTIVITY_ONDESTROY = "void onDestroy()";
	public static final String ACTIVITY_ONATTACHFRAGMENT = "void onAttachFragment(android.app.Fragment)";

	public static final String SERVICE_ONCREATE = "void onCreate()";
	public static final String SERVICE_ONSTART1 = "void onStart(android.content.Intent,int)";
	public static final String SERVICE_ONSTART2 = "int onStartCommand(android.content.Intent,int,int)";
	public static final String SERVICE_ONBIND = "android.os.IBinder onBind(android.content.Intent)";
	public static final String SERVICE_ONREBIND = "void onRebind(android.content.Intent)";
	public static final String SERVICE_ONUNBIND = "boolean onUnbind(android.content.Intent)";
	public static final String SERVICE_ONDESTROY = "void onDestroy()";

	public static final String GCMINTENTSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages(android.content.Context,int)";
	public static final String GCMINTENTSERVICE_ONERROR = "void onError(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONMESSAGE = "void onMessage(android.content.Context,android.content.Intent)";
	public static final String GCMINTENTSERVICE_ONRECOVERABLEERROR = "void onRecoverableError(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONREGISTERED = "void onRegistered(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONUNREGISTERED = "void onUnregistered(android.content.Context,java.lang.String)";

	public static final String GCMLISTENERSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages()";
	public static final String GCMLISTENERSERVICE_ONMESSAGERECEIVED = "void onMessageReceived(java.lang.String,android.os.Bundle)";
	public static final String GCMLISTENERSERVICE_ONMESSAGESENT = "void onMessageSent(java.lang.String)";
	public static final String GCMLISTENERSERVICE_ONSENDERROR = "void onSendError(java.lang.String,java.lang.String)";

	public static final String HOSTAPDUSERVICE_PROCESSCOMMANDAPDU = "byte[] processCommandApdu(byte[],android.os.Bundle)";
	public static final String HOSTAPDUSERVICE_ONDEACTIVATED = "void onDeactivated(int)";

	public static final String FRAGMENT_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String FRAGMENT_ONATTACH = "void onAttach(android.app.Activity)";
	public static final String FRAGMENT_ONCREATEVIEW = "android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)";
	public static final String FRAGMENT_ONVIEWCREATED = "void onViewCreated(android.view.View,android.os.Bundle)";
	public static final String FRAGMENT_ONSTART = "void onStart()";
	public static final String FRAGMENT_ONACTIVITYCREATED = "void onActivityCreated(android.os.Bundle)";
	public static final String FRAGMENT_ONVIEWSTATERESTORED = "void onViewStateRestored(android.app.Activity)";
	public static final String FRAGMENT_ONRESUME = "void onResume()";
	public static final String FRAGMENT_ONPAUSE = "void onPause()";
	public static final String FRAGMENT_ONSTOP = "void onStop()";
	public static final String FRAGMENT_ONDESTROYVIEW = "void onDestroyView()";
	public static final String FRAGMENT_ONDESTROY = "void onDestroy()";
	public static final String FRAGMENT_ONDETACH = "void onDetach()";
	public static final String FRAGMENT_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";

	public static final String BROADCAST_ONRECEIVE = "void onReceive(android.content.Context,android.content.Intent)";

	public static final String CONTENTPROVIDER_ONCREATE = "boolean onCreate()";
	public static final String CONTENTPROVIDER_INSERT = "android.net.Uri insert(android.net.Uri,android.content.ContentValues)";
	public static final String CONTENTPROVIDER_QUERY = "android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)";
	public static final String CONTENTPROVIDER_UPDATE = "int update(android.net.Uri,android.content.ContentValues,java.lang.String,java.lang.String[])";
	public static final String CONTENTPROVIDER_DELETE = "int delete(android.net.Uri,java.lang.String,java.lang.String[])";
	public static final String CONTENTPROVIDER_GETTYPE = "java.lang.String getType(android.net.Uri)";

	public static final String APPLICATION_ONCREATE = "void onCreate()";
	public static final String APPLICATION_ONTERMINATE = "void onTerminate()";

	public static final String SERVICECONNECTION_ONSERVICECONNECTED = "void onServiceConnected(android.content.ComponentName,android.os.IBinder)";
	public static final String SERVICECONNECTION_ONSERVICEDISCONNECTED = "void onServiceDisconnected(android.content.ComponentName)";

	public static final String ACTIVITYLIFECYCLECALLBACKSINTERFACE = "android.app.Application$ActivityLifecycleCallbacks";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED = "void onActivityStarted(android.app.Activity)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED = "void onActivityStopped(android.app.Activity)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE = "void onActivitySaveInstanceState(android.app.Activity,android.os.Bundle)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED = "void onActivityResumed(android.app.Activity)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED = "void onActivityPaused(android.app.Activity)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED = "void onActivityDestroyed(android.app.Activity)";
	public static final String ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED = "void onActivityCreated(android.app.Activity,android.os.Bundle)";

	public static final String COMPONENTCALLBACKSINTERFACE = "android.content.ComponentCallbacks";
	public static final String COMPONENTCALLBACKS_ONLOWMEMORY = "void onLowMemory()";
	public static final String COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED = "void onConfigurationChanged(android.content.res.Configuration)";

	public static final String COMPONENTCALLBACKS2INTERFACE = "android.content.ComponentCallbacks2";
	public static final String COMPONENTCALLBACKS2_ONTRIMMEMORY = "void onTrimMemory(int)";

	/*
	 * ========================================================================
	 */

	private static final String[] activityMethods = { ACTIVITY_ONCREATE, ACTIVITY_ONDESTROY, ACTIVITY_ONPAUSE,
			ACTIVITY_ONRESTART, ACTIVITY_ONRESUME, ACTIVITY_ONSTART, ACTIVITY_ONSTOP, ACTIVITY_ONSAVEINSTANCESTATE,
			ACTIVITY_ONRESTOREINSTANCESTATE, ACTIVITY_ONCREATEDESCRIPTION, ACTIVITY_ONPOSTCREATE, ACTIVITY_ONPOSTRESUME,
			ACTIVITY_ONATTACHFRAGMENT };
	private static final List<String> activityMethodList = Arrays.asList(activityMethods);

	private static final String[] serviceMethods = { SERVICE_ONCREATE, SERVICE_ONDESTROY, SERVICE_ONSTART1,
			SERVICE_ONSTART2, SERVICE_ONBIND, SERVICE_ONREBIND, SERVICE_ONUNBIND };
	private static final List<String> serviceMethodList = Arrays.asList(serviceMethods);

	private static final String[] fragmentMethods = { FRAGMENT_ONCREATE, FRAGMENT_ONDESTROY, FRAGMENT_ONPAUSE,
			FRAGMENT_ONATTACH, FRAGMENT_ONDESTROYVIEW, FRAGMENT_ONRESUME, FRAGMENT_ONSTART, FRAGMENT_ONSTOP,
			FRAGMENT_ONCREATEVIEW, FRAGMENT_ONACTIVITYCREATED, FRAGMENT_ONVIEWSTATERESTORED, FRAGMENT_ONDETACH,
			FRAGMENT_ONSAVEINSTANCESTATE, FRAGMENT_ONVIEWCREATED };
	private static final List<String> fragmentMethodList = Arrays.asList(fragmentMethods);

	private static final String[] gcmIntentServiceMethods = { GCMINTENTSERVICE_ONDELETEDMESSAGES,
			GCMINTENTSERVICE_ONERROR, GCMINTENTSERVICE_ONMESSAGE, GCMINTENTSERVICE_ONRECOVERABLEERROR,
			GCMINTENTSERVICE_ONREGISTERED, GCMINTENTSERVICE_ONUNREGISTERED };
	private static final List<String> gcmIntentServiceMethodList = Arrays.asList(gcmIntentServiceMethods);

	private static final String[] gcmListenerServiceMethods = { GCMLISTENERSERVICE_ONDELETEDMESSAGES,
			GCMLISTENERSERVICE_ONMESSAGERECEIVED, GCMLISTENERSERVICE_ONMESSAGESENT, GCMLISTENERSERVICE_ONSENDERROR };
	private static final List<String> gcmListenerServiceMethodList = Arrays.asList(gcmListenerServiceMethods);

	private static final String[] hostApduServiceMethods = { HOSTAPDUSERVICE_PROCESSCOMMANDAPDU,
			HOSTAPDUSERVICE_ONDEACTIVATED };
	private static final List<String> hostApduServiceMethodList = Arrays.asList(hostApduServiceMethods);

	private static final String[] broadcastMethods = { BROADCAST_ONRECEIVE };
	private static final List<String> broadcastMethodList = Arrays.asList(broadcastMethods);

	private static final String[] contentproviderMethods = { CONTENTPROVIDER_ONCREATE, CONTENTPROVIDER_DELETE,
			CONTENTPROVIDER_GETTYPE, CONTENTPROVIDER_INSERT, CONTENTPROVIDER_QUERY, CONTENTPROVIDER_UPDATE };
	private static final List<String> contentProviderMethodList = Arrays.asList(contentproviderMethods);

	private static final String[] applicationMethods = { APPLICATION_ONCREATE, APPLICATION_ONTERMINATE };
	private static final List<String> applicationMethodList = Arrays.asList(applicationMethods);

	private static final String[] activityLifecycleMethods = { ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED };
	private static final List<String> activityLifecycleMethodList = Arrays.asList(activityLifecycleMethods);

	private static final String[] componentCallbackMethods = { COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED,
			COMPONENTCALLBACKS_ONLOWMEMORY };
	private static final List<String> componentCallbackMethodList = Arrays.asList(componentCallbackMethods);

	public static final String[] componentCallback2Methods = { COMPONENTCALLBACKS2_ONTRIMMEMORY };
	public static final List<String> componentCallback2MethodList = Arrays.asList(componentCallback2Methods);

	private static final String[] serviceConnectionMethods = { SERVICECONNECTION_ONSERVICECONNECTED,
			SERVICECONNECTION_ONSERVICEDISCONNECTED };
	private static final List<String> serviceConnectionMethodList = Arrays.asList(serviceConnectionMethods);
	/*
	 * ========================================================================
	 */

	public static List<String> getActivityLifecycleMethods() {
		return activityMethodList;
	}

	public static List<String> getServiceLifecycleMethods() {
		return serviceMethodList;
	}

	public static List<String> getFragmentLifecycleMethods() {
		return fragmentMethodList;
	}

	public static List<String> getGCMIntentServiceMethods() {
		return gcmIntentServiceMethodList;
	}

	public static List<String> getGCMListenerServiceMethods() {
		return gcmListenerServiceMethodList;
	}

	public static List<String> getHostApduServiceMethods() {
		return hostApduServiceMethodList;
	}

	public static List<String> getBroadcastLifecycleMethods() {
		return broadcastMethodList;
	}

	public static List<String> getContentproviderLifecycleMethods() {
		return contentProviderMethodList;
	}

	public static List<String> getApplicationLifecycleMethods() {
		return applicationMethodList;
	}

	public static List<String> getActivityLifecycleCallbackMethods() {
		return activityLifecycleMethodList;
	}

	public static List<String> getComponentCallbackMethods() {
		return componentCallbackMethodList;
	}

	public static List<String> getComponentCallback2Methods() {
		return componentCallback2MethodList;
	}

	public static List<String> getServiceConnectionMethods() {
		return serviceConnectionMethodList;
	}

	/*
	 * ========================================================================
	 */

	/**
	 * Gets whether the given class if one of Android's default lifecycle classes
	 * (android.app.Activity etc.)
	 * 
	 * @param className The name of the class to check
	 * @return True if the given class is one of Android's default lifecycle
	 *         classes, otherwise false
	 */
	public static boolean isLifecycleClass(String className) {
		return className.equals(ACTIVITYCLASS) || className.equals(SERVICECLASS) || className.equals(FRAGMENTCLASS)
				|| className.equals(BROADCASTRECEIVERCLASS) || className.equals(CONTENTPROVIDERCLASS)
				|| className.equals(APPLICATIONCLASS) || className.equals(APPCOMPATACTIVITYCLASS_V4)
				|| className.equals(APPCOMPATACTIVITYCLASS_V7) || className.equals(APPCOMPATACTIVITYCLASS_X);
	}

	/**
	 * Do not allow anyone to instantiate this class.
	 */
	private AndroidEntryPointConstants() {

	}

	/**
	 * Checks whether the given subsignature corresponds to the subsignature of a
	 * lifecycle method
	 * 
	 * @param subsig The subsignature
	 * @return True if the given subsignature is potentially a lifecycle method
	 *         (depending on the parent class which is not checked here), false
	 *         otherwise
	 */
	public static boolean isLifecycleSubsignature(String subsig) {
		return activityMethodList.contains(subsig) || serviceMethodList.contains(subsig)
				|| fragmentMethodList.contains(subsig) || gcmIntentServiceMethodList.contains(subsig)
				|| gcmListenerServiceMethodList.contains(subsig) || hostApduServiceMethodList.contains(subsig)
				|| broadcastMethodList.contains(subsig) || contentProviderMethodList.contains(subsig)
				|| applicationMethodList.contains(subsig) || activityLifecycleMethodList.contains(subsig)
				|| componentCallbackMethodList.contains(subsig) || componentCallback2MethodList.contains(subsig)
				|| serviceConnectionMethodList.contains(subsig);
	}

}
