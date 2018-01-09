/*******************************************************************************
 * Copyright (c) 2013 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package soot.jimple.infoflow.collect;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An modifiable set holding up to two elements. Particularly useful within flow functions.
 *
 * @param <E>
 * @see FlowFunction
 * 
 * @author Eric Bodden
 * @author Steven Arzt
 */
public class MutableTwoElementSet<E> extends AbstractSet<E> {
	
	protected E first, second;
	
	public MutableTwoElementSet() {
		this.first = null;
		this.second = null;
	}	

	public MutableTwoElementSet(E first, E second) {
		this.first = first;
		this.second = second;
	}
	
	public boolean add(E item) {
		if (first == null) {
			first = item;
			return true;
		}
		if (first != null && first.equals(item))
			return false;
		
		if (second == null) {
			second = item;
			return true;
		}
		if (second != null && second.equals(item))
			return false;

		throw new RuntimeException("Capacity exceeded");
	}
	
	public static <E> MutableTwoElementSet<E> twoElementSet(E first, E second) {
		return new MutableTwoElementSet<E>(first, second);
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int size = -1;
			int elementsRead = 0;
			
			@Override
			public boolean hasNext() {
				if (size < 0)
					size = size();
				return elementsRead < size;
			}

			@Override
			public E next() {
				switch(elementsRead) {
				case 0:
					elementsRead++;
					return first;
				case 1:
					elementsRead++;
					return second;
				default:
					throw new NoSuchElementException();	
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size() {
		int size = 0;
		if (first != null)
			size++;
		if (second != null)
			size++;
		return size;
	}	
}
