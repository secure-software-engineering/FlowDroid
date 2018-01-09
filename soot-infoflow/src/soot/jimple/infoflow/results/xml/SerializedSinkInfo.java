package soot.jimple.infoflow.results.xml;

/**
 * Information about a flow sink loaded from an external data storage. This
 * object thus cannot reference actual Soot objects
 * 
 * @author Steven Arzt
 *
 */
public class SerializedSinkInfo extends AbstractSerializedSourceSink {

	SerializedSinkInfo(SerializedAccessPath accessPath, String statement,
			String method) {
		super(accessPath, statement, method);
	}

	
}
