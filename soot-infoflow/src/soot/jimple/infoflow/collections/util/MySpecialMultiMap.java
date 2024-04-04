package soot.jimple.infoflow.collections.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Special MultiMap that saves the first-added element seperated from the element added afterward
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MySpecialMultiMap<K, V> {
    /**
     * Set that saves the first value separately
     *
     * @param <V> value type
     */
    protected static class MySet<V> {
        protected final V firstValue;
        protected final Set<V> otherValues;

        MySet(V v) {
            this(v, new HashSet<>());
        }

        private MySet(V v, Set<V> newSet) {
            this.firstValue = v;
            // This can be a thread-unsafe set because there is always a lock on the key
            // when the set is accessed.
            this.otherValues = newSet;
        }

        void add(V value) {
            if (!firstValue.equals(value))
                otherValues.add(value);
        }
    }

    /**
     * Dummy set that prevents any reuse
     *
     * @param <V> value type
     */
    protected static class NotReusableSet<V> extends MySet<V> {
        NotReusableSet() {
            super(null, Collections.emptySet());
        }

        @Override
        void add(V value) {
            // NO-OP
        }
    }
    protected NotReusableSet<V> notReusableSet = new NotReusableSet<>();

    protected final ConcurrentMap<K, MySet<V>> m;

    public MySpecialMultiMap() {
        this.m = new ConcurrentHashMap<>();
    }


    /**
     * Puts the value into the map and gets the first added value for this key
     *
     * @param key   key
     * @param value value
     * @return the first added value or null if the key had no mapping before
     */
    public V putAndGetFirst(K key, V value) {
        @SuppressWarnings("unchecked")
        V[] returnValue = (V[]) new Object[1];

        m.compute(key, (k, set) -> {
            if (set == null)
                return new MySet<>(value);

            returnValue[0] = set.firstValue;
            set.add(value);
            return set;
        });

        return returnValue[0];
    }

    /**
     * Runs the consumer on all other values and removes the key value mapping
     *
     * @param key      key
     * @param consumer function that runs on all other values
     */
    public void consumeOtherValuesAndRemove(K key, Consumer<V> consumer) {
        m.computeIfPresent(key, (k, set) -> {
            for (V v : set.otherValues)
                consumer.accept(v);
            return notReusableSet;
        });
    }

    /**
     * Runs the consumer on all other values and removes the key value mapping
     *
     * @param key      key
     * @param consumer function that runs on all other values
     */
    public void consumeOtherValues(K key, Consumer<V> consumer) {
        m.computeIfPresent(key, (k, set) -> {
            for (V v : set.otherValues)
                consumer.accept(v);
            return set;
        });
    }

    /**
     * Clears the mapping
     */
    public void clear() {
        this.m.clear();
    }
}
