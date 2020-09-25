package soot.jimple.infoflow.android.manifest.binary;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;
import soot.jimple.infoflow.android.manifest.IService;

/**
 * A service loaded from a binary Android manifest
 * 
 * @author Steven Arzt
 *
 */
public class BinaryManifestService extends AbstractBinaryAndroidComponent implements IService {

	/**
	 * Creates a new instance of the {@link BinaryManifestService} class
	 * 
	 * @param node     The binary XML node
	 * @param manifest The manifest
	 */
	public BinaryManifestService(AXmlNode node, BaseProcessManifest<?, ?, ?, ?> manifest) {
		super(node, manifest);
	}

}
