package soot.jimple.infoflow.entryPointCreators;

import soot.tagkit.Tag;

/**
 * Tag to denote that a certain statement is simulating the effects of a dynamic invoke
 *
 */
public class SimulatedDynamicInvokeTag implements Tag {

	public static final String TAG_NAME = "SimulatedDynamicInvokeTag";
	public static SimulatedDynamicInvokeTag TAG = new SimulatedDynamicInvokeTag();

	private SimulatedDynamicInvokeTag() {
		//
	}

	@Override
	public String getName() {
		return TAG_NAME;
	}

}
