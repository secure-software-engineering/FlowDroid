package soot.jimple.infoflow.android.resources.controls;

import java.util.Map;
import java.util.Map.Entry;

import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;

/**
 * Factory class for creating Android layout controls
 * 
 * @author Steven Arzt
 *
 */
public class LayoutControlFactory {

	private boolean loadAdditionalAttributes = false;
	private SootClass scEditText = null;

	public AndroidLayoutControl createLayoutControl(String layoutFile, SootClass layoutClass, AXmlNode node) {
		// Initialize the required classes
		if (scEditText == null)
			scEditText = Scene.v().getSootClassUnsafe("android.widget.EditText");

		// Create the layout control
		AndroidLayoutControl lc = createLayoutControl(layoutClass);

		applyAttributes(node, lc);
		return lc;
	}

	protected void applyAttributes(AXmlNode node, AndroidLayoutControl lc) {
		Map<String, AXmlAttribute<?>> attributes = node.getAttributes();
		for (Entry<String, AXmlAttribute<?>> entry : attributes.entrySet()) {
			if (entry.getKey() == null)
				continue;

			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();

			// On obfuscated Android malware, the attribute name may be empty
			if (attrName.isEmpty())
				continue;

			// Check that we're actually working on an android attribute
			if (!isAndroidNamespace(attr.getNamespace()))
				continue;

			// Let the layout control handle its custom attributes
			lc.handleAttribute(attr, loadAdditionalAttributes);
		}
	}

	/**
	 * Creates an empty layout control that corresponds to the given class
	 * @param layoutClass
	 *            The layout class in Android that implements the control
	 * @return The newly created layout control
	 */
	protected AndroidLayoutControl createLayoutControl(SootClass layoutClass) {
		if (scEditText != null
				&& Scene.v().getFastHierarchy().canStoreType(layoutClass.getType(), scEditText.getType()))
			return new EditTextControl(layoutClass);
		else
			return new GenericLayoutControl(layoutClass);
	}

	/**
	 * Sets whether the parser should load all additional attributes as well. If
	 * this option is disabled, it only loads those well-known attributes that
	 * influence the data flow analysis.
	 * 
	 * @param loadAdditionalAttributes
	 *            True to load all attributes present in the layout XML files,
	 *            false to only load the ones required for FlowDroid.
	 */
	public void setLoadAdditionalAttributes(boolean loadAdditionalAttributes) {
		this.loadAdditionalAttributes = loadAdditionalAttributes;
	}

	/**
	 * Checks whether the given namespace belongs to the Android operating
	 * system
	 * 
	 * @param ns
	 *            The namespace to check
	 * @return True if the namespace belongs to Android, otherwise false
	 */
	protected boolean isAndroidNamespace(String ns) {
		if (ns == null)
			return false;
		ns = ns.trim();
		if (ns.startsWith("*"))
			ns = ns.substring(1);
		if (!ns.equals("http://schemas.android.com/apk/res/android"))
			return false;
		return true;
	}

}
