package soot.jimple.infoflow.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentIdentityHashMap<K, V> implements ConcurrentMap<K, V> {

	private final ConcurrentMap<IdentityWrapper<K>, V> innerMap;

	public ConcurrentIdentityHashMap() {
		this.innerMap = new ConcurrentHashMap<IdentityWrapper<K>, V>();
	}

	@Override
	public int size() {
		return innerMap.size();
	}

	@Override
	public boolean isEmpty() {
		return innerMap.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		return innerMap.containsKey(new IdentityWrapper<K>((K) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return innerMap.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		return innerMap.get(new IdentityWrapper<K>((K) key));
	}

	@Override
	public V put(K key, V value) {
		return innerMap.put(new IdentityWrapper<K>(key), value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		return innerMap.remove(new IdentityWrapper<K>((K) key));
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> entry : m.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	@Override
	public void clear() {
		innerMap.clear();
	}

	@Override
	public Set<K> keySet() {
		Set<K> set = Collections.newSetFromMap(new IdentityHashMap<K, Boolean>());
		for (IdentityWrapper<K> k : innerMap.keySet())
			set.add(k.getContents());
		return set;
	}

	@Override
	public Collection<V> values() {
		return innerMap.values();
	}

	public class MapEntry implements Entry<K, V> {

		private final K key;
		private final V value;

		public MapEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new RuntimeException("Unsupported operation");
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (this.getClass() != other.getClass())
				return false;
			@SuppressWarnings("unchecked")
			MapEntry me = (MapEntry) other;
			return (this.key == me.key && this.value.equals(me.value));
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(key) + value.hashCode();
		}

	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> set = new HashSet<Entry<K, V>>();
		for (Entry<IdentityWrapper<K>, V> entry : innerMap.entrySet())
			set.add(new MapEntry(entry.getKey().getContents(), entry.getValue()));
		return set;
	}

	@Override
	public String toString() {
		return innerMap.toString();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return innerMap.putIfAbsent(new IdentityWrapper<K>(key), value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return innerMap.remove(new IdentityWrapper<>(key), value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return innerMap.replace(new IdentityWrapper<K>(key), oldValue, newValue);
	}

	@Override
	public V replace(K key, V value) {
		return innerMap.replace(new IdentityWrapper<K>(key), value);
	}

}
