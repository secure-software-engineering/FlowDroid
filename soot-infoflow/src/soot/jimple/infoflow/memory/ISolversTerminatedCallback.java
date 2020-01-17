package soot.jimple.infoflow.memory;

/**
 * Callback to notify the outside world that the solvers have been terminated
 * 
 * @author Steven Arzt
 *
 */
public interface ISolversTerminatedCallback {

	/**
	 * This method is invoked when all solvers have been terminated due to a timeout
	 */
	public void onSolversTerminated();

}
