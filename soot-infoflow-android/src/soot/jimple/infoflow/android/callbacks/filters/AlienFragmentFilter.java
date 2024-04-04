package soot.jimple.infoflow.android.callbacks.filters;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.util.MultiMap;

/**
 * Filter that discards callbacks that belong to a fragment that, in turn, does
 * not belong to the current component.
 * 
 * @author Steven Arzt
 *
 */
public class AlienFragmentFilter extends AbstractCallbackFilter {

	private SootClass fragmentClass;
	private final MultiMap<SootClass, SootClass> fragmentToActivity;

	/**
	 * Creates a new instance of the {@link AlienFragmentFilter} class
	 * 
	 * @param fragmentToActivity mapping from fragments to the activities containing
	 *                           them
	 */
	public AlienFragmentFilter(MultiMap<SootClass, SootClass> fragmentToActivity) {
		this.fragmentToActivity = fragmentToActivity;
	}

	@Override
	public boolean accepts(SootClass component, SootClass callbackHandler) {
		if (fragmentClass == null)
			// No filtering possible
			return true;
		if (Scene.v().getOrMakeFastHierarchy().canStoreType(callbackHandler.getType(), this.fragmentClass.getType()))
			if (!fragmentToActivity.get(callbackHandler).contains(component))
				return false;
		return true;
	}

	@Override
	public boolean accepts(SootClass component, SootMethod callback) {
		return true;
	}

	@Override
	public void reset() {
		this.fragmentClass = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.FRAGMENTCLASS);
	}

}
