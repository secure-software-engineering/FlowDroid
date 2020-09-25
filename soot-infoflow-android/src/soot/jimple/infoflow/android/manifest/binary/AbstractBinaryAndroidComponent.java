package soot.jimple.infoflow.android.manifest.binary;

import java.util.Map.Entry;

import pxb.android.axml.AxmlVisitor;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;
import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Abstract base class for Android components loaded from a binary manifest file
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractBinaryAndroidComponent implements IAndroidComponent {

	protected final AXmlNode node;
	protected final BaseProcessManifest<?, ?, ?, ?> manifest;
	protected boolean enabled;
	protected boolean exported;

	protected AbstractBinaryAndroidComponent(AXmlNode node, BaseProcessManifest<?, ?, ?, ?> manifest) {
		this.node = node;
		this.manifest = manifest;

		AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
		enabled = attrEnabled == null || !attrEnabled.getValue().equals(Boolean.FALSE);

		AXmlAttribute<?> attrExported = node.getAttribute("exported");
		exported = attrExported == null || !attrExported.getValue().equals(Boolean.FALSE);
	}


	public AXmlNode getAXmlNode() {
		return node;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isExported() {
		return exported;
	}

	@Override
	public String getNameString() {
		AXmlAttribute<?> attr = node.getAttribute("name");
		if (attr != null)
			return manifest.expandClassName((String) attr.getValue());
		else {
			// This component does not have a name, so this might be
			// obfuscated malware. We apply a heuristic.
			for (Entry<String, AXmlAttribute<?>> a : node.getAttributes().entrySet()) {
				AXmlAttribute<?> attrValue = a.getValue();
				if (attrValue != null) {
					String attrValueName = attrValue.getName();
					if ((attrValueName == null || attrValueName.isEmpty())
							&& attrValue.getType() == AxmlVisitor.TYPE_STRING) {
						String name = (String) attrValue.getValue();
						if (isValidComponentName(name)) {
							String expandedName = manifest.expandClassName(name);
							if (!SystemClassHandler.v().isClassInSystemPackage(expandedName))
								return expandedName;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks if the specified name is a valid Android component name
	 *
	 * @param name The Android component name to check
	 * @return True if the given name is a valid Android component name, otherwise
	 *         false
	 */
	private boolean isValidComponentName(String name) {
		if (name == null || name.isEmpty())
			return false;
		if (name.equals("true") || name.equals("false"))
			return false;
		if (Character.isDigit(name.charAt(0)))
			return false;

		if (name.startsWith("."))
			return true;

		// Be conservative
		return false;
	}

}
