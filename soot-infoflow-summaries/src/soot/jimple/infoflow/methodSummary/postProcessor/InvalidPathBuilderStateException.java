package soot.jimple.infoflow.methodSummary.postProcessor;

/**
 * Exception that is thrown when the path reconstruction algorithm has
 * encountered an invalid state
 * 
 * @author Steven Arzt
 *
 */
public class InvalidPathBuilderStateException extends RuntimeException {

	private static final long serialVersionUID = -5269997769793552669L;

	public InvalidPathBuilderStateException() {
		super();
	}

	public InvalidPathBuilderStateException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidPathBuilderStateException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidPathBuilderStateException(String message) {
		super(message);
	}

	public InvalidPathBuilderStateException(Throwable cause) {
		super(cause);
	}

}
