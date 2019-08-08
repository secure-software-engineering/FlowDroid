package soot.jimple.infoflow.android.callbacks.filters;

import java.util.Set;

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

	private SootClass activityLifecycleCallbacks;
	private SootClass provideAssistDataListener;
	private SootClass componentCallbacks;

	/**
	 * Creates a new instance of the {@link ApplicationCallbackFilter} class
	 * 
	 * @param entrypoints
	 *            The set of entry points into the app
	 */
	public ApplicationCallbackFilter(Set<SootClass> entrypoints) {
		this(getApplicationClass(entrypoints));
	}

	/**
	 * Scans through the list of entry points and finds the application class
	 * 
	 * @param entrypoints
	 *            A set containing all entry points in the current app
	 * @return The name of the application class if one exists, otherwise null
	 */
	private static String getApplicationClass(Set<SootClass> entrypoints) {
		SootClass scApplication = Scene.v().getSootClassUnsafe("android.app.Application");
		for (SootClass sc : entrypoints) {
			if (sc != null && Scene.v().getOrMakeFastHierarchy().canStoreType(sc.getType(), scApplication.getType())) {
				return sc.getName();
			}
		}
		return null;
	}

	/**
	 * Creates a new instance of the {@link ApplicationCallbackFilter} class
	 * 
	 * @param applicationClass
	 *            The class extending android.app.Application
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
			if (!Scene.v().getOrMakeFastHierarchy().canStoreType(callbackHandler.getType(),
					this.activityLifecycleCallbacks.getType())
					&& !Scene.v().getOrMakeFastHierarchy().canStoreType(callbackHandler.getType(),
							this.provideAssistDataListener.getType())
					&& !Scene.v().getOrMakeFastHierarchy().canStoreType(callbackHandler.getType(),
							this.componentCallbacks.getType()))
				return false;
		}

		return true;
	}

	@Override
	public void reset() {
		this.activityLifecycleCallbacks = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACKSINTERFACE);
		this.provideAssistDataListener = Scene.v()
				.getSootClassUnsafe("android.app.Application$OnProvideAssistDataListener");
		this.componentCallbacks = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.COMPONENTCALLBACKSINTERFACE);
	}

	@Override
	public boolean accepts(SootClass component, SootMethod callback) {
		// We do not accept ActivityLifecycleCallbacks and ComponentCallbacks in
		// components that are not the application
		if (component.getName().equals(applicationClass))
			return true;
		String subSig = callback.getSubSignature();
		return !AndroidEntryPointConstants.getActivityLifecycleCallbackMethods().contains(subSig)
				&& !AndroidEntryPointConstants.getComponentCallbackMethods().contains(subSig)
				&& !AndroidEntryPointConstants.getComponentCallback2Methods().contains(subSig);
	}

}
