package soot.jimple.infoflow.android.test.manifest;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

public class ProcessManifestTest {
	@Test
	public void testGetVersionCode() {
		ProcessManifest manifest = null;

		try {
			manifest = new ProcessManifest("testAPKs/enriched1.apk");
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}

		boolean throwsException = false;

		try {
			manifest.getVersionCode();
			manifest.getMinSdkVersion();
			manifest.getTargetSdkVersion();
		} catch (Exception ex) {
			throwsException = true;
		}

		org.junit.Assert.assertFalse(throwsException);
	}

	@Test
	public void testSdkVersion() {
		ProcessManifest manifest = null;

		try {
			manifest = new ProcessManifest("testAPKs/enriched1.apk");
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}

		boolean throwsException = false;

		try {
			manifest.getMinSdkVersion();
			manifest.getTargetSdkVersion();
		} catch (Exception ex) {
			throwsException = true;
		}

		org.junit.Assert.assertFalse(throwsException);
	}

	@Test
	public void testGetAliasActivity() {
		ProcessManifest manifest = null;
		try {
			manifest = new ProcessManifest("testAPKs/FlowDroidAliasActivity.apk");
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}

		boolean throwsException = false;

		try {
			List<AXmlNode> aliasActivities = manifest.getAliasActivities();
			org.junit.Assert.assertFalse(aliasActivities.isEmpty()); // test that there is an Alias activity
			AXmlNode aliasActivity = aliasActivities.get(0);
			org.junit.Assert.assertTrue(ProcessManifest.isAliasActivity(aliasActivity));
			AXmlNode target = manifest.getAliasActivityTarget(aliasActivities.get(0));
			org.junit.Assert.assertNotNull(target);

			AXmlAttribute<?> attr = aliasActivities.get(0).getAttribute("name");
			AXmlNode aliasActivityBis = manifest.getAliasActivity((String) attr.getValue());
			org.junit.Assert.assertNotNull(aliasActivityBis);
		} catch (Exception ex) {
			throwsException = true;
		}
		org.junit.Assert.assertFalse(throwsException);
	}
}
