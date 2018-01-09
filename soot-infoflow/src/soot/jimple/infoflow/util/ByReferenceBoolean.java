package soot.jimple.infoflow.util;

/**
 * Wrapper class for passing boolean values by reference
 * 
 * @author Steven Arzt
 *
 */
public class ByReferenceBoolean {

	public boolean value;

	public ByReferenceBoolean() {
		this.value = false;
	}

	public ByReferenceBoolean(boolean initialValue) {
		this.value = initialValue;
	}

	public ByReferenceBoolean and(boolean b) {
		this.value &= b;
		return this;
	}

	public ByReferenceBoolean or(boolean b) {
		this.value |= b;
		return this;
	}

	public ByReferenceBoolean xor(boolean b) {
		this.value ^= b;
		return this;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (value ? 1231 : 1237);
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
		ByReferenceBoolean other = (ByReferenceBoolean) obj;
		if (value != other.value)
			return false;
		return true;
	}

}
