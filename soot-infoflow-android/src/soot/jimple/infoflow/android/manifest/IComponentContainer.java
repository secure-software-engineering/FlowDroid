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

}
