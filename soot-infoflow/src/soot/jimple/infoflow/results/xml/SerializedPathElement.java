package soot.jimple.infoflow.results.xml;

/**
 * Class representing an element on the taint propagation path of a data flow
 * result
 * 
 * @author Steven Arzt
 *
 */
public class SerializedPathElement extends AbstractSerializedSourceSink {

	SerializedPathElement(SerializedAccessPath ap, String statement,
			String method) {
		super(ap, statement, method);
	}

}
