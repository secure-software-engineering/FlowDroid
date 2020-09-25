package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;
import soot.jimple.infoflow.android.manifest.IContentProvider;

/**
 * A content provider loaded from a binary Android manifest
 * 
 * @author Steven Arzt
 *
 */
public class BinaryManifestContentProvider extends AbstractBinaryAndroidComponent implements IContentProvider {

	public BinaryManifestContentProvider(AXmlNode node, BaseProcessManifest<?, ?, ?, ?> manifest) {
		super(node, manifest);
	}

}
