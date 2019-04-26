package soot.jimple.infoflow.collect;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Container object that wraps an array such that it can be used as a key in a
 * {@link HashMap}
 * 
 * @author Steven Arzt
 *
 */
public class ArrayContainer<T> {

	private final T[] array;

	public ArrayContainer(T[] array) {
		this.array = array;
	}

	public T[] getArray() {
		return array;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayContainer other = (ArrayContainer) obj;
		if (!Arrays.equals(array, other.array))
			return false;
		return true;
	}

}
