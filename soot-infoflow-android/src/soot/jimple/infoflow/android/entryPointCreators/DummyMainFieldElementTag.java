package soot.jimple.infoflow.android.entryPointCreators;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Tag to denote that a certain method or class was created by an entry point
 * creator and should not be considered real app code
 * 
 * @author Steven Arzt
 *
 */
public class DummyMainFieldElementTag implements Tag {

	public static final String TAG_NAME = "DummyMainFieldElementTag";
	public static DummyMainFieldElementTag TAG = new DummyMainFieldElementTag();
	private String fieldName;

	private DummyMainFieldElementTag() {
		//
	}

	public DummyMainFieldElementTag(String name) {
		this.fieldName = name;
	}

	public String getFieldName() {
		return fieldName;
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
