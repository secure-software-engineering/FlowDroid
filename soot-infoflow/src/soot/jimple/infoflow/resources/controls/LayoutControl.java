package soot.jimple.infoflow.resources.controls;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Abstract base class for all layout controls
 * 
 * @author Steven Arzt
 *
 */
public abstract class LayoutControl {

	/**
	 * Gets whether this control can contain sensitive data according to some
	 * reasonable general definition
	 * 
	 * @return True if this control can contain sensitive information, otherwise
	 *         false
	 */
	public boolean isSensitive() {
		return false;
	}

	/**
	 * If this control shall be treated as a source, this method is called to obtain
	 * the precise definition of the source
	 * 
	 * @return The source definition for this layout control
	 */
	public abstract ISourceSinkDefinition getSourceDefinition();

}
