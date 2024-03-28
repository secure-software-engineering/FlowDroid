/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.collect;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

/**
 * Multithreaded version of a hash set
 * 
 * @author Steven Arzt
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E> {

	protected final ConcurrentMap<E, E> delegate;

	private static final MapMaker mapMaker = new MapMaker()
			.concurrencyLevel(Runtime.getRuntime().availableProcessors());

	/**
	 * Creates a new, empty ConcurrentHashSet. 
	 */
	public ConcurrentHashSet() {
		// had some really weird NPEs with Java's ConcurrentHashMap (i.e. got a
		// NPE at size()), now trying witrh Guava instead
		delegate = mapMaker.makeMap();
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
		assert o != null;
		return delegate.put(o, o) == null;
	}

	public E addOrGet(E o) {
		assert o != null;
		return delegate.putIfAbsent(o, o);
	}

	@Override
	public boolean remove(Object o) {
		return delegate.remove(o) != null;
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
