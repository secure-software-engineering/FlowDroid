package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;

/**
 * An Android application loaded from a binary manifest file
 * 
 * @author Steven Arzt
 *
 */
public class BinaryAndroidApplication implements IAndroidApplication {

	protected final AXmlNode node;
	protected final ProcessManifest manifest;

	protected boolean enabled;
	protected String name;

	public BinaryAndroidApplication(AXmlNode node, ProcessManifest manifest) {
		this.node = node;
		this.manifest = manifest;

		AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
		enabled = attrEnabled == null || !attrEnabled.getValue().equals(Boolean.FALSE);

		this.name = loadApplicationName();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Gets the name of the Android application class
	 *
	 * @return The name of the Android application class
	 */
	private String loadApplicationName() {
		AXmlAttribute<?> attr = node.getAttribute("name");
		if (attr != null) {
			Object value = attr.getValue();
			if (value != null) {
				if (value instanceof String)
					return manifest.expandClassName((String) attr.getValue());
				else if (value instanceof Integer) {
					AbstractResource res = manifest.getArscParser().findResource((Integer) attr.getValue());
					if (res instanceof StringResource) {
						StringResource strRes = (StringResource) res;
						return strRes.getValue();
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	public AXmlNode getAXmlNode() {
		return node;
	}

}
