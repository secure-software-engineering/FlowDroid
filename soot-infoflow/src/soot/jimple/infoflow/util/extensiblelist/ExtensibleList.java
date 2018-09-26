
package soot.jimple.infoflow.util.extensiblelist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * This list is intended to be used to save lists, which share the same elements
 * at the start of the list. The list is meant to be iterated in reverse order,
 * and operations requiring accessing the head will be slower.
 * 
 * @author Marc Miltenberger
 * @param <T> the type to save in this list
 */
public class ExtensibleList<T> {
	// private List<T> check;

	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(ExtensibleList.class);

	private static class ExtensibleListIterator<T> implements Iterator<T> {

		public ExtensibleListIterator(ExtensibleList<T> start, ListIterator<T> itStart) {
			this.list = start;
			this.it = itStart;
		}

		ExtensibleList<T> list;
		private ListIterator<T> it;

		@Override
		public boolean hasNext() {
			if (list == null)
				return false;

			if (it == null || !it.hasPrevious()) {
				while (true) {
					int l = list.previousLockedAt;
					list = list.previous;
					if (list == null)
						return false;
					if (list.actualList == null)
						continue;

					if (l < 0)
						l = list.actualList.size();
					if (l < 0)
						throw new RuntimeException("List has less than zero elements");
					it = list.actualList.listIterator(l);
					if (it.hasPrevious())
						return true;
				}
			}
			return it.hasPrevious();
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new ArrayIndexOutOfBoundsException("No more elements");

			return it.previous();
		}

		public boolean isSamePosition(ExtensibleListIterator<T> it2) {
			return list.actualList == it2.list && it.previousIndex() == it2.it.previousIndex();
		}

	}

	ExtensibleList<T> previous;

	int size;
	int lastLocked = -1;
	private List<T> actualList = null;
	private int previousLockedAt;
	private int savedHashCode = Integer.MIN_VALUE;
	private int parentHashCode;

	/**
	 * Creates a new extensible list. The given parameter is the prepended to this
	 * list.
	 * 
	 * @param original the prepended list
	 */
	public ExtensibleList(ExtensibleList<T> original) {
		// Someone else might be doing the same right now, which means that "original"
		// can be in an unexpected state. Even worse, we could bring it into such a
		// state by writing the lock index.
		synchronized (original) {
			this.size = original.size;
			this.previous = original;

			if (original.actualList != null) {
				previousLockedAt = original.actualList.size();
				original.lastLocked = Math.min(original.lastLocked, original.actualList.size());
				if (original.lastLocked == -1)
					original.lastLocked = original.actualList.size();
			} else if (original.previous != null) {
				previousLockedAt = original.previousLockedAt;
				previous = original.previous;
			} else
				previousLockedAt = 0;

			/*
			 * this.check = new ArrayList<>(original.check.size()); for (T i :
			 * original.check) check.add(i);
			 */

			parentHashCode = original.onlyElementHashCode();
		}
	}

	@Override
	public int hashCode() {
		return onlyElementHashCode() ^ (size * 15);
	}

	/**
	 * Returns the aggregated hash code of each element, including the elements in
	 * the previous list
	 * 
	 * @return the hash code
	 */
	private int onlyElementHashCode() {
		if (savedHashCode != Integer.MIN_VALUE)
			return savedHashCode;
		int h = parentHashCode;
		if (actualList != null) {
			for (T l : actualList) {
				h = 31 * h + l.hashCode();
			}
		}
		savedHashCode = h;
		return h;
	}

	public ExtensibleList() {
		previousLockedAt = -1;
		parentHashCode = 0;
		// this.check = new ArrayList<>();
	}

	/**
	 * Returns the size of the list
	 * 
	 * @return the size
	 */
	public int size() {
		return size;
	}

