package soot.jimple.infoflow.methodSummary.data.summary;

/**
 * Enumeration containing the types of sources and sinks for which summaries can
 * be generated.
 * 
 * @author Steven Arzt
 */
public enum SourceSinkType {
	/**
	 * The flow starts or ends at a field of the current base object
	 */
	Field,
	
	/**
	 * The flow starts or ends at a field of a parameter of the current method
	 */
	Parameter,
	
	/**
	 * The flow starts or ends at the return value of the current method
	 */
	Return,
	
	/**
	 * The base object of a call to a gap method
	 */
	GapBaseObject,
	
	/**
	 * A custom type of source or sink. Such elements will be ignored by the
	 * default implementation
	 */
	Custom
}
