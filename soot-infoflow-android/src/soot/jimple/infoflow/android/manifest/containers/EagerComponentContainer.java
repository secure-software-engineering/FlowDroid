package soot.jimple.infoflow.android.manifest.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.IComponentContainer;

/**
 * Component container for eager loading
 * 
 * @author Steven Arzt
 *
 */
public class EagerComponentContainer<E extends IAndroidComponent> implements IComponentContainer<E> {

	private final Map<String, E> innerCollection;

	public EagerComponentContainer(Collection<E> innerCollection) {
		this.innerCollection = new HashMap<>(innerCollection.size());
		for (E e : innerCollection)
			this.innerCollection.put(e.getNameString(), e);
	}

	@Override
	public List<E> asList() {
		return new ArrayList<>(innerCollection.values());
	}

	@Override
	public Iterator<E> iterator() {
		return innerCollection.values().iterator();
	}

	@Override
	public E getComponentByName(String name) {
		return innerCollection.get(name);
	}

	@Override
	public boolean isEmpty() {
		return innerCollection.isEmpty();
	}

}
