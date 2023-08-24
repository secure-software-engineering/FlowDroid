package soot.jimple.infoflow.android.manifest;

import java.util.List;

import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;

/**
 * Interface for services inside an Android app
 * 
 * @author Steven Arzt
 *
 */
public interface IService extends IAndroidComponent {

	@Override
	default public List<String> getLifecycleMethods() {
		return AndroidEntryPointConstants.getServiceLifecycleMethods();
	}
}
