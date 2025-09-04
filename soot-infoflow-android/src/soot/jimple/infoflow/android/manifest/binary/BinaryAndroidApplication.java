package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

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
	private String appComponentFactory;

	public BinaryAndroidApplication(AXmlNode node, BaseProcessManifest<?, ?, ?, ?> manifest) {
		this.node = node;
		this.manifest = manifest;
		ARSCFileParser arscParser = manifest.getArscParser();

		AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
		enabled = attrEnabled == null || attrEnabled.asBoolean(arscParser);

		AXmlAttribute<?> attrDebuggable = node.getAttribute("debuggable");
		debuggable = attrDebuggable != null && attrDebuggable.asBoolean(arscParser);

		AXmlAttribute<?> attrAllowBackup = node.getAttribute("allowBackup");
		allowBackup = attrAllowBackup == null || attrAllowBackup.asBoolean(arscParser);

		AXmlAttribute<?> attrCleartextTraffic = node.getAttribute("usesCleartextTraffic");
		if (attrCleartextTraffic != null)
			usesCleartextTraffic = attrCleartextTraffic.asBoolean(arscParser);

		this.name = expandClassAttribute("name");
		this.appComponentFactory = expandClassAttribute("appComponentFactory");
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns the value of an attribute describing a class
	 * @param attrName the attribute name
	 * @return the value (or null if not set)
	 */
	private String expandClassAttribute(String attrName) {
		String n = expandAttribute(attrName);
		if (n != null) {
			if (n.startsWith(".")) {
				n = manifest.getPackageName() + n;
			}
		}
		return n;
	}

	/**
	 * Returns the value of an attribute while resolving resources, e.g. if a
	 * string resource is referenced
	 * @param attrName the attribute name
	 * @return the value (or null if not set)
	 */
	private String expandAttribute(String attrName) {
		AXmlAttribute<?> attr = node.getAttribute(attrName);
		if (attr != null)
			return attr.asString(manifest.getArscParser());
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

	@Override
	public String getAppComponentFactory() {
		return appComponentFactory;
	}

}
