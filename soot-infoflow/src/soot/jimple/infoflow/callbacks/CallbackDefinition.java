package soot.jimple.infoflow.callbacks;

import soot.SootMethod;

/**
 * A base class for callbacks.
 * 
 * @author Steven Arzt, Marc Miltenberger
 */
public class CallbackDefinition {

	protected final SootMethod targetMethod;
	protected final SootMethod parentMethod;

	/**
	 * Creates a new instance of the {@link CallbackDefinition} class
	 * 
	 * @param targetMethod The callback method
	 * @param parentMethod The parent method in the Android framework, e.g., in the
	 *                     callback interface method
	 */
	public CallbackDefinition(SootMethod targetMethod, SootMethod parentMethod) {
		this.targetMethod = targetMethod;
		this.parentMethod = parentMethod;
	}

	/**
	 * Gets the callback method represented by this data object
	 * 
	 * @return The callback method represented by this data object
	 */
	public SootMethod getTargetMethod() {
		return this.targetMethod;
	}

	/**
	 * Gets the parent method in the Android framework that causes the target method
	 * to be a callback. The parent is usually the method in the framework's
	 * callback interface.
	 * 
	 * @return The parent method in the Android framework
	 */
	public SootMethod getParentMethod() {
		return this.parentMethod;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
