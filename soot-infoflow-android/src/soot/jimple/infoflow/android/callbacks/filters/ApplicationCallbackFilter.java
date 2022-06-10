package soot.jimple.infoflow.android.callbacks.filters;

import java.util.Set;

import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;

/**
 * A callback filter that restricts application callbacks to ComponentCallbacks
 * and ActivityLifecycleCallsbacks.
 * 
 * @author Steven Arzt
 *
 */
public class ApplicationCallbackFilter extends AbstractCallbackFilter {

	private final String applicationClass;

	private RefType activityLifecycleCallbacks;
	private RefType provideAssistDataListener;
	private RefType componentCallbacks;
	private RefType componentCallbacks2;

	/**
	 * Creates a new instance of the {@link ApplicationCallbackFilter} class
	 * 
	 * @param entrypoints The set of entry points into the app
	 */
	public ApplicationCallbackFilter(Set<SootClass> entrypoints) {
		this(getApplicationClass(entrypoints));
	}

	/**
	 * Scans through the list of entry points and finds the application class
	 * 
	 * @param entrypoints A set containing all entry points in the current app
	 * @return The name of the application class if one exists, otherwise null
	 */
	private static String getApplicationClass(Set<SootClass> entrypoints) {
		SootClass scApplication = Scene.v().getSootClassUnsafe("android.app.Application");
		for (SootClass sc : entrypoints) {
			if (sc != null && scApplication != null
					&& Scene.v().getOrMakeFastHierarchy().canStoreType(sc.getType(), scApplication.getType())) {
				return sc.getName();
			}
		}
		return null;
	}

	/**
	 * Creates a new instance of the {@link ApplicationCallbackFilter} class
	 * 
	 * @param applicationClass The class extending android.app.Application
	 */
	public ApplicationCallbackFilter(String applicationClass) {
		super();
		this.applicationClass = applicationClass;
		reset();
	}

	@Override
	public boolean accepts(SootClass component, SootClass callbackHandler) {
		// Special handling for callbacks registered for the application, but
		// not implemented there
		if (this.applicationClass != null && component.getName().equals(this.applicationClass)
				&& !callbackHandler.getName().equals(applicationClass)) {
			final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
			final RefType callbackType = callbackHandler.getType();
			if (!fh.canStoreType(callbackType, this.activityLifecycleCallbacks)
					&& !fh.canStoreType(callbackType, this.provideAssistDataListener)
					&& !fh.canStoreType(callbackType, this.componentCallbacks))
				return false;
		}

		return true;
	}

	@Override
	public void reset() {
		this.activityLifecycleCallbacks = RefType.v(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACKSINTERFACE);
		this.provideAssistDataListener = RefType.v("android.app.Application$OnProvideAssistDataListener");
		this.componentCallbacks = RefType.v(AndroidEntryPointConstants.COMPONENTCALLBACKSINTERFACE);
		this.componentCallbacks2 = RefType.v(AndroidEntryPointConstants.COMPONENTCALLBACKS2INTERFACE);
	}

	@Override
	public boolean accepts(SootClass component, SootMethod callback) {
		// We do not accept ActivityLifecycleCallbacks and ComponentCallbacks in
		// components that are not the application
		if (component.getName().equals(applicationClass))
			return true;

		String subSig = callback.getSubSignature();
		final FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		final RefType callbackType = callback.getDeclaringClass().getType();
		if (AndroidEntryPointConstants.getActivityLifecycleCallbackMethods().contains(subSig))
			return fh.canStoreType(callbackType, this.activityLifecycleCallbacks);
		if (AndroidEntryPointConstants.getComponentCallbackMethods().contains(subSig))
			return fh.canStoreType(callbackType, this.componentCallbacks);
		if (AndroidEntryPointConstants.getComponentCallback2Methods().contains(subSig))
			return fh.canStoreType(callbackType, this.componentCallbacks2);

		return true;
	}

}
