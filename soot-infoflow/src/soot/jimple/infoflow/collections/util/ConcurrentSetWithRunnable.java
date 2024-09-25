package soot.jimple.infoflow.collections.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Thread-safe set that allows to execute a runnable while keeping a lock on a
 * key
 *
 * @param <E> type
 */
public class ConcurrentSetWithRunnable<E> extends AbstractSet<E> implements Set<E>, Serializable {

	private static final long serialVersionUID = 792232379356912460L;

	private final Map<E, Boolean> m;
	private transient Set<E> s;

	public ConcurrentSetWithRunnable() {
		this.m = new ConcurrentHashMap<>();
		this.s = this.m.keySet();
	}

	/**
	 * Runs the runnable if the element is absent
	 *
	 * @param e element
	 * @param r runnable
	 * @return true if the element was absent
	 */
	public boolean runIfAbsent(E e, Runnable r) {
		return this.m.computeIfAbsent(e, (k) -> {
			r.run();
			return null;
		}) == null;
	}

	// Below derived from Collections.newSetFromMap(...)
	@Override
	public void clear() {
		this.m.clear();
	}

	@Override
	public int size() {
		return this.m.size();
	}

	@Override
	public boolean isEmpty() {
		return this.m.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.m.containsKey(o);
	}

	@Override
	public boolean remove(Object o) {
		return this.m.remove(o) != null;
	}

	@Override
	public boolean add(E e) {
		return this.m.put(e, Boolean.TRUE) == null;
	}

	@Override
	public Iterator<E> iterator() {
		return this.s.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.s.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.s.toArray(a);
	}

	@Override
	public String toString() {
		return this.s.toString();
	}

	@Override
	public int hashCode() {
		return this.s.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o == this || this.s.equals(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.s.containsAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.s.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.s.retainAll(c);
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		this.s.forEach(action);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return this.s.removeIf(filter);
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.s.spliterator();
	}

	@Override
	public Stream<E> stream() {
		return this.s.stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return this.s.parallelStream();
	}
}
