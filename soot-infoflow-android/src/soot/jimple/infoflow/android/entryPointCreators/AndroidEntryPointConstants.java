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
	public static final String ACTIVITY_ONCREATE2 = "void onCreate(android.os.Bundle,android.os.PersistableBundle)";
	public static final String ACTIVITY_ONSTART = "void onStart()";
	public static final String ACTIVITY_ONRESTOREINSTANCESTATE = "void onRestoreInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPOSTCREATE = "void onPostCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONPOSTCREATE2 = "void onPostCreate(android.os.Bundle,android.os.PersistableBundle)";
	public static final String ACTIVITY_ONRESUME = "void onResume()";
	public static final String ACTIVITY_ONPOSTRESUME = "void onPostResume()";
	public static final String ACTIVITY_ONCREATEDESCRIPTION = "java.lang.CharSequence onCreateDescription()";
	public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONSAVEINSTANCESTATE2 = "void onSaveInstanceState(android.os.Bundle,android.os.PersistableBundle)";

	public static final String ACTIVITY_ONPAUSE = "void onPause()";
	public static final String ACTIVITY_ONSTOP = "void onStop()";
	public static final String ACTIVITY_ONRESTART = "void onRestart()";
	public static final String ACTIVITY_ONDESTROY = "void onDestroy()";
	public static final String ACTIVITY_ONATTACHFRAGMENT = "void onAttachFragment(android.app.Fragment)";

	public static final String ACTIVITY_ONGENERICMOTIONEVENT = "boolean onGenericMotionEvent(android.view.MotionEvent)";
	public static final String ACTIVITY_ONENTERANIMATIONCOMPLETE = "void onEnterAnimationComplete()";
	public static final String ACTIVITY_ONGETDIRECTACTIONS = "void onGetDirectActions(android.os.CancellationSignal,java.util.function.Consumer)";
	public static final String ACTIVITY_ONCONTEXTITEMSELECTED = "boolean onContextItemSelected(android.view.MenuItem)";
	public static final String ACTIVITY_ONSEARCHREQUESTED = "boolean onSearchRequested()";
	public static final String ACTIVITY_ONSEARCHREQUESTED2 = "boolean onSearchRequested(android.view.SearchEvent)";
	public static final String ACTIVITY_ONKEYSHORTCUT = "boolean onKeyShortcut(int,android.view.KeyEvent)";
	public static final String ACTIVITY_ONKEYDOWN = "boolean onKeyDown(int,android.view.KeyEvent)";
	public static final String ACTIVITY_ONKEYMULTIPLE = "boolean onKeyMultiple(int,int,android.view.KeyEvent)";
	public static final String ACTIVITY_ONCHILDTITLECHANGED = "void onChildTitleChanged(android.app.Activity,java.lang.CharSequence)";
	public static final String ACTIVITY_ONWINDOWSTARTINGACTIONMODE = "android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode$Callback)";
	public static final String ACTIVITY_ONWINDOWSTARTINGACTIONMODE2 = "android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode$Callback,int)";
	public static final String ACTIVITY_OVERRIDEPENDINGTRANSITION = "void overridePendingTransition(int,int)";
	public static final String ACTIVITY_OVERRIDEPENDINGTRANSITION2 = "void overridePendingTransition(int,int,int)";
	public static final String ACTIVITY_ONPICTUREINPICTUREMODECHANGED = "void onPictureInPictureModeChanged(boolean,android.content.res.Configuration)";
	public static final String ACTIVITY_ONPICTUREINPICTUREMODECHANGED2 = "void onPictureInPictureModeChanged(boolean)";
	public static final String ACTIVITY_ONTOUCHEVENT = "boolean onTouchEvent(android.view.MotionEvent)";
	public static final String ACTIVITY_ONACTIONMODESTARTED = "void onActionModeStarted(android.view.ActionMode)";
	public static final String ACTIVITY_ONNAVIGATEUP = "boolean onNavigateUp()";
	public static final String ACTIVITY_ONMULTIWINDOWMODECHANGED = "void onMultiWindowModeChanged(boolean)";
	public static final String ACTIVITY_ONMULTIWINDOWMODECHANGED2 = "void onMultiWindowModeChanged(boolean,android.content.res.Configuration)";
	public static final String ACTIVITY_ONPICTUREINPICTUREREQUESTED = "boolean onPictureInPictureRequested()";
	public static final String ACTIVITY_ONPREPAREOPTIONSMENU = "boolean onPrepareOptionsMenu(android.view.Menu)";
	public static final String ACTIVITY_ONPREPAREDIALOG = "void onPrepareDialog(int,android.app.Dialog)";
	public static final String ACTIVITY_ONPREPAREDIALOG2 = "void onPrepareDialog(int,android.app.Dialog,android.os.Bundle)";
	public static final String ACTIVITY_ONPROVIDEASSISTDATA = "void onProvideAssistData(android.os.Bundle)";
	public static final String ACTIVITY_ONPREPARENAVIGATEUPTASKSTACK = "void onPrepareNavigateUpTaskStack(android.app.TaskStackBuilder)";
	public static final String ACTIVITY_ONOPTIONSMENUCLOSED = "void onOptionsMenuClosed(android.view.Menu)";
	public static final String ACTIVITY_ONCONTEXTMENUCLOSED = "void onContextMenuClosed(android.view.Menu)";
	public static final String ACTIVITY_ONOPTIONSITEMSELECTED = "boolean onOptionsItemSelected(android.view.MenuItem)";
	public static final String ACTIVITY_ONTITLECHANGED = "void onTitleChanged(java.lang.CharSequence,int)";
	public static final String ACTIVITY_ONUSERLEAVEHINT = "void onUserLeaveHint()";
	public static final String ACTIVITY_ONPROVIDEKEYBOARDSHORTCUTS = "void onProvideKeyboardShortcuts(java.util.List,android.view.Menu,int)";
	public static final String ACTIVITY_ONPROVIDEASSISTCONTENT = "void onProvideAssistContent(android.app.assist.AssistContent)";
	public static final String ACTIVITY_ONAPPLYTHEMERESOURCE = "void onApplyThemeResource(android.content.res.Resources$Theme,int,boolean)";
	public static final String ACTIVITY_ONKEYLONGPRESS = "boolean onKeyLongPress(int,android.view.KeyEvent)";
	public static final String ACTIVITY_ONKEYUP = "boolean onKeyUp(int,android.view.KeyEvent)";
	public static final String ACTIVITY_ONCREATEVIEW = "android.view.View onCreateView(java.lang.String,android.content.Context,android.util.AttributeSet)";
	public static final String ACTIVITY_ONCREATENAVIGATEUPTASKSTACK = "void onCreateNavigateUpTaskStack(android.app.TaskStackBuilder)";
	public static final String ACTIVITY_ONPICTUREINPICTUREUISTATECHANGED = "void onPictureInPictureUiStateChanged(android.app.PictureInPictureUiState)";
	public static final String ACTIVITY_ONMENUOPENED = "boolean onMenuOpened(int,android.view.Menu)";
	public static final String ACTIVITY_ONLOCALVOICEINTERACTIONSTARTED = "void onLocalVoiceInteractionStarted()";
	public static final String ACTIVITY_ONLOCALVOICEINTERACTIONSTOPPED = "void onLocalVoiceInteractionStopped()";
	public static final String ACTIVITY_ONTOPRESUMEDACTIVITYCHANGED = "void onTopResumedActivityChanged(boolean)";
	public static final String ACTIVITY_ONATTACHEDTOWINDOW = "void onAttachedToWindow()";
	public static final String ACTIVITY_ONWINDOWFOCUSCHANGED = "void onWindowFocusChanged(boolean)";
	public static final String ACTIVITY_ONWINDOWATTRIBUTESCHANGED = "void onWindowAttributesChanged(android.view.WindowManager$LayoutParams)";
	public static final String ACTIVITY_ONBACKPRESSED = "void onBackPressed()";
	public static final String ACTIVITY_ONSTATENOTSAVED = "void onStateNotSaved()";
	public static final String ACTIVITY_ONPANELCLOSED = "void onPanelClosed(int,android.view.Menu)";
	public static final String ACTIVITY_ONTRACKBALLEVENT = "boolean onTrackballEvent(android.view.MotionEvent)";
	public static final String ACTIVITY_ONCREATETHUMBNAIL = "boolean onCreateThumbnail(android.graphics.Bitmap,android.graphics.Canvas)";
	public static final String ACTIVITY_ONMENUITEMSELECTED = "boolean onMenuItemSelected(int,android.view.MenuItem)";
	public static final String ACTIVITY_ONCREATECONTEXTMENU = "void onCreateContextMenu(android.view.ContextMenu,android.view.View,android.view.ContextMenu$ContextMenuInfo)";

	public static final String SERVICE_ONCREATE = "void onCreate()";
	public static final String SERVICE_ONSTART1 = "void onStart(android.content.Intent,int)";
	public static final String SERVICE_ONSTART2 = "int onStartCommand(android.content.Intent,int,int)";
	public static final String SERVICE_ONBIND = "android.os.IBinder onBind(android.content.Intent)";
	public static final String SERVICE_ONREBIND = "void onRebind(android.content.Intent)";
	public static final String SERVICE_ONUNBIND = "boolean onUnbind(android.content.Intent)";
	public static final String SERVICE_ONTIMEOUT = "void onTimeout(int)";
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
	public static final String CONTENTPROVIDER_INSERT2 = "android.net.Uri insert(android.net.Uri,android.content.ContentValues,android.os.Bundle)";
	public static final String CONTENTPROVIDER_UPDATE2 = "int update(android.net.Uri,android.content.ContentValues,android.os.Bundle)";
	public static final String CONTENTPROVIDER_APPLYBATCH = "android.content.ContentProviderResult[] applyBatch(java.lang.String,java.util.ArrayList)";
	public static final String CONTENTPROVIDER_APPLYBATCH2 = "android.content.ContentProviderResult[] applyBatch(java.util.ArrayList)";
	public static final String CONTENTPROVIDER_OPENTYPEDASSETFILE = "android.content.res.AssetFileDescriptor openTypedAssetFile(android.net.Uri,java.lang.String,android.os.Bundle)";
	public static final String CONTENTPROVIDER_BULKINSERT = "int bulkInsert(android.net.Uri,android.content.ContentValues[])";
	public static final String CONTENTPROVIDER_QUERY2 = "android.database.Cursor query(android.net.Uri,java.lang.String[],android.os.Bundle,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_QUERY3 = "android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_OPENTYPEDASSETFILE2 = "android.content.res.AssetFileDescriptor openTypedAssetFile(android.net.Uri,java.lang.String,android.os.Bundle,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_CALL = "android.os.Bundle call(java.lang.String,java.lang.String,java.lang.String,android.os.Bundle)";
	public static final String CONTENTPROVIDER_CALL2 = "android.os.Bundle call(java.lang.String,java.lang.String,android.os.Bundle)";
	public static final String CONTENTPROVIDER_GETTYPEANONYMOUS = "java.lang.String getTypeAnonymous(android.net.Uri)";
	public static final String CONTENTPROVIDER_OPENPIPEHELPER = "android.os.ParcelFileDescriptor openPipeHelper(android.net.Uri,java.lang.String,android.os.Bundle,java.lang.Object,android.content.ContentProvider$PipeDataWriter)";
	public static final String CONTENTPROVIDER_CANONICALIZE = "android.net.Uri canonicalize(android.net.Uri)";
	public static final String CONTENTPROVIDER_UNCANONICALIZE = "android.net.Uri uncanonicalize(android.net.Uri)";
	public static final String CONTENTPROVIDER_OPENFILE = "android.os.ParcelFileDescriptor openFile(android.net.Uri,java.lang.String,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_OPENFILE2 = "android.os.ParcelFileDescriptor openFile(android.net.Uri,java.lang.String)";
	public static final String CONTENTPROVIDER_GETSTREAMTYPES = "java.lang.String[] getStreamTypes(android.net.Uri,java.lang.String)";
	public static final String CONTENTPROVIDER_ISTEMPORARY = "boolean isTemporary()";
	public static final String CONTENTPROVIDER_ATTACHINFO = "void attachInfo(android.content.Context,android.content.pm.ProviderInfo)";
	public static final String CONTENTPROVIDER_ONCALLINGPACKAGECHANGED = "void onCallingPackageChanged()";
	public static final String CONTENTPROVIDER_OPENASSETFILE = "android.content.res.AssetFileDescriptor openAssetFile(android.net.Uri,java.lang.String)";
	public static final String CONTENTPROVIDER_OPENASSETFILE2 = "android.content.res.AssetFileDescriptor openAssetFile(android.net.Uri,java.lang.String,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_REFRESH = "boolean refresh(android.net.Uri,android.os.Bundle,android.os.CancellationSignal)";
	public static final String CONTENTPROVIDER_DELETE2 = "int delete(android.net.Uri,android.os.Bundle)";
	public static final String CONTENTPROVIDER_DUMP = "void dump(java.io.FileDescriptor,java.io.PrintWriter,java.lang.String[])";

	public static final String APPLICATION_ONCREATE = "void onCreate()";
	public static final String APPLICATION_ONTERMINATE = "void onTerminate()";

	public static final String SERVICECONNECTION_ONSERVICECONNECTED = "void onServiceConnected(android.content.ComponentName,android.os.IBinder)";
	public static final String SERVICECONNECTION_ONSERVICEDISCONNECTED = "void onServiceDisconnected(android.content.ComponentName)";

	public static final String APPCOMPONENTFACTORYCLASS = "android.app.AppComponentFactory";
	public static final String APPCOMPONENTFACTORY_INSTANTIATEACTIVITY = "android.app.Activity instantiateActivity(java.lang.ClassLoader,java.lang.String,android.content.Intent)";
	public static final String APPCOMPONENTFACTORY_INSTANTIATERECEIVER = "android.content.BroadcastReceiver instantiateReceiver(java.lang.ClassLoader,java.lang.String,android.content.Intent)";
	public static final String APPCOMPONENTFACTORY_INSTANTIATEAPPLICATION = "android.app.Application instantiateApplication(java.lang.ClassLoader,java.lang.String)";
	public static final String APPCOMPONENTFACTORY_INSTANTIATECLASSLOADER = "java.lang.ClassLoader instantiateClassLoader(java.lang.ClassLoader,android.content.pm.ApplicationInfo)";
	public static final String APPCOMPONENTFACTORY_INSTANTIATEPROVIDER = "android.content.ContentProvider instantiateProvider(java.lang.ClassLoader,java.lang.String)";

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

	private static final String[] activityMethods = { ACTIVITY_ONCREATE, ACTIVITY_ONCREATE2, ACTIVITY_ONDESTROY,
			ACTIVITY_ONPAUSE, ACTIVITY_ONRESTART, ACTIVITY_ONRESUME, ACTIVITY_ONSTART, ACTIVITY_ONSTOP,
			ACTIVITY_ONSAVEINSTANCESTATE2, ACTIVITY_ONSAVEINSTANCESTATE, ACTIVITY_ONRESTOREINSTANCESTATE,
			ACTIVITY_ONCREATEDESCRIPTION, ACTIVITY_ONPOSTCREATE2, ACTIVITY_ONPOSTCREATE, ACTIVITY_ONPOSTRESUME,
			ACTIVITY_ONATTACHFRAGMENT, ACTIVITY_ONGENERICMOTIONEVENT, ACTIVITY_ONENTERANIMATIONCOMPLETE,
			ACTIVITY_ONGETDIRECTACTIONS, ACTIVITY_ONCONTEXTITEMSELECTED, ACTIVITY_ONSEARCHREQUESTED,
			ACTIVITY_ONSEARCHREQUESTED2, ACTIVITY_ONKEYSHORTCUT, ACTIVITY_ONKEYDOWN, ACTIVITY_ONKEYMULTIPLE,
			ACTIVITY_ONCHILDTITLECHANGED, ACTIVITY_ONWINDOWSTARTINGACTIONMODE, ACTIVITY_ONWINDOWSTARTINGACTIONMODE2,
			ACTIVITY_OVERRIDEPENDINGTRANSITION, ACTIVITY_OVERRIDEPENDINGTRANSITION2,
			ACTIVITY_ONPICTUREINPICTUREMODECHANGED, ACTIVITY_ONPICTUREINPICTUREMODECHANGED2, ACTIVITY_ONTOUCHEVENT,
			ACTIVITY_ONACTIONMODESTARTED, ACTIVITY_ONNAVIGATEUP, ACTIVITY_ONMULTIWINDOWMODECHANGED,
			ACTIVITY_ONMULTIWINDOWMODECHANGED2, ACTIVITY_ONPICTUREINPICTUREREQUESTED, ACTIVITY_ONPREPAREOPTIONSMENU,
			ACTIVITY_ONPREPAREDIALOG, ACTIVITY_ONPREPAREDIALOG2, ACTIVITY_ONPROVIDEASSISTDATA,
			ACTIVITY_ONPREPARENAVIGATEUPTASKSTACK, ACTIVITY_ONOPTIONSMENUCLOSED, ACTIVITY_ONCONTEXTMENUCLOSED,
			ACTIVITY_ONOPTIONSITEMSELECTED, ACTIVITY_ONTITLECHANGED, ACTIVITY_ONUSERLEAVEHINT,
			ACTIVITY_ONPROVIDEKEYBOARDSHORTCUTS, ACTIVITY_ONPROVIDEASSISTCONTENT, ACTIVITY_ONAPPLYTHEMERESOURCE,
			ACTIVITY_ONKEYLONGPRESS, ACTIVITY_ONKEYUP, ACTIVITY_ONCREATEVIEW, ACTIVITY_ONCREATENAVIGATEUPTASKSTACK,
			ACTIVITY_ONPICTUREINPICTUREUISTATECHANGED, ACTIVITY_ONMENUOPENED, ACTIVITY_ONLOCALVOICEINTERACTIONSTARTED,
			ACTIVITY_ONLOCALVOICEINTERACTIONSTOPPED, ACTIVITY_ONTOPRESUMEDACTIVITYCHANGED, ACTIVITY_ONATTACHEDTOWINDOW,
			ACTIVITY_ONWINDOWFOCUSCHANGED, ACTIVITY_ONWINDOWATTRIBUTESCHANGED, ACTIVITY_ONBACKPRESSED,
			ACTIVITY_ONSTATENOTSAVED, ACTIVITY_ONPANELCLOSED, ACTIVITY_ONTRACKBALLEVENT, ACTIVITY_ONCREATETHUMBNAIL,
			ACTIVITY_ONMENUITEMSELECTED, ACTIVITY_ONCREATECONTEXTMENU, COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED,
			COMPONENTCALLBACKS_ONLOWMEMORY, COMPONENTCALLBACKS2_ONTRIMMEMORY };
	private static final List<String> activityMethodList = Arrays.asList(activityMethods);

	private static final String[] serviceMethods = { SERVICE_ONCREATE, SERVICE_ONDESTROY, SERVICE_ONSTART1,
			SERVICE_ONSTART2, SERVICE_ONBIND, SERVICE_ONREBIND, SERVICE_ONTIMEOUT, SERVICE_ONUNBIND,
			COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED, COMPONENTCALLBACKS_ONLOWMEMORY,
			COMPONENTCALLBACKS2_ONTRIMMEMORY };
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
			CONTENTPROVIDER_GETTYPE, CONTENTPROVIDER_INSERT, CONTENTPROVIDER_QUERY, CONTENTPROVIDER_UPDATE,
			CONTENTPROVIDER_INSERT2, CONTENTPROVIDER_UPDATE2, CONTENTPROVIDER_APPLYBATCH, CONTENTPROVIDER_APPLYBATCH2,
			CONTENTPROVIDER_OPENTYPEDASSETFILE, CONTENTPROVIDER_BULKINSERT, CONTENTPROVIDER_QUERY2,
			CONTENTPROVIDER_QUERY3, CONTENTPROVIDER_OPENTYPEDASSETFILE2, CONTENTPROVIDER_CALL, CONTENTPROVIDER_CALL2,
			CONTENTPROVIDER_GETTYPEANONYMOUS, CONTENTPROVIDER_OPENPIPEHELPER, CONTENTPROVIDER_CANONICALIZE,
			CONTENTPROVIDER_UNCANONICALIZE, CONTENTPROVIDER_OPENFILE, CONTENTPROVIDER_OPENFILE2,
			CONTENTPROVIDER_GETSTREAMTYPES, CONTENTPROVIDER_ISTEMPORARY, CONTENTPROVIDER_ATTACHINFO,
			CONTENTPROVIDER_ONCALLINGPACKAGECHANGED, CONTENTPROVIDER_OPENASSETFILE, CONTENTPROVIDER_OPENASSETFILE2,
			CONTENTPROVIDER_REFRESH, CONTENTPROVIDER_DELETE2, CONTENTPROVIDER_DUMP,
			COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED, COMPONENTCALLBACKS2_ONTRIMMEMORY,
			COMPONENTCALLBACKS_ONLOWMEMORY };
	private static final List<String> contentProviderMethodList = Arrays.asList(contentproviderMethods);

	private static final String[] applicationMethods = { APPLICATION_ONCREATE, APPLICATION_ONTERMINATE };
	private static final List<String> applicationMethodList = Arrays.asList(applicationMethods);

	private static final String[] activityLifecycleMethods = { ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTARTED,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSTOPPED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYRESUMED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYPAUSED,
			ACTIVITYLIFECYCLECALLBACK_ONACTIVITYDESTROYED, ACTIVITYLIFECYCLECALLBACK_ONACTIVITYCREATED };
	private static final List<String> activityLifecycleMethodList = Arrays.asList(activityLifecycleMethods);

	private static final String[] componentFactoryLifecycleMethods = { APPCOMPONENTFACTORY_INSTANTIATEACTIVITY,
			APPCOMPONENTFACTORY_INSTANTIATEAPPLICATION, APPCOMPONENTFACTORY_INSTANTIATECLASSLOADER,
			APPCOMPONENTFACTORY_INSTANTIATEPROVIDER, APPCOMPONENTFACTORY_INSTANTIATERECEIVER };
	private static final List<String> componentFactoryMethodList = Arrays.asList(componentFactoryLifecycleMethods);

	private static final String[] componentCallbackMethods = { COMPONENTCALLBACKS_ONCONFIGURATIONCHANGED,
			COMPONENTCALLBACKS_ONLOWMEMORY };
	private static final List<String> componentCallbackMethodList = Arrays.asList(componentCallbackMethods);

	public static final String[] componentCallback2Methods = { COMPONENTCALLBACKS2_ONTRIMMEMORY };
	public static final List<String> componentCallback2MethodList = Arrays.asList(componentCallback2Methods);

	private static final String[] serviceConnectionMethods = { SERVICECONNECTION_ONSERVICECONNECTED,
			SERVICECONNECTION_ONSERVICEDISCONNECTED };
	private static final List<String> serviceConnectionMethodList = Arrays.asList(serviceConnectionMethods);
	public static final String ATTACH_BASE_CONTEXT = "void attachBaseContext(android.content.Context)";
	public static final String CONTEXT_WRAPPER = "android.content.ContextWrapper";
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

	public static List<String> getComponentFactoryCallbackMethods() {
		return componentFactoryMethodList;
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
