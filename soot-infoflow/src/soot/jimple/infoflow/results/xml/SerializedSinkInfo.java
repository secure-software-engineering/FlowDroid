package soot.jimple.infoflow.results.xml;

/**
 * Information about a flow sink loaded from an external data storage. This
 * object thus cannot reference actual Soot objects
 * 
 * @author Steven Arzt
 *
 */
public class SerializedSinkInfo extends AbstractSerializedSourceSink {

	private final String calledMethod;

	SerializedSinkInfo(SerializedAccessPath accessPath, String statement,
			String method, String calledMethod) {
		super(accessPath, statement, method);
		this.calledMethod = calledMethod;
	}

	/**
	 * Gets the method signature of the method of the statement.
	 * 
	 * @return The soot signature of the method that is called by the statement.
	 */
	public String getCalledMethod() {
		return this.calledMethod;
	}
}