	/**
	 * In case this list is locked, a new one is created and returned. So, always
	 * use the returned list!
	 * 
	 * @param add the object to add
	 * @return the new list
	 */
	public ExtensibleList<T> add(T add) {
		if (actualList == null)
			actualList = new ArrayList<T>(4);
		actualList.add(add);
		size++;
		savedHashCode = Integer.MIN_VALUE;

		// printStats();
		/*
		 * if (check != null) { check.add(add); if (savedHashCode != Integer.MIN_VALUE)
		 * savedHashCode ^= add.hashCode(); check(); }
		 */
		return this;
	}

	private void printStats() {
		int i = 0;
		int max = 0, min = 0;
		ExtensibleList<T> list = this;
		while (list != null) {
			i++;
			if (list.actualList != null)
				max = Math.max(max, list.actualList.size());
			if (list.actualList != null)
				min = Math.min(min, list.actualList.size());
			list = list.previous;
		}
		/* LOGGER.debug */System.out
				.println(String.format("%d list parts for %d elements, min: %d, max: %d", i, size, min, max));
	}

	public ExtensibleList<T> addAll(T... add) {
		for (int i = 0; i < add.length; i++) {
			add(add[i]);
		}
		return this;
	}

	/*
	 * private void check() { //Check whether size is correct if (size !=
	 * check.size()) throw new AssertionError();
	 * 
	 * if (size >= 1) if (!getFirstSlow().equals(check.get(0))) throw new
	 * AssertionError();
	 * 
	 * ExtensibleList<T> l = new ExtensibleList<>(); l.check = null; for (T c :
	 * check) { l.add(c); }
	 * 
	 * if (l.hashCode() != hashCode()) throw new AssertionError(); if
	 * (!l.equals(this)) throw new AssertionError();
	 * 
	 * ExtensibleListIterator<T> it = reverseIterator(); for (int x = check.size() -
	 * 1; x >= 0; x--) { if (!it.hasNext()) throw new AssertionError(); if
	 * (!it.next().equals(check.get(x))) throw new AssertionError(); } if
	 * (it.hasNext()) throw new AssertionError(); }
	 */

	/**
	 * Returns true when this list is empty
	 * 
	 * @return true when this list is empty
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Removes the last element and returns it Alternatively, it may return an
	 * extensible list, in case the original one could not be changed!
	 * 
	 * @return the last element (or null) or an extensible list in case it cannot be
	 *         changed in place.
	 */
	public Object removeLast() {
		return getOrRemoveLast(true);
	}

	/**
	 * Returns an iterator, which works reverse through the list
	 * 
	 * @return the iterator
	 */
	public ExtensibleListIterator<T> reverseIterator() {
		final ListIterator<T> itStart;
		if (actualList != null)
			itStart = actualList.listIterator(actualList.size());
		else
			itStart = null;
		return new ExtensibleListIterator<T>(this, itStart);
	}

	/**
	 * Returns the last element
	 * 
	 * @return the last element or null
	 */
	public T getLast() {
		return (T) getOrRemoveLast(false);
	}

