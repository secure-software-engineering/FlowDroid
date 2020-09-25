package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
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
	protected final BaseProcessManifest<?, ?, ?, ?> manifest;

	protected boolean enabled;
	protected boolean debuggable;
	protected boolean allowBackup;
	protected String name;
	protected Boolean usesCleartextTraffic;

	public BinaryAndroidApplication(AXmlNode node, BaseProcessManifest<?, ?, ?, ?> manifest) {
		this.node = node;
		this.manifest = manifest;

		AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
		enabled = attrEnabled == null || !attrEnabled.getValue().equals(Boolean.FALSE);

		AXmlAttribute<?> attrDebuggable = node.getAttribute("debuggable");
		debuggable = attrDebuggable != null && attrDebuggable.getValue().equals(Boolean.TRUE);

		AXmlAttribute<?> attrAllowBackup = node.getAttribute("allowBackup");
		allowBackup = attrAllowBackup != null && attrAllowBackup.getValue().equals(Boolean.TRUE);

		AXmlAttribute<?> attrCleartextTraffic = node.getAttribute("usesCleartextTraffic");
		if (attrCleartextTraffic != null)
			usesCleartextTraffic = attrCleartextTraffic.getValue().equals(Boolean.TRUE);

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

	@Override
	public boolean isDebuggable() {
		return debuggable;
	}

	@Override
	public boolean isAllowBackup() {
		return allowBackup;
	}

	@Override
	public Boolean isUsesCleartextTraffic() {
		return usesCleartextTraffic;
	}

}
