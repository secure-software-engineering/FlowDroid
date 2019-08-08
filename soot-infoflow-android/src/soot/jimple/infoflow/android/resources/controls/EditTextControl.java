package soot.jimple.infoflow.android.resources.controls;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import pxb.android.axml.AxmlVisitor;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.flags.InputType;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * EditText control in Android
 * 
 * @author Steven Arzt
 *
 */
public class EditTextControl extends AndroidLayoutControl {
	protected final static ISourceSinkDefinition UI_PASSWORD_SOURCE_DEF;
	protected final static ISourceSinkDefinition UI_ELEMENT_SOURCE_DEF;

	static {
		UI_PASSWORD_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
				Collections.singleton(AccessPathTuple.fromPathElements("android.widget.TextView",
						Collections.singletonList("content"), Collections.singletonList("android.text.Editable"),
						SourceSinkType.Source)),
				CallType.MethodCall);
		UI_ELEMENT_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
				Collections.singleton(AccessPathTuple.fromPathElements("android.widget.TextView",
						Collections.singletonList("content"), Collections.singletonList("android.text.Editable"),
						SourceSinkType.Source)),
				CallType.MethodCall);

		UI_PASSWORD_SOURCE_DEF.setCategory(new ISourceSinkCategory() {

			@Override
			public String getHumanReadableDescription() {
				return "Password Input";
			}

			@Override
			public String toString() {
				return "Password Input";
			}

			@Override
			public String getID() {
				return "PASSWORD";
			}

		});
		UI_ELEMENT_SOURCE_DEF.setCategory(new ISourceSinkCategory() {

			@Override
			public String getHumanReadableDescription() {
				return "UI Element";
			}

			@Override
			public String toString() {
				return "UI Element";
			}

			@Override
			public String getID() {
				return "UIELEMENT";
			}

		});
	}

	private int inputType;
	private boolean isPassword;
	private String text;
	private Collection<Integer> inputTypeFlags;

	EditTextControl(SootClass viewClass) {
		super(viewClass);
	}

	public EditTextControl(int id, SootClass viewClass) {
		super(id, viewClass);
	}

	public EditTextControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
		super(id, viewClass, additionalAttributes);
	}

	/**
	 * Sets the type of this input (text, password, etc.)
	 * 
	 * @param inputType The input type
	 */
	void setInputType(int inputType) {
		this.inputType = inputType;
		this.inputTypeFlags = InputType.getFlags(inputType);

	}

	/**
	 * Gets the type of this input (text, password, etc.)
	 * 
	 * @return The input type
	 */
	public int getInputType() {
		return inputType;
	}

	/**
	 * Returns true if the input satiesfies all specified types
	 * 
	 * @see InputType
	 * @param type the types to check
	 */
	public boolean satisfiesInputType(int... type) {
		if (inputTypeFlags == null)
			return false;

		for (int i : type)
			if (!inputTypeFlags.contains(i))
				return false;
		return true;
	}

	/**
	 * Gets the text of this edit control
	 * 
	 * @return The text of this edit control
	 */
	public String getText() {
		return text;
	}

	@Override
	protected void handleAttribute(AXmlAttribute<?> attribute, boolean loadOptionalData) {
		final String attrName = attribute.getName().trim();
		final int type = attribute.getType();

		if (attrName.equals("inputType") && type == AxmlVisitor.TYPE_INT_HEX) {
			setInputType((Integer) attribute.getValue());
		} else if (attrName.equals("password")) {
			if (type == AxmlVisitor.TYPE_INT_HEX)
				isPassword = ((Integer) attribute.getValue()) != 0; // -1 for
			// true, 0
			// for false
			else if (type == AxmlVisitor.TYPE_INT_BOOLEAN)
				isPassword = (Boolean) attribute.getValue();
			else
				throw new RuntimeException("Unknown representation of boolean data type");
		} else if (loadOptionalData && type == AxmlVisitor.TYPE_STRING && attrName.equals("text")) {
			text = (String) attribute.getValue();
		} else
			super.handleAttribute(attribute, loadOptionalData);
	}

	@Override
	public boolean isSensitive() {
		if (isPassword)
			return true;

		if (satisfiesInputType(InputType.numberPassword))
			return true;
		if (satisfiesInputType(InputType.textVisiblePassword))
			return true;
		if (satisfiesInputType(InputType.textWebPassword))
			return true;
		if (satisfiesInputType(InputType.textPassword))
			return true;

		return false;
	}

	@Override
	public ISourceSinkDefinition getSourceDefinition() {
		return isSensitive() ? UI_PASSWORD_SOURCE_DEF : UI_ELEMENT_SOURCE_DEF;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + inputType;
		result = prime * result + (isPassword ? 1231 : 1237);
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		EditTextControl other = (EditTextControl) obj;
		if (inputType != other.inputType)
			return false;
		if (isPassword != other.isPassword)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

}
