package soot.jimple.infoflow.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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

	private final ConcurrentMap<T, AtomicInteger> map;

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
			AtomicInteger i = parentEntry.setValue(new AtomicInteger(value));
			return i == null ? 0 : i.get();
		}

	}

	public ConcurrentCountingMap() {
		this.map = new ConcurrentHashMap<>();
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
		AtomicInteger old = map.put(key, value == null ? null : new AtomicInteger(value));
		return old == null ? 0 : old.get();
	}

	@Override
	public Integer remove(Object key) {
		AtomicInteger old = map.remove(key);
		return old == null ? 0 : old.get();
	}

	@Override
	public void putAll(Map<? extends T, ? extends Integer> m) {
		for (T t : m.keySet()) {
			Integer i = m.get(t);
			map.put(t, i == null ? null : new AtomicInteger(i));
		}
	}

	@Override
	public void clear() {
		map.clear();
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
		AtomicInteger i = map.putIfAbsent(key, new AtomicInteger(value));
		return i == null ? 0 : i.get();
	}

	@Override
	public boolean remove(Object key, Object value) {
		if (value instanceof Integer)
			return map.remove(key, new AtomicInteger((Integer) value));
		return false;
	}

	@Override
	public boolean replace(T key, Integer oldValue, Integer newValue) {
		if (oldValue == null || newValue == null)
			return false;
		return map.replace(key, new AtomicInteger(oldValue), new AtomicInteger(newValue));
	}

	@Override
	public Integer replace(T key, Integer value) {
		if (value == null)
			return null;
		AtomicInteger i = map.replace(key, new AtomicInteger(value));
		return i == null ? 0 : i.get();
	}

	/**
	 * Increments the counter associated with the given value by one
	 * 
	 * @param key The key of the value for which to increment the counter
	 * @return The new counter value
	 */
	public int increment(T key) {
		AtomicInteger i = map.putIfAbsent(key, new AtomicInteger(1));
		if (i == null)
			return 1;
		return i.incrementAndGet();
	}

}
