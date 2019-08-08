package soot.jimple.infoflow.nativeCallHandler;

import soot.jimple.infoflow.InfoflowManager;

/**
 * Abstract base class for all native call handlers
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractNativeCallHandler implements INativeCallHandler {

	/**
	 * Data flow manager that gives access to internal solver objects
	 */
	protected InfoflowManager manager;
	
	@Override
	public void initialize(InfoflowManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void shutdown() {
		//
	}

}
