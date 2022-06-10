package soot.jimple.infoflow.results.xml;

/**
 * Information about a flow sink loaded from an external data storage. This
 * object thus cannot reference actual Soot objects
 * 
 * @author Steven Arzt
 *
 */
public class SerializedSinkInfo extends AbstractSerializedSourceSink {

	private final String methodSourceSinkDefinition;

	SerializedSinkInfo(SerializedAccessPath accessPath, String statement,
			String method, String methodSourceSinkDefinition) {
		super(accessPath, statement, method);
		this.methodSourceSinkDefinition = methodSourceSinkDefinition;
	}

	/**
	 * Gets the method signature of the method of the statement.
	 * 
	 * @return The soot signature of the method that is called by the statement.
	 */
	public String getMethodSourceSinkDefinition() {
		return this.methodSourceSinkDefinition;
	}
}
