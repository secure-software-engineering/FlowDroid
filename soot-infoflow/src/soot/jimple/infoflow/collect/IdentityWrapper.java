package soot.jimple.infoflow.collect;

class IdentityWrapper<E> {

	private final E contents;
	
	public IdentityWrapper(E abs) {
		this.contents = abs;
	}
	
	public E getContents() {
		return this.contents;
	}
	
	@Override
	public String toString() {
		return this.contents.toString();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (this.getClass() != other.getClass())
			return false;
		return this.contents == ((IdentityWrapper) other).contents;
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(this.contents);
	}
	
}
