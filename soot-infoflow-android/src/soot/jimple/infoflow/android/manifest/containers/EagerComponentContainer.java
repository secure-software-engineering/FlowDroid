package soot.jimple.infoflow.android.manifest.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.IComponentContainer;

/**
 * Component container for eager loading
 * 
 * @author Steven Arzt
 *
 */
public class EagerComponentContainer<E extends IAndroidComponent> implements IComponentContainer<E> {

	private final Collection<E> innerCollection;

	public EagerComponentContainer(Collection<E> innerCollection) {
		this.innerCollection = innerCollection;
	}

	@Override
	public List<E> asList() {
		return new ArrayList<>(innerCollection);
	}

	@Override
	public Iterator<E> iterator() {
		return innerCollection.iterator();
	}

}
