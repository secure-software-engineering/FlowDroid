package soot.jimple.infoflow.data;

import java.util.Objects;

/**
 * A context definition for distinguishing individual elements inside a
 * container
 * 
 * @author Steven Arzt
 *
 */
public class ContextDefinition {

	private final String name;
	private final String value;

	public ContextDefinition(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Gets the name of this context parameter
	 * 
	 * @return The name of this context parameter
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of this context parameter
	 * 
	 * @return The value of this context parameter
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.format("%s=%s", name, value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContextDefinition other = (ContextDefinition) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}

}
