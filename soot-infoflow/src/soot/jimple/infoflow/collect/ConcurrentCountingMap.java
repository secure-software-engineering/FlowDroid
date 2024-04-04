package soot.jimple.infoflow.collect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A map that associates an element with a number. This map will never return a
 * null value, but will pre-initialize all counters with a default value of
 * zero.
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentCountingMap<T> implements ConcurrentMap<T, Integer> {

	public enum LockingMode {
		/**
		 * Perform no locking at all. The map is fully synchronous
		 */
		NoLocking,
		/**
		 * Perform fast locking. This will make the map mostly consistent across
		 * threads.
		 */
		Fast,
		/**
		 * Always lock. This will make the map behave like a fully synchronized map.
		 */
		Safe
	}

	private final ConcurrentMap<T, AtomicInteger> map;
	private final ReentrantLock lock = new ReentrantLock();
	private LockingMode lockingMode = LockingMode.NoLocking;
	private AtomicInteger changeCounter = new AtomicInteger();

	public class Entry implements Map.Entry<T, Integer> {

		private final Map.Entry<T, AtomicInteger> parentEntry;

		private Entry(Map.Entry<T, AtomicInteger> parentEntry) {
			this.parentEntry = parentEntry;
		}

		@Override
		public T getKey() {
			return parentEntry.getKey();
		}

		@Override
		public Integer getValue() {
			AtomicInteger i = parentEntry.getValue();
			return i == null ? 0 : i.get();
		}

		@Override
		public Integer setValue(Integer value) {
			try {
				switch (lockingMode) {
				case Fast:
					if (lock.isLocked())
						lock.lock();
					break;
				case Safe:
					lock.lock();
				}

				AtomicInteger i = parentEntry.setValue(new AtomicInteger(value));
				return i == null ? 0 : i.get();
			} finally {
				if (lock.isHeldByCurrentThread())
					lock.unlock();
			}
		}

	}

	public ConcurrentCountingMap() {
		this.map = new ConcurrentHashMap<>();
	}

	public ConcurrentCountingMap(int size) {
		this.map = new ConcurrentHashMap<>(size);
	}

	public ConcurrentCountingMap(Map<T, AtomicInteger> map) {
		this.map = new ConcurrentHashMap<>(map);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof Integer) {
			AtomicInteger i = new AtomicInteger((Integer) value);
			return map.containsValue(i);
		}
		return false;
	}

	@Override
	public Integer get(Object key) {
		AtomicInteger i = map.get(key);
		return i == null ? 0 : i.get();
	}

	@Override
	public Integer put(T key, Integer value) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			AtomicInteger old = map.put(key, value == null ? null : new AtomicInteger(value));
			changeCounter.incrementAndGet();
			return old == null ? 0 : old.get();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public Integer remove(Object key) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			AtomicInteger old = map.remove(key);
			changeCounter.incrementAndGet();
			return old == null ? 0 : old.get();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public void putAll(Map<? extends T, ? extends Integer> m) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			for (T t : m.keySet()) {
				Integer i = m.get(t);
				map.put(t, i == null ? null : new AtomicInteger(i));
				changeCounter.incrementAndGet();
			}
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public void clear() {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			map.clear();
			changeCounter.incrementAndGet();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public Set<T> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<Integer> values() {
		return map.values().stream().map(i -> i == null ? 0 : i.get()).collect(Collectors.toSet());
	}

	@Override
	public Set<Map.Entry<T, Integer>> entrySet() {
		return map.entrySet().stream().map(e -> new Entry(e)).collect(Collectors.toSet());
	}

	@Override
	public Integer putIfAbsent(T key, Integer value) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			AtomicInteger i = map.computeIfAbsent(key, k -> new AtomicInteger(value));
			if (i == null) {
				changeCounter.incrementAndGet();
				return 0;
			} else
				return i.get();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public boolean remove(Object key, Object value) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			if (value instanceof Integer) {
				boolean res = map.remove(key, new AtomicInteger((Integer) value));
				changeCounter.incrementAndGet();
				return res;
			}
			return false;
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public boolean replace(T key, Integer oldValue, Integer newValue) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			if (oldValue == null || newValue == null)
				return false;
			boolean res = map.replace(key, new AtomicInteger(oldValue), new AtomicInteger(newValue));
			changeCounter.incrementAndGet();
			return res;
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	@Override
	public Integer replace(T key, Integer value) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			if (value == null)
				return null;
			AtomicInteger i = map.replace(key, new AtomicInteger(value));
			changeCounter.incrementAndGet();
			return i == null ? 0 : i.get();
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	/**
	 * Increments the counter associated with the given value by one
	 * 
	 * @param key The key of the value for which to increment the counter
	 * @return The new counter value
	 */
	public int increment(T key) {
		return increment(key, 1);
	}

	/**
	 * Increments the counter associated with the given value by the given delta
	 * 
	 * @param key   The key of the value for which to increment the counter
	 * @param delta The delta by which to increment the counter
	 * @return The new counter value
	 */
	public int increment(T key, int delta) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			AtomicInteger i = map.computeIfAbsent(key, k -> new AtomicInteger(0));
			changeCounter.incrementAndGet();
			int val = 0;
			for (int j = 0; j < delta; j++)
				val = i.incrementAndGet();
			return val;
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	/**
	 * Decrements the counter associated with the given value by one
	 * 
	 * @param key The key of the value for which to decrement the counter
	 * @return The new counter value
	 */
	public int decrement(T key) {
		try {
			switch (lockingMode) {
			case Fast:
				if (lock.isLocked())
					lock.lock();
				break;
			case Safe:
				lock.lock();
				break;
			}

			AtomicInteger i = map.get(key);
			if (i == null)
				return 0;
			int res = i.decrementAndGet();
			changeCounter.incrementAndGet();
			return res;
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	/**
	 * Gets all keys that have the given value. Note that values are free to change
	 * at any time, so the set returned by this method may contain keys that have
	 * other values as well in case they were mutated by a different thread while
	 * this method was running.
	 * 
	 * @param value The expected value
	 * @return A set with all keys that have the given value
	 */
	public Set<T> getByValue(int value) {
		Set<T> set = new HashSet<>();
		for (java.util.Map.Entry<T, AtomicInteger> e : map.entrySet()) {
			AtomicInteger atomicInt = e.getValue();
			if (atomicInt != null && atomicInt.get() == value)
				set.add(e.getKey());
		}
		return set;
	}

	/**
	 * Sets how whether and how this class shall perform locking
	 * 
	 * @param lockingMode The new locking mode
	 */
	public void setLockingMode(LockingMode lockingMode) {
		this.lockingMode = lockingMode;
	}

	/**
	 * Creates a snapshot of this map
	 * 
	 * @return A copy of the current state of this map
	 */
	public ConcurrentCountingMap<T> snapshot() {
		try {
			lock.lock();

			ConcurrentCountingMap<T> snapshot = new ConcurrentCountingMap<>();
			for (T key : map.keySet())
				snapshot.put(key, map.get(key).get());
			return snapshot;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Creates a partial snapshot of this map
	 * 
	 * @param subset The subset of element for which to create a partial snapshot
	 * @return A copy of the current state of this map
	 */
	public ConcurrentCountingMap<T> snapshot(Collection<T> subset) {
		try {
			lock.lock();

			ConcurrentCountingMap<T> snapshot = new ConcurrentCountingMap<>(subset.size());
			for (T key : subset) {
				AtomicInteger atomic = map.get(key);
				if (atomic != null)
					snapshot.put(key, atomic.get());
			}
			return snapshot;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Gets the current state of the change counter. This counter is modified
	 * whenever the contents of the map are changed.
	 * 
	 * @return The current value of the change counter
	 */
	public int getChangeCounter() {
		return changeCounter.get();
	}

}
