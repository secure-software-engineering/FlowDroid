package soot.jimple.infoflow.collect;

import java.util.Collection;
import java.util.Iterator;

/**
 * This collection does nothing. You can add elements to it, but they won't be
 * stored. This collection is useful if you have to supply a collection to some
 * API method, but you don't really care about what gets added to the collection.
 * 
 * @author Steven Arzt
 *
 * @param <E> The type of elements in the collection
 */
public class BlackHoleCollection<E> implements Collection<E> {

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public E next() {
				return null;
			}
			
			@Override
			public void remove() {
			}
			
		};
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return a;
	}

	@Override
	public boolean add(E e) {
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {
	}

}
