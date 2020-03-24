package soot.jimple.infoflow.methodSummary.data.sourceSink;

/**
 * Exception that is thrown when a source or sink specification inside a summary
 * flow is invalid
 * 
 * @author Steven Arzt
 *
 */
public class InvalidFlowSpecificationException extends RuntimeException {

	private static final long serialVersionUID = -6548286243034212560L;

	private final AbstractFlowSinkSource sinkSource;

	public InvalidFlowSpecificationException(String message, AbstractFlowSinkSource sinkSource) {
		super(message);
		this.sinkSource = sinkSource;
	}

	public AbstractFlowSinkSource getSinkSource() {
		return sinkSource;
	}

}
