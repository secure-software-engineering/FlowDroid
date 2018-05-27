package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.HashSet;
import java.util.Set;

/**
 * Class for defining fields as sources or sinks
 * 
 * @author Steven Arzt
 *
 */
public class FieldSourceSinkDefinition extends SourceSinkDefinition {

	protected final String fieldSignature;
	protected Set<AccessPathTuple> accessPaths;

	/**
	 * Creates a new instance of the {@link FieldSourceSinkDefinition} class
	 * 
	 * @param fieldSignature
	 *            The Soot signature of the target field
	 */
	public FieldSourceSinkDefinition(String fieldSignature) {
		this(fieldSignature, null);
	}

	/**
	 * Creates a new instance of the {@link FieldSourceSinkDefinition} class
	 * 
	 * @param fieldSignature
	 *            The Soot signature of the target field
	 * @param accessPaths
	 *            The access paths on the field that have been defined as sources or
	 *            sinks
	 */
	public FieldSourceSinkDefinition(String fieldSignature, Set<AccessPathTuple> accessPaths) {
		this.fieldSignature = fieldSignature;
		this.accessPaths = accessPaths;
	}

	/**
	 * Gets the signature of the referenced Soot field
	 * 
	 * @return The signature of the referenced Soot field
	 */
	public String getFieldSignature() {
		return fieldSignature;
	}

	/**
	 * Gets the access paths on the field that have been defined as sources or sinks
	 * 
	 * @return The access paths on the field that have been defined as sources or
	 *         sinks
	 */
	public Set<AccessPathTuple> getAccessPaths() {
		return accessPaths;
	}

	@Override
	public SourceSinkDefinition getSourceOnlyDefinition() {
		Set<AccessPathTuple> sources = null;
		if (accessPaths != null) {
			sources = new HashSet<>(accessPaths.size());
			for (AccessPathTuple apt : accessPaths)
				if (apt.getSourceSinkType().isSource())
					sources.add(apt);
		}
		return buildNewDefinition(sources);
	}

	@Override
	public SourceSinkDefinition getSinkOnlyDefinition() {
		Set<AccessPathTuple> sinks = null;
		if (accessPaths != null) {
			sinks = new HashSet<>(accessPaths.size());
			for (AccessPathTuple apt : accessPaths)
				if (apt.getSourceSinkType().isSink())
					sinks.add(apt);
		}
		return buildNewDefinition(sinks);
	}

	/**
	 * Factory method for creating a new field-based source/sink definition based on
	 * the current one. This method is used when transforming the current
	 * definition. Derived classes can override this method to create instances of
	 * the correct class.
	 * 
	 * @param accessPaths
	 *            The of access paths for the new definition
	 * @return The new source/sink definition
	 */
	protected SourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> accessPaths) {
		return new FieldSourceSinkDefinition(fieldSignature, accessPaths);
	}

	@Override
	public void merge(SourceSinkDefinition other) {
		if (other instanceof FieldSourceSinkDefinition) {
			FieldSourceSinkDefinition otherField = (FieldSourceSinkDefinition) other;

			// Merge the base object definitions
			if (otherField.accessPaths != null && !otherField.accessPaths.isEmpty()) {
				if (this.accessPaths == null)
					this.accessPaths = new HashSet<>();
				for (AccessPathTuple apt : otherField.accessPaths)
					this.accessPaths.add(apt);
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return accessPaths == null || accessPaths.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPaths == null) ? 0 : accessPaths.hashCode());
		result = prime * result + ((fieldSignature == null) ? 0 : fieldSignature.hashCode());
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
		FieldSourceSinkDefinition other = (FieldSourceSinkDefinition) obj;
		if (accessPaths == null) {
			if (other.accessPaths != null)
				return false;
		} else if (!accessPaths.equals(other.accessPaths))
			return false;
		if (fieldSignature == null) {
			if (other.fieldSignature != null)
				return false;
		} else if (!fieldSignature.equals(other.fieldSignature))
			return false;
		return true;
	}

}
