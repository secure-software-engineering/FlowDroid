package soot.jimple.infoflow.solver;

/**
 * Container class for abstractions to be propagated
 * 
 * @author Steven Arzt
 *
 */
public class Propagator<D> {

	private final D abstraction;

	public Propagator(D abstraction) {
		this.abstraction = abstraction;
	}

	public D getAbstraction() {
		return abstraction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
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
		Propagator other = (Propagator) obj;
		if (abstraction == null) {
			if (other.abstraction != null)
				return false;
		} else if (!abstraction.equals(other.abstraction))
			return false;
		return true;
	}

}
