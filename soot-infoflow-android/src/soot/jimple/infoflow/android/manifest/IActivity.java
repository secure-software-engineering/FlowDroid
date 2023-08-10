package soot.jimple.infoflow.android.manifest;

import java.util.List;

import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;

/**
 * An activity inside an Android app
 * 
 * @author Steven Arzt
 *
 */
public interface IActivity extends IAndroidComponent {

	@Override
	default public List<String> getLifecycleMethods() {
		return AndroidEntryPointConstants.getActivityLifecycleMethods();
	}
}
