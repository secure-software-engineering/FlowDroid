package soot.jimple.infoflow.collections.util;

import java.util.HashSet;

public class NonNullHashSet<E> extends HashSet<E> {
    @Override
    public boolean add(E e) {
        if (e == null)
            return false;
        return super.add(e);
    }
}
