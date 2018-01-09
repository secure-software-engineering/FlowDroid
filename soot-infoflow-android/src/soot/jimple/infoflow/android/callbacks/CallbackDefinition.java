package soot.jimple.infoflow.android.callbacks;

import soot.SootMethod;

/**
 * Data class that represents a single Android callback
 * 
 * @author Steven Arzt
 *
 */
public class CallbackDefinition {
	
	/**
	 * Enumeration containing the possible types of callbacks
	 * 
	 * @author Steven Arzt
	 *
	 */
	public enum CallbackType {
		/**
		 * The callback is on a UI control, e.g., a button click handler
		 */
		Widget,
		
		/**
		 * The callback is a normal method without special properties
		 */
		Default
	}
	
	private final SootMethod targetMethod;
	private final SootMethod parentMethod;
	private final CallbackType callbackType;
	
	/**
	 * Creates a new instance of the {@link CallbackDefinition} class
	 * @param targetMethod The callback method
	 * @param parentMethod The parent method in the Android framework, e.g.,
	 * in the callback interface method
	 * @param callbackType The type of callback, e.g., UI callback
	 */
	public CallbackDefinition(SootMethod targetMethod, SootMethod parentMethod, CallbackType callbackType) {
		this.targetMethod = targetMethod;
		this.parentMethod = parentMethod;
		this.callbackType = callbackType;
	}
	
	/**
	 * Gets the callback method represented by this data object
	 * @return The callback method represented by this data object
	 */
	public SootMethod getTargetMethod() {
		return this.targetMethod;
	}
	
	/**
	 * Gets the parent method in the Android framework that causes the target
	 * method to be a callback. The parent is usually the method in the
	 * framework's callback interface.
	 * @return The parent method in the Android framework
	 */
	public SootMethod getParentMethod() {
		return this.parentMethod;
	}
	
	/**
	 * Gets the type of callback, e.g., UI callback
	 * @return The type of callback, e.g., UI callback
	 */
	public CallbackType getCallbackType() {
		return this.callbackType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callbackType == null) ? 0 : callbackType.hashCode());
		result = prime * result + ((targetMethod == null) ? 0 : targetMethod.hashCode());
		result = prime * result + ((parentMethod == null) ? 0 : parentMethod.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallbackDefinition other = (CallbackDefinition) obj;
		if (callbackType != other.callbackType)
			return false;
		if (targetMethod == null) {
			if (other.targetMethod != null)
				return false;
		} else if (!targetMethod.equals(other.targetMethod))
			return false;
		if (parentMethod == null) {
			if (other.parentMethod != null)
				return false;
		} else if (!parentMethod.equals(other.parentMethod))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return targetMethod.toString();
	}
	
}