	/**
	 * This method returns the last element and also removes it when the given
	 * boolean parameter is true
	 * 
	 * @param remove whether to remove the last element
	 * @return the last element (or null)
	 */
	private Object getOrRemoveLast(boolean remove) {
		if (size == 0)
			return null;

		ExtensibleList<T> check = this;
		int lockedAt = -1;
		boolean updateHash = remove;
		try {
			while (check != null) {
				if (check.actualList != null && !check.actualList.isEmpty()) {
					int elem;
					if (lockedAt == -1)
						elem = check.actualList.size() - 1;
					else
						elem = lockedAt;

					if (remove) {
						if (elem < check.lastLocked) {
							// Too bad... We cannot remove the element, since it is locked (some other list
							// may need it).
							ExtensibleList<T> result = new ExtensibleList<>();
							result.actualList = new ArrayList<T>(size);
							ExtensibleListIterator<T> it = reverseIterator();
							it.next(); // remove first element
							while (it.hasNext()) {
								T n = it.next();
								result.actualList.add(n);
							}
							Collections.reverse(result.actualList);
							result.size = size - 1;
							updateHash = false;
							return result;
						}
						T b = check.actualList.remove(elem);
						size--;

						// We need to recompute the hash code:
						savedHashCode = Integer.MIN_VALUE;
						/*
						 * check.check.remove(check.check.size() - 1); check();
						 */
						return b;
					} else
						return check.actualList.get(elem);
				}
				lockedAt = check.previousLockedAt - 1;
				check = check.previous;
			}
			if (remove) {
				throw new RuntimeException("No element found to delete");
			}
		} finally {
			if (updateHash) {
				// we have changed one list, so let's update all hashcodes from here up to
				// this list
				// since this list was not locked, we do not have to worry about other lists,
				// since no other list should dependent on that list where we removed the
				// element.
				ExtensibleList<T> update = this;
				List<ExtensibleList<T>> chain = new ArrayList<ExtensibleList<T>>();
				while (true) {
					chain.add(update);
					if (update == check)
						break;
					update = update.previous;
				}

				int parentHashCode = check.parentHashCode;
				ListIterator<ExtensibleList<T>> l = chain.listIterator(chain.size());
				while (l.hasPrevious()) {
					ExtensibleList<T> list = l.previous();
					list.parentHashCode = parentHashCode;
					// force recomputation
					list.savedHashCode = Integer.MIN_VALUE;
					parentHashCode = list.onlyElementHashCode();
				}
			}
		}

		return null;
	}

	@Override
	public boolean equals(Object obj) {
		ExtensibleList<T> other = (ExtensibleList<T>) obj;
		if (other.size != size)
			return false;
		if (previous == other.previous) {
			if (previousLockedAt != other.previousLockedAt)
				return false;
			if (actualList == null || actualList.isEmpty()) {
				return other.actualList == null || other.actualList.isEmpty();
			}
			return actualList.equals(other.actualList);
		}
		ExtensibleListIterator<T> it1 = reverseIterator();
		ExtensibleListIterator<T> it2 = other.reverseIterator();
		while (true) {
			boolean i1 = it1.hasNext();
			boolean i2 = it2.hasNext();
			if (i1 != i2)
				return false;

			if (!i1)
				return true;

			if (it1.isSamePosition(it2))
				return true;

			T t1 = it1.next();
			T t2 = it2.next();
			if (!t1.equals(t2))
				return false;
		}
	}

	/**
	 * Returns the first element. It is relatively slow compared to getting the last
	 * element. This method should not be used in normal path reconstruction.
	 * 
	 * @return the first element or null
	 */
	public T getFirstSlow() {
		ExtensibleList<T> list = this;
		T first = null;
		while (true) {
			if (list.actualList != null) {
				first = list.actualList.get(0);
			}
			if (list.previous == null)
				break;

			list = list.previous;
		}
		return first;
	}

	/**
	 * Adds an element to the beginning. Since this data structure was not meant to
	 * prepend an element, it is relatively slow, so be careful. Does not change
	 * this list, but returns a new list! This method should not be used in normal
	 * path reconstruction.
	 * 
	 * @param toAdd the element to add
	 * @return the new list
	 */
	public ExtensibleList<T> addFirstSlow(T toAdd) {
		ExtensibleList<T> list = new ExtensibleList<T>();
		list.actualList = new LinkedList<T>();
		ExtensibleListIterator<T> it = reverseIterator();
		while (it.hasNext()) {
			list.actualList.add(0, it.next());
		}
		list.actualList.add(0, toAdd);
		// list.check = list.actualList;
		list.size = size + 1;
		return list;
	}

	@Override
	public String toString() {
		List<T> res = new ArrayList<T>();
		ExtensibleListIterator<T> it = reverseIterator();
		while (it.hasNext()) {
			res.add(it.next());
		}
		Collections.reverse(res);
		return res.toString();
	}

	public List<T> getActualList() {
		return actualList;
	}

}
