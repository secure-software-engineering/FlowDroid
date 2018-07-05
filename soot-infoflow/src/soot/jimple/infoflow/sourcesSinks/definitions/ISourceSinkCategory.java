package soot.jimple.infoflow.sourcesSinks.definitions;

/**
 * Common interface for all classes that provide categorization or some other
 * form of organization for sources or sinks
 * 
 * @author Steven Arzt
 *
 */
public interface ISourceSinkCategory {

	/**
	 * Gets a human-readable description of the current category. This can be
	 * different from toString(), which is allowed to yield a more technical
	 * description.
	 * 
	 * @return A human readable description for this category
	 */
	public String getHumanReadableDescription();

	/**
	 * Gets a machine-readable description of the current category.
	 *  
	 * @return An id for this category
	 */
	public String getID();

}
