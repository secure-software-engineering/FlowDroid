package soot.jimple.infoflow.collections.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Iterators;

/**
 * Immutable set simply using an array in the background
 *
 * @param <E> element type
 */
public class ImmutableArraySet<E> extends AbstractSet<E> implements Set<E> {
    private final E[] innerArray;

    @SuppressWarnings("unchecked")
    public ImmutableArraySet(E e) {
        this.innerArray = (E[]) new Object[1];
        this.innerArray[0] = e;
    }

    @SuppressWarnings("unchecked")
    public ImmutableArraySet(Set<E> other) {
        this.innerArray = (E[]) new Object[other.size()];
        int i = 0;
        for (E e : other)
            this.innerArray[i++] = e;
    }

    public ImmutableArraySet(ImmutableArraySet<E> other) {
        this.innerArray = other.innerArray;
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.forArray(this.innerArray);
    }

    @Override
    public int size() {
        return this.innerArray.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImmutableArraySet<?> that = (ImmutableArraySet<?>) o;
        return Arrays.equals(innerArray, that.innerArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(innerArray);
    }
}
