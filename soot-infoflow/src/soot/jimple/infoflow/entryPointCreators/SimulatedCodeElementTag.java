package soot.jimple.infoflow.entryPointCreators;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Tag to denote that a certain method or class was created by an entry point
 * creator and should not be considered real app code
 * 
 * @author Steven Arzt
 *
 */
public class SimulatedCodeElementTag implements Tag {

	public static final String TAG_NAME = "SimulatedCodeElementTag";
	public static SimulatedCodeElementTag TAG = new SimulatedCodeElementTag();

	private SimulatedCodeElementTag() {
		//
	}

	@Override
	public String getName() {
		return TAG_NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}

}
