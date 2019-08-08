package soot.jimple.infoflow.android.source;

/**
 * Exception that is thrown when the data flow analysis is instructed to load
 * its sources and sinks from a file of an unsupported format
 * 
 * @author Steven Arzt
 *
 */
public class UnsupportedSourceSinkFormatException extends RuntimeException {

	private static final long serialVersionUID = -8452194172838311666L;

	public UnsupportedSourceSinkFormatException() {
		super();
	}

	public UnsupportedSourceSinkFormatException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnsupportedSourceSinkFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedSourceSinkFormatException(String message) {
		super(message);
	}

	public UnsupportedSourceSinkFormatException(Throwable cause) {
		super(cause);
	}

}
