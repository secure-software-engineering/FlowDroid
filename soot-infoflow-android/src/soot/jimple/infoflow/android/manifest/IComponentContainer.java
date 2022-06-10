package soot.jimple.infoflow.android.manifest;

import java.util.List;

/**
 * Container for (potentially delay-loaded) components inside an Android app
 * 
 * @author Steven Arzt
 *
 * @param The type of Android component inside this container
 */
public interface IComponentContainer<E extends IAndroidComponent> extends Iterable<E> {

	/**
	 * Gets the components in this container as a list
	 * 
	 * @return A list that contains all the components in this container
	 */
	public List<E> asList();

	/**
	 * Gets the component with the given unique class name
	 * 
	 * @param name The name of the class that implements the component
	 * @return The component definition for the given class name, or
	 *         <code>null</code> if no such component exists
	 */
	public E getComponentByName(String name);

	/**
	 * Checks whether a component with the given unique class name exists
	 * 
	 * @param name The name of the class that implements the component
	 * @return True if this container contains an Android component that is
	 *         implemented by a class with the given name, false otherwise
	 */
	default public boolean hasComponentByName(String name) {
		return name != null && getComponentByName(name) != null;
	}

	/**
	 * Checks whether this container is empty, i.e., does not contain any components
	 * 
	 * @return True if this container is empty, false otherwise
	 */
	public boolean isEmpty();

}
