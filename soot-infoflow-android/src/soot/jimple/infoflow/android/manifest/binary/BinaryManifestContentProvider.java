package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.IContentProvider;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

/**
 * A content provider loaded from a binary Android manifest
 * 
 * @author Steven Arzt
 *
 */
public class BinaryManifestContentProvider extends AbstractBinaryAndroidComponent implements IContentProvider {

	public BinaryManifestContentProvider(AXmlNode node, ProcessManifest manifest) {
		super(node, manifest);
	}

}
