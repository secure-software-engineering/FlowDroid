package soot.jimple.infoflow.collect;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

/**
 * HashSet with weak keys
 * 
 * @author Steven Arzt
 *
 * @param <E>
 */
public class WeakConcurrentHashSet<E> extends AbstractSet<E> {

    protected ConcurrentMap<E,E> delegate;
    
    /**
     * Creates a new, empty ConcurrentHashSet. 
     */
    public WeakConcurrentHashSet() {
        delegate = new MapMaker().weakKeys().concurrencyLevel
        		(Runtime.getRuntime().availableProcessors()).makeMap();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.keySet().iterator();
    }

    @Override
    public boolean add(E o) {
        return delegate.put(o, o)==null;
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o)!=null;
    }

    @Override
    public void clear() {
        delegate.entrySet().clear();
    }

    @Override
	public int hashCode() {
		return delegate.hashCode();
	}

    @Override
	public boolean equals(Object obj) {
		return obj instanceof ConcurrentHashSet && delegate.equals(obj);
	}
	
    @Override
	public String toString() {
		return delegate.keySet().toString();
	}

}
