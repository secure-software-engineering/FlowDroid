package soot.jimple.infoflow.android.manifest.containers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.IComponentContainer;

/**
 * Empty component container
 * 
 * @author Steven Arzt
 *
 */
public class EmptyComponentContainer<E extends IAndroidComponent> implements IComponentContainer<E> {

	private final static EmptyComponentContainer<?> INSTANCE = new EmptyComponentContainer<>();

	private EmptyComponentContainer() {
	}

	@SuppressWarnings("unchecked")
	public static <E extends IAndroidComponent> EmptyComponentContainer<E> get() {
		return (EmptyComponentContainer<E>) INSTANCE;
	}

	@Override
	public List<E> asList() {
		return Collections.emptyList();
	}

	@Override
	public Iterator<E> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public E getComponentByName(String name) {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

}
