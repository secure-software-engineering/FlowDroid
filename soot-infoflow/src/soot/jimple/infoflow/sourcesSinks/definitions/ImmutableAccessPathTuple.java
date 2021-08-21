package soot.jimple.infoflow.sourcesSinks.definitions;

/**
 * Immutable version of the {@link AccessPathTuple}
 * 
 * @author Steven Arzt
 *
 */
public class ImmutableAccessPathTuple extends AccessPathTuple {

	public ImmutableAccessPathTuple(AccessPathTuple parent) {
		super(parent);
	}

	@Override
	public void setDescription(String description) {
		throw new RuntimeException("Immutable object cannot be modified");
	}

}
