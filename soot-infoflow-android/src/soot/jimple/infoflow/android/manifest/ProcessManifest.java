package soot.jimple.infoflow.android.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestActivity;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestBroadcastReceiver;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestContentProvider;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestService;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

/**
 * This class provides easy access to all data of an AppManifest.<br />
 * Nodes and attributes of a parsed manifest can be changed. A new byte
 * compressed manifest considering the changes can be generated.
 *
 * @author Steven Arzt
 * @author Stefan Haas, Mario Schlipf
 * @see <a href=
 *      "http://developer.android.com/guide/topics/manifest/manifest-intro.html">App
 *      Manifest</a>
 */
public class ProcessManifest extends
		BaseProcessManifest<BinaryManifestActivity, BinaryManifestService, BinaryManifestContentProvider, BinaryManifestBroadcastReceiver> {

	public ProcessManifest(File apkFile, ARSCFileParser arscParser) throws IOException, XmlPullParserException {
		super(apkFile, arscParser);
	}

	public ProcessManifest(File apkFile) throws IOException, XmlPullParserException {
		super(apkFile);
	}

	public ProcessManifest(InputStream manifestIS, ARSCFileParser arscParser)
			throws IOException, XmlPullParserException {
		super(manifestIS, arscParser);
	}

	public ProcessManifest(String apkPath) throws IOException, XmlPullParserException {
		super(apkPath);
	}

	@Override
	protected IComponentFactory<BinaryManifestActivity, BinaryManifestService, BinaryManifestContentProvider, BinaryManifestBroadcastReceiver> createComponentFactory() {
		return new IComponentFactory<BinaryManifestActivity, BinaryManifestService, BinaryManifestContentProvider, BinaryManifestBroadcastReceiver>() {

			@Override
			public BinaryManifestActivity createActivity(AXmlNode node) {
				return new BinaryManifestActivity(node, ProcessManifest.this);
			}

			@Override
			public BinaryManifestBroadcastReceiver createBroadcastReceiver(AXmlNode node) {
				return new BinaryManifestBroadcastReceiver(node, ProcessManifest.this);
			}

			@Override
			public BinaryManifestContentProvider createContentProvider(AXmlNode node) {
				return new BinaryManifestContentProvider(node, ProcessManifest.this);
			}

			@Override
			public BinaryManifestService createService(AXmlNode node) {
				return new BinaryManifestService(node, ProcessManifest.this);
			}

		};
	}

}
